package com.kgps.result.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.kgps.result.Repo
import com.kgps.result.Settings
import com.kgps.result.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalBackupEntry(
    val fileName: String,
    val path: String,
    val modifiedMs: Long,
    val sizeBytes: Long
)

data class AutoBackupStatus(
    val localEnabled: Boolean = true,
    val localFolderUri: String? = null,
    val localFolderLabel: String = "অ্যাপ স্টোরেজ + Downloads/ResultMaker",
    val lastLocalBackupMs: Long = 0L,
    val lastLocalFileName: String = "",
    val lastLocalError: String? = null,
    val localSnapshots: List<LocalBackupEntry> = emptyList(),
    val driveEnabled: Boolean = false,
    val driveNeedsConsent: Boolean = false,
    val driveBusy: Boolean = false,
    val lastDriveBackupMs: Long = 0L,
    val lastDriveFileName: String = "",
    val lastDriveError: String? = null
)

class AutoBackupManager(
    context: Context,
    private val userId: String
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("rm_auto_backup_$userId", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(loadInitialStatus())
    val status: StateFlow<AutoBackupStatus> = _status.asStateFlow()

    private fun loadInitialStatus(): AutoBackupStatus {
        val folderUri = prefs.getString(KEY_LOCAL_FOLDER_URI, null)
        return AutoBackupStatus(
            localEnabled = prefs.getBoolean(KEY_LOCAL_ENABLED, true),
            localFolderUri = folderUri,
            localFolderLabel = buildFolderLabel(folderUri),
            lastLocalBackupMs = prefs.getLong(KEY_LAST_LOCAL_MS, 0L),
            lastLocalFileName = prefs.getString(KEY_LAST_LOCAL_NAME, "").orEmpty(),
            driveEnabled = prefs.getBoolean(KEY_DRIVE_ENABLED, false),
            lastDriveBackupMs = prefs.getLong(KEY_LAST_DRIVE_MS, 0L),
            lastDriveFileName = prefs.getString(KEY_LAST_DRIVE_NAME, "").orEmpty()
        )
    }

    private fun buildFolderLabel(uriStr: String?): String {
        if (uriStr.isNullOrBlank()) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "অ্যাপ স্টোরেজ + Downloads/ResultMaker"
            } else {
                "অ্যাপ ব্যাকআপ ফোল্ডার"
            }
        }
        val docName = runCatching {
            DocumentFile.fromTreeUri(appContext, Uri.parse(uriStr))?.name
        }.getOrNull()?.takeIf { it.isNotBlank() }
        return if (docName != null) "নির্বাচিত ফোল্ডার: $docName" else "নির্বাচিত কাস্টম ফোল্ডার"
    }

    fun setLocalAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCAL_ENABLED, enabled).apply()
        _status.update { it.copy(localEnabled = enabled, lastLocalError = null) }
    }

    fun setLocalFolderUri(uri: Uri?) {
        val raw = uri?.toString()
        if (uri != null) {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        prefs.edit().putString(KEY_LOCAL_FOLDER_URI, raw).apply()
        _status.update {
            it.copy(
                localFolderUri = raw,
                localFolderLabel = buildFolderLabel(raw),
                lastLocalError = null
            )
        }
    }

    fun setDriveAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DRIVE_ENABLED, enabled).apply()
        _status.update {
            it.copy(
                driveEnabled = enabled,
                lastDriveError = if (!enabled) null else it.lastDriveError
            )
        }
    }

    fun refreshLocalSnapshots(schoolId: String) {
        val dir = schoolBackupDir(schoolId)
        val entries = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { f ->
                LocalBackupEntry(
                    fileName = f.name,
                    path = f.absolutePath,
                    modifiedMs = f.lastModified(),
                    sizeBytes = f.length()
                )
            } ?: emptyList()
        _status.update { it.copy(localSnapshots = entries) }
    }

    suspend fun runLocalBackupIfNeeded(
        schoolId: String,
        schoolCode: String,
        students: List<Student>,
        settings: Settings,
        force: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!force && !_status.value.localEnabled) {
            return@withContext Result.success("skipped")
        }
        val json = Repo.toJson(students, settings)
        val hash = sha256(json)
        val prevHash = prefs.getString(KEY_LAST_LOCAL_HASH, "")
        if (!force && hash == prevHash) {
            refreshLocalSnapshots(schoolId)
            return@withContext Result.success("unchanged")
        }

        runCatching {
            val bytes = json.toByteArray(Charsets.UTF_8)
            val dateTag = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val safeCode = schoolCode.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "SCHOOL" }
            val dailyName = "ResultMaker_${safeCode}_$dateTag.json"

            // 1. App-private rolling backup folder (always reliable, zero permissions)
            val dir = schoolBackupDir(schoolId)
            File(dir, "latest_backup.json").writeBytes(bytes)
            File(dir, dailyName).writeBytes(bytes)
            pruneOldSnapshots(dir, keepDailyCount = 14)

            // Also keep standard local Repo snapshot synchronized
            Repo.save(appContext, students, settings)

            // 2. Android 10+ Downloads/ResultMaker folder (user-accessible without permissions)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeToMediaStoreDownloads("ResultMaker_${safeCode}_latest.json", bytes)
            }

            // 3. Optional user-picked SAF folder
            val folderUriStr = _status.value.localFolderUri
            if (!folderUriStr.isNullOrBlank()) {
                writeToSafFolder(Uri.parse(folderUriStr), dailyName, bytes)
            }

            val nowMs = System.currentTimeMillis()
            prefs.edit()
                .putString(KEY_LAST_LOCAL_HASH, hash)
                .putLong(KEY_LAST_LOCAL_MS, nowMs)
                .putString(KEY_LAST_LOCAL_NAME, dailyName)
                .apply()

            refreshLocalSnapshots(schoolId)
            _status.update {
                it.copy(
                    lastLocalBackupMs = nowMs,
                    lastLocalFileName = dailyName,
                    lastLocalError = null
                )
            }
            dailyName
        }.onFailure { err ->
            _status.update {
                it.copy(lastLocalError = err.localizedMessage ?: "লোকাল ব্যাকআপ ত্রুটি")
            }
        }
    }

    suspend fun readLocalSnapshot(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching { File(path).readText(Charsets.UTF_8) }
    }

    suspend fun runDriveBackupIfNeeded(
        schoolCode: String,
        students: List<Student>,
        settings: Settings,
        force: Boolean = false
    ): DriveAuthOutcome {
        if (!force && !_status.value.driveEnabled) {
            return DriveAuthOutcome.Failed("disabled")
        }
        val json = Repo.toJson(students, settings)
        val hash = sha256(json)
        val prevHash = prefs.getString(KEY_LAST_DRIVE_HASH, "")
        val lastMs = prefs.getLong(KEY_LAST_DRIVE_MS, 0L)
        val elapsedMs = System.currentTimeMillis() - lastMs
        if (!force && hash == prevHash) {
            return DriveAuthOutcome.Authorized("unchanged")
        }
        if (!force && elapsedMs < MIN_AUTO_DRIVE_INTERVAL_MS) {
            return DriveAuthOutcome.Authorized("throttled")
        }

        _status.update { it.copy(driveBusy = true, lastDriveError = null) }
        return when (val auth = GoogleDriveBackupClient.authorizeDrive(appContext)) {
            is DriveAuthOutcome.NeedsConsent -> {
                _status.update { it.copy(driveBusy = false, driveNeedsConsent = true) }
                auth
            }
            is DriveAuthOutcome.Failed -> {
                _status.update { it.copy(driveBusy = false, lastDriveError = auth.message) }
                auth
            }
            is DriveAuthOutcome.Authorized -> {
                uploadWithToken(auth.accessToken, schoolCode, json, hash)
                auth
            }
        }
    }

    suspend fun uploadWithToken(
        accessToken: String,
        schoolCode: String,
        jsonContent: String,
        hash: String = sha256(jsonContent)
    ): Result<DriveBackupFile> {
        _status.update { it.copy(driveBusy = true, driveNeedsConsent = false, lastDriveError = null) }
        val dateTag = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val safeCode = schoolCode.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "SCHOOL" }
        val fileName = "ResultMaker_${safeCode}_$dateTag.json"

        val result = GoogleDriveBackupClient.uploadBackupJson(accessToken, fileName, jsonContent)
        result.onSuccess { uploaded ->
            val nowMs = System.currentTimeMillis()
            prefs.edit()
                .putString(KEY_LAST_DRIVE_HASH, hash)
                .putLong(KEY_LAST_DRIVE_MS, nowMs)
                .putString(KEY_LAST_DRIVE_NAME, uploaded.name)
                .putBoolean(KEY_DRIVE_ENABLED, true)
                .apply()
            _status.update {
                it.copy(
                    driveEnabled = true,
                    driveBusy = false,
                    driveNeedsConsent = false,
                    lastDriveBackupMs = nowMs,
                    lastDriveFileName = uploaded.name,
                    lastDriveError = null
                )
            }
        }.onFailure { err ->
            _status.update {
                it.copy(
                    driveBusy = false,
                    lastDriveError = err.localizedMessage ?: "Google Drive ব্যাকআপ ব্যর্থ হয়েছে"
                )
            }
        }
        return result
    }

    private fun schoolBackupDir(schoolId: String): File {
        val safeId = schoolId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "default" }
        val dir = File(appContext.filesDir, "auto_backups/$safeId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun pruneOldSnapshots(dir: File, keepDailyCount: Int) {
        val dailyFiles = dir.listFiles { f ->
            f.isFile && f.name.startsWith("ResultMaker_") && f.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() } ?: return
        dailyFiles.drop(keepDailyCount).forEach { runCatching { it.delete() } }
    }

    private fun writeToMediaStoreDownloads(fileName: String, bytes: ByteArray) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val resolver = appContext.contentResolver
        val relPath = "${Environment.DIRECTORY_DOWNLOADS}/ResultMaker"
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

        // Remove previous copy with the same filename in Download/ResultMaker so latest stays clean
        runCatching {
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
            resolver.delete(collection, selection, arrayOf(fileName, "$relPath/"))
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return
        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (_: Exception) {
            runCatching { resolver.delete(uri, null, null) }
        }
    }

    private fun writeToSafFolder(treeUri: Uri, fileName: String, bytes: ByteArray) {
        val dirDoc = DocumentFile.fromTreeUri(appContext, treeUri) ?: return
        val existing = dirDoc.findFile(fileName)
        val target = existing ?: dirDoc.createFile("application/json", fileName) ?: return
        appContext.contentResolver.openOutputStream(target.uri, "wt")?.use { out ->
            out.write(bytes)
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_LOCAL_ENABLED = "local_enabled"
        private const val KEY_LOCAL_FOLDER_URI = "local_folder_uri"
        private const val KEY_LAST_LOCAL_HASH = "last_local_hash"
        private const val KEY_LAST_LOCAL_MS = "last_local_ms"
        private const val KEY_LAST_LOCAL_NAME = "last_local_name"

        private const val KEY_DRIVE_ENABLED = "drive_enabled"
        private const val KEY_LAST_DRIVE_HASH = "last_drive_hash"
        private const val KEY_LAST_DRIVE_MS = "last_drive_ms"
        private const val KEY_LAST_DRIVE_NAME = "last_drive_name"

        private const val MIN_AUTO_DRIVE_INTERVAL_MS = 60_000L
    }
}
