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
    val msg = firestoreEx?.message ?: error.message ?: ""
    return when {
        msg.contains("Quota exceeded", ignoreCase = true) ->
            "দৈনিক ফ্রি কোটা শেষ হয়েছে। আগামীকাল কোটা পুনরায় চালু হবে।"
        firestoreEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "এই কাজের অনুমতি নেই বা সেশন মেয়াদ শেষ হয়েছে।"
        firestoreEx?.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "নেটওয়ার্ক সংযোগ নেই — অনলাইন হলে স্বয়ংক্রিয়ভাবে সিঙ্ক হবে।"
        else -> msg.ifBlank { "সার্ভারের সাথে সংযোগে সমস্যা হয়েছে" }
    }
}
