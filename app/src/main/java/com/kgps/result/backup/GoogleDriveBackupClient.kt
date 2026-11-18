package com.kgps.result.backup

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class DriveBackupFile(
    val id: String,
    val name: String,
    val modifiedTime: String,
    val sizeBytes: Long
)

sealed interface DriveAuthOutcome {
    data class Authorized(val accessToken: String) : DriveAuthOutcome
    data class NeedsConsent(val intentSender: IntentSender) : DriveAuthOutcome
    data class Failed(val message: String) : DriveAuthOutcome
}

object GoogleDriveBackupClient {
    private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    private const val BACKUP_FOLDER_NAME = "Result Maker Backups"
    private const val DRIVE_FILES_API = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_API = "https://www.googleapis.com/upload/drive/v3/files"

    suspend fun authorizeDrive(context: Context): DriveAuthOutcome {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
                .build()
            val result: AuthorizationResult = Identity.getAuthorizationClient(context)
                .authorize(request)
                .await()
            interpretAuthResult(result)
        } catch (e: Exception) {
            val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: e::class.java.simpleName
            DriveAuthOutcome.Failed("Google Drive অনুমতি যাচাই ব্যর্থ: $detail")
        }
    }

    fun handleConsentResult(context: Context, data: Intent?): DriveAuthOutcome {
        return try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)
            interpretAuthResult(result)
        } catch (e: Exception) {
            val detail = e.localizedMessage?.takeIf { it.isNotBlank() } ?: e::class.java.simpleName
            DriveAuthOutcome.Failed("Google Drive অনুমতি সম্পন্ন হয়নি: $detail")
        }
    }

    private fun interpretAuthResult(result: AuthorizationResult): DriveAuthOutcome {
        if (result.hasResolution()) {
            val sender = result.pendingIntent?.intentSender
            if (sender != null) {
                return DriveAuthOutcome.NeedsConsent(sender)
            }
        }
        val token = result.accessToken
        return if (!token.isNullOrBlank()) {
            DriveAuthOutcome.Authorized(token)
        } else {
            DriveAuthOutcome.Failed("Google Drive অ্যাক্সেস টোকেন পাওয়া যায়নি")
        }
    }

    suspend fun uploadBackupJson(
        accessToken: String,
        fileName: String,
        jsonContent: String
    ): Result<DriveBackupFile> = withContext(Dispatchers.IO) {
        runCatching {
            val folderId = ensureBackupFolder(accessToken)
            val existingId = findFileInFolder(accessToken, folderId, fileName)
            val boundary = "====ResultMakerDriveBoundary${System.currentTimeMillis()}===="
            val metadata = JSONObject().apply {
                put("name", fileName)
                put("mimeType", "application/json")
                if (existingId == null) {
                    put("parents", JSONArray().put(folderId))
                }
            }

            val endpoint = if (existingId == null) {
                "$DRIVE_UPLOAD_API?uploadType=multipart&fields=id,name,modifiedTime,size"
            } else {
                "$DRIVE_UPLOAD_API/$existingId?uploadType=multipart&fields=id,name,modifiedTime,size"
            }

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = if (existingId == null) "POST" else "PATCH"
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 30_000
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            }

            conn.outputStream.use { out ->
                val writer = OutputStreamWriter(out, Charsets.UTF_8)
                writer.write("--$boundary\r\n")
                writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                writer.write(metadata.toString())
                writer.write("\r\n--$boundary\r\n")
                writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                writer.write(jsonContent)
                writer.write("\r\n--$boundary--\r\n")
                writer.flush()
            }

            val code = conn.responseCode
            val body = readResponseBody(conn, code in 200..299)
            if (code !in 200..299) {
                throw IllegalStateException(formatDriveHttpError(code, body))
            }

            val obj = JSONObject(body)
            DriveBackupFile(
                id = obj.optString("id"),
                name = obj.optString("name", fileName),
                modifiedTime = obj.optString("modifiedTime"),
                sizeBytes = obj.optString("size", jsonContent.toByteArray(Charsets.UTF_8).size.toString()).toLongOrNull() ?: 0L
            )
        }
    }

    suspend fun listBackupFiles(accessToken: String): Result<List<DriveBackupFile>> = withContext(Dispatchers.IO) {
        runCatching {
            val folderId = findFolderId(accessToken) ?: return@runCatching emptyList()
            val q = URLEncoder.encode("'$folderId' in parents and trashed = false and mimeType != 'application/vnd.google-apps.folder'", "UTF-8")
            val fields = URLEncoder.encode("files(id,name,modifiedTime,size)", "UTF-8")
            val orderBy = URLEncoder.encode("modifiedTime desc", "UTF-8")
            val endpoint = "$DRIVE_FILES_API?q=$q&fields=$fields&orderBy=$orderBy&pageSize=30"

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            val code = conn.responseCode
            val body = readResponseBody(conn, code in 200..299)
            if (code !in 200..299) {
                throw IllegalStateException(formatDriveHttpError(code, body))
            }

            val arr = JSONObject(body).optJSONArray("files") ?: JSONArray()
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                DriveBackupFile(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    modifiedTime = obj.optString("modifiedTime"),
                    sizeBytes = obj.optString("size", "0").toLongOrNull() ?: 0L
                )
            }
        }
    }

    suspend fun downloadBackupJson(accessToken: String, fileId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$DRIVE_FILES_API/$fileId?alt=media"
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            val code = conn.responseCode
            val body = readResponseBody(conn, code in 200..299)
            if (code !in 200..299) {
                throw IllegalStateException(formatDriveHttpError(code, body))
            }
            body
        }
    }

    private fun ensureBackupFolder(accessToken: String): String {
        findFolderId(accessToken)?.let { return it }

        val payload = JSONObject().apply {
            put("name", BACKUP_FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }
        val conn = (URL("$DRIVE_FILES_API?fields=id").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val body = readResponseBody(conn, code in 200..299)
        if (code !in 200..299) {
            throw IllegalStateException(formatDriveHttpError(code, body))
        }
        return JSONObject(body).getString("id")
    }

    private fun findFolderId(accessToken: String): String? {
        val escaped = BACKUP_FOLDER_NAME.replace("'", "\\'")
        val q = URLEncoder.encode("name = '$escaped' and mimeType = 'application/vnd.google-apps.folder' and trashed = false", "UTF-8")
        val endpoint = "$DRIVE_FILES_API?q=$q&fields=files(id,name)&pageSize=1"
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        val code = conn.responseCode
        val body = readResponseBody(conn, code in 200..299)
        if (code !in 200..299) {
            throw IllegalStateException(formatDriveHttpError(code, body))
        }
        val arr = JSONObject(body).optJSONArray("files") ?: return null
        return arr.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
    }

    private fun findFileInFolder(accessToken: String, folderId: String, fileName: String): String? {
        val escaped = fileName.replace("'", "\\'")
        val q = URLEncoder.encode("'$folderId' in parents and name = '$escaped' and trashed = false", "UTF-8")
        val endpoint = "$DRIVE_FILES_API?q=$q&fields=files(id)&pageSize=1"
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        val code = conn.responseCode
        val body = readResponseBody(conn, code in 200..299)
        if (code !in 200..299) {
            throw IllegalStateException(formatDriveHttpError(code, body))
        }
        val arr = JSONObject(body).optJSONArray("files") ?: return null
        return arr.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
    }

    private fun readResponseBody(conn: HttpURLConnection, ok: Boolean): String {
        val stream = if (ok) conn.inputStream else conn.errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    private fun formatDriveHttpError(code: Int, rawBody: String): String {
        if (code == 403 && (rawBody.contains("accessNotConfigured", ignoreCase = true) ||
                rawBody.contains("drive.googleapis.com", ignoreCase = true))
        ) {
            return "Google Cloud Console-এ Google Drive API চালু (Enable) করা নেই। অনুগ্রহ করে প্রজেক্টে Google Drive API Enable করুন।"
        }
        if (code == 401) {
            return "Google Drive অনুমোদনের মেয়াদ শেষ হয়েছে — পুনরায় সংযুক্ত করুন।"
        }
        val msg = runCatching {
            JSONObject(rawBody).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        return "Google Drive ত্রুটি (HTTP $code): ${msg ?: rawBody.take(160)}"
    }
}
