package com.kgps.result.cloud

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = Firebase.auth.currentUser
    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

fun friendlyFirestoreErrorMessage(error: Throwable): String {
    val firestoreEx = generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    val msg = (firestoreEx?.message ?: error.message ?: "").trim()
    val code = firestoreEx?.code
    return when {
        msg.contains("Quota exceeded", ignoreCase = true) ->
            "দৈনিক ফ্রি কোটা শেষ হয়েছে। আগামীকাল কোটা পুনরায় চালু হবে।"
        code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "এই কাজের অনুমতি নেই বা Firestore Rules অনুমোদিত নয়।"
        code == FirebaseFirestoreException.Code.NOT_FOUND ||
            code == FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
            "Firestore ডেটাবেজ প্রস্তুত নয় (${code.name}): ${msg.take(120)}"
        code == FirebaseFirestoreException.Code.UNAVAILABLE -> {
            val detail = msg.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
            "Firestore সার্ভারে সংযোগ পাওয়া যায়নি$detail"
        }
        else -> msg.ifBlank { "সার্ভারের সাথে সংযোগে সমস্যা হয়েছে" }
    }
}
