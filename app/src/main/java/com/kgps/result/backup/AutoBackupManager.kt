package com.kgps.result.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.kgps.result.BlankSheetQrMeta
import com.kgps.result.CsvHeaderStyle
import com.kgps.result.Repo
import com.kgps.result.ReportBackupPrefs
import com.kgps.result.Settings
import com.kgps.result.Student
import com.kgps.result.bn
import com.kgps.result.cloud.SheetTemplateCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class BackupSystemKind {
    LOCAL,
    DRIVE,
    CSV
}

enum class BackupFileRetentionMode(
    val id: String,
    val shortBn: String,
    val descBn: String
) {
    SINGLE_LATEST(
        id = "single",
        shortBn = "১টি ফাইল (আপডেট)",
        descBn = "একই স্থায়ী ফাইলে সর্বশেষ ব্যাকআপ আপডেট হবে, প্রতিদিন নতুন ফাইল জমবে না"
    ),
    DAILY_HISTORY(
        id = "daily",
        shortBn = "একাধিক (দৈনিক)",
        descBn = "প্রতিদিন আলাদা তারিখের নামে ফাইল সংরক্ষিত হবে (সর্বোচ্চ ১৪ দিন)"
    );

    companion object {
        fun fromId(id: String?): BackupFileRetentionMode =
            entries.firstOrNull { it.id == id } ?: SINGLE_LATEST
    }
}

object BackupFileNaming {
    fun sanitizeSchoolCode(schoolCode: String): String =
        schoolCode.replace(Regex("[^A-Za-z0-9]"), "").ifBlank { "SCHOOL" }

    fun formatDateTag(nowMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))

    fun resolveBackupFileName(
        schoolCode: String,
        extensionWithDot: String,
        mode: BackupFileRetentionMode,
        nowMs: Long = System.currentTimeMillis()
    ): String {
        val safeCode = sanitizeSchoolCode(schoolCode)
        val normalizedExt = if (extensionWithDot.startsWith(".")) extensionWithDot else ".$extensionWithDot"
        return when (mode) {
            BackupFileRetentionMode.SINGLE_LATEST -> "ResultMaker_${safeCode}_latest$normalizedExt"
            BackupFileRetentionMode.DAILY_HISTORY -> "ResultMaker_${safeCode}_${formatDateTag(nowMs)}$normalizedExt"
        }
    }
}


enum class CsvBackupFormat(
    val id: String,
    val labelBn: String,
    val shortBn: String,
    val descBn: String
) {
    CSV(
        id = "csv",
        labelBn = "CSV ফাইল (১টি ফাইলে সব শ্রেণি)",
        shortBn = "CSV (১ ফাইল)",
        descBn = "একটি .csv ফাইলে সকল শ্রেণির শিক্ষার্থী ও নম্বর থাকবে"
    ),
    EXCEL(
        id = "excel",
        labelBn = "Excel (.xlsx · আলাদা আলাদা শিট)",
        shortBn = "Excel (আলাদা শিট)",
        descBn = "একটি .xlsx ফাইলের ভেতর প্রতিটি শ্রেণি আলাদা আলাদা শিটে থাকবে"
    );

    companion object {
        fun fromId(id: String?): CsvBackupFormat =
            entries.firstOrNull { it.id == id } ?: CSV
    }
}

enum class BackupScheduleMode(
    val id: String,
    val labelBn: String,
    val shortBn: String
) {
    ON_CHANGE("on_change", "ডাটা পরিবর্তনে (তাৎক্ষণিক)", "তাৎক্ষণিক"),
    EVERY_6_HOURS("every_6h", "প্রতি ৬ ঘণ্টায়", "৬ ঘণ্টা"),
    EVERY_12_HOURS("every_12h", "প্রতি ১২ ঘণ্টায়", "১২ ঘণ্টা"),
    DAILY("daily", "প্রতিদিন নির্ধারিত সময়ে", "প্রতিদিন"),
    WEEKLY("weekly", "সাপ্তাহিক নির্ধারিত সময়ে", "সাপ্তাহিক");

    companion object {
        fun fromId(id: String?): BackupScheduleMode =
            entries.firstOrNull { it.id == id } ?: DAILY
    }
}

data class BackupScheduleConfig(
    val mode: BackupScheduleMode = BackupScheduleMode.ON_CHANGE,
    val hour: Int = 21,
    val minute: Int = 0,
    val dayOfWeek: Int = Calendar.FRIDAY
)

object BackupScheduleCalculator {
    private const val HOUR_MS = 3_600_000L

    private fun anchorCalendar(hour: Int, minute: Int, nowMs: Long): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun latestScheduledSlotMs(cfg: BackupScheduleConfig, nowMs: Long = System.currentTimeMillis()): Long {
        return when (cfg.mode) {
            BackupScheduleMode.ON_CHANGE -> nowMs
            BackupScheduleMode.EVERY_6_HOURS -> {
                val base = anchorCalendar(cfg.hour, cfg.minute, nowMs).timeInMillis
                val candidates = (-4..4).map { base + it * 6L * HOUR_MS }
                candidates.filter { it <= nowMs }.maxOrNull() ?: base
            }
            BackupScheduleMode.EVERY_12_HOURS -> {
                val base = anchorCalendar(cfg.hour, cfg.minute, nowMs).timeInMillis
                val candidates = (-3..3).map { base + it * 12L * HOUR_MS }
                candidates.filter { it <= nowMs }.maxOrNull() ?: base
            }
            BackupScheduleMode.DAILY -> {
                val cal = anchorCalendar(cfg.hour, cfg.minute, nowMs)
                if (cal.timeInMillis > nowMs) cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.timeInMillis
            }
            BackupScheduleMode.WEEKLY -> {
                val cal = anchorCalendar(cfg.hour, cfg.minute, nowMs)
                cal.set(Calendar.DAY_OF_WEEK, cfg.dayOfWeek.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY))
                if (cal.timeInMillis > nowMs) cal.add(Calendar.WEEK_OF_YEAR, -1)
                cal.timeInMillis
            }
        }
    }

    fun nextScheduledSlotMs(cfg: BackupScheduleConfig, nowMs: Long = System.currentTimeMillis()): Long {
        return when (cfg.mode) {
            BackupScheduleMode.ON_CHANGE -> nowMs
            BackupScheduleMode.EVERY_6_HOURS -> {
                val base = anchorCalendar(cfg.hour, cfg.minute, nowMs).timeInMillis
                val candidates = (-4..5).map { base + it * 6L * HOUR_MS }
                candidates.filter { it > nowMs }.minOrNull() ?: (nowMs + 6L * HOUR_MS)
            }
            BackupScheduleMode.EVERY_12_HOURS -> {
                val base = anchorCalendar(cfg.hour, cfg.minute, nowMs).timeInMillis
                val candidates = (-3..4).map { base + it * 12L * HOUR_MS }
                candidates.filter { it > nowMs }.minOrNull() ?: (nowMs + 12L * HOUR_MS)
            }
            BackupScheduleMode.DAILY -> {
                val cal = anchorCalendar(cfg.hour, cfg.minute, nowMs)
                if (cal.timeInMillis <= nowMs) cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            BackupScheduleMode.WEEKLY -> {
                val cal = anchorCalendar(cfg.hour, cfg.minute, nowMs)
                cal.set(Calendar.DAY_OF_WEEK, cfg.dayOfWeek.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY))
                if (cal.timeInMillis <= nowMs) cal.add(Calendar.WEEK_OF_YEAR, 1)
                cal.timeInMillis
            }
        }
    }

    fun isScheduledDue(
        cfg: BackupScheduleConfig,
        lastSlotHandledMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (cfg.mode == BackupScheduleMode.ON_CHANGE) return true
        val latestSlot = latestScheduledSlotMs(cfg, nowMs)
        return lastSlotHandledMs < latestSlot
    }

    fun formatTime12hBn(hour: Int, minute: Int): String {
        val h24 = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val period = when {
            h24 in 5..11 -> "সকাল"
            h24 in 12..15 -> "দুপুর"
            h24 in 16..18 -> "বিকাল"
            else -> "রাত"
        }
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        val amPm = if (h24 < 12) "AM" else "PM"
        val hh = h12.toString().padStart(2, '0')
        val mm = m.toString().padStart(2, '0')
        return "$period ${bn("$hh:$mm")} $amPm"
    }

    fun formatWeekdayShortBn(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.SATURDAY -> "শনি"
        Calendar.SUNDAY -> "রবি"
        Calendar.MONDAY -> "সোম"
        Calendar.TUESDAY -> "মঙ্গল"
        Calendar.WEDNESDAY -> "বুধ"
        Calendar.THURSDAY -> "বৃহঃ"
        else -> "শুক্র"
    }

    fun formatWeekdayFullBn(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.SATURDAY -> "শনিবার"
        Calendar.SUNDAY -> "রবিবার"
        Calendar.MONDAY -> "সোমবার"
        Calendar.TUESDAY -> "মঙ্গলবার"
        Calendar.WEDNESDAY -> "বুধবার"
        Calendar.THURSDAY -> "বৃহস্পতিবার"
        else -> "শুক্রবার"
    }

    fun formatNextRunBn(cfg: BackupScheduleConfig, nowMs: Long = System.currentTimeMillis()): String {
        if (cfg.mode == BackupScheduleMode.ON_CHANGE) {
            return "ডাটা পরিবর্তনের সাথে সাথে"
        }
        val nextMs = nextScheduledSlotMs(cfg, nowMs)
        val calNow = Calendar.getInstance().apply { timeInMillis = nowMs }
        val calNext = Calendar.getInstance().apply { timeInMillis = nextMs }
        val sameDay = calNow.get(Calendar.YEAR) == calNext.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calNext.get(Calendar.DAY_OF_YEAR)
        val calTomorrow = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val isTomorrow = calTomorrow.get(Calendar.YEAR) == calNext.get(Calendar.YEAR) &&
            calTomorrow.get(Calendar.DAY_OF_YEAR) == calNext.get(Calendar.DAY_OF_YEAR)
        val dayPrefix = when {
            sameDay -> "আজ"
            isTomorrow -> "আগামীকাল"
            else -> bn(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(nextMs)))
        }
        return "$dayPrefix ${formatTime12hBn(calNext.get(Calendar.HOUR_OF_DAY), calNext.get(Calendar.MINUTE))}"
    }
}

data class LocalBackupEntry(
    val fileName: String,
    val path: String,
    val modifiedMs: Long,
    val sizeBytes: Long
)

data class AutoBackupStatus(
    // 1. Local JSON Auto Backup
    val localEnabled: Boolean = true,
    val localFolderUri: String? = null,
    val localFolderLabel: String = "অ্যাপ স্টোরেজ + Downloads/ResultMaker",
    val localSchedule: BackupScheduleConfig = BackupScheduleConfig(
        mode = BackupScheduleMode.ON_CHANGE,
        hour = 21,
        minute = 0,
        dayOfWeek = Calendar.FRIDAY
    ),
    val localRetentionMode: BackupFileRetentionMode = BackupFileRetentionMode.SINGLE_LATEST,
    val lastLocalBackupMs: Long = 0L,
    val lastLocalFileName: String = "",
    val lastLocalError: String? = null,
    val localSnapshots: List<LocalBackupEntry> = emptyList(),

    // 2. Google Drive Auto Backup
    val driveEnabled: Boolean = false,
    val driveNeedsConsent: Boolean = false,
    val driveBusy: Boolean = false,
    val driveSchedule: BackupScheduleConfig = BackupScheduleConfig(
        mode = BackupScheduleMode.DAILY,
        hour = 22,
        minute = 0,
        dayOfWeek = Calendar.FRIDAY
    ),
    val driveRetentionMode: BackupFileRetentionMode = BackupFileRetentionMode.SINGLE_LATEST,
    val lastDriveBackupMs: Long = 0L,
    val lastDriveFileName: String = "",
    val lastDriveError: String? = null,

    // 3. CSV / Excel Auto Backup
    val csvEnabled: Boolean = true,
    val csvFolderUri: String? = null,
    val csvFolderLabel: String = "অ্যাপ স্টোরেজ + Downloads/ResultMaker",
    val csvHeaderStyle: CsvHeaderStyle = CsvHeaderStyle.FRIENDLY_BN,
    val csvBackupFormat: CsvBackupFormat = CsvBackupFormat.CSV,
    val csvSchedule: BackupScheduleConfig = BackupScheduleConfig(
        mode = BackupScheduleMode.DAILY,
        hour = 21,
        minute = 30,
        dayOfWeek = Calendar.FRIDAY
    ),
    val csvRetentionMode: BackupFileRetentionMode = BackupFileRetentionMode.SINGLE_LATEST,
    val lastCsvBackupMs: Long = 0L,
    val lastCsvFileName: String = "",
    val lastCsvError: String? = null,
    val csvSnapshots: List<LocalBackupEntry> = emptyList()
)

class AutoBackupManager(
    context: Context,
    private val userId: String
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("rm_auto_backup_$userId", Context.MODE_PRIVATE)
    private val localMutex = Mutex()
    private val csvMutex = Mutex()
    private val driveMutex = Mutex()

    private val _status = MutableStateFlow(loadInitialStatus())
    val status: StateFlow<AutoBackupStatus> = _status.asStateFlow()

    private fun loadSchedule(
        prefix: String,
        defaultMode: BackupScheduleMode,
        defaultHour: Int,
        defaultMinute: Int
    ): BackupScheduleConfig {
        val mode = BackupScheduleMode.fromId(prefs.getString("${prefix}_mode", defaultMode.id))
        val hour = prefs.getInt("${prefix}_hour", defaultHour).coerceIn(0, 23)
        val minute = prefs.getInt("${prefix}_minute", defaultMinute).coerceIn(0, 59)
        val dow = prefs.getInt("${prefix}_dow", Calendar.FRIDAY).coerceIn(Calendar.SUNDAY, Calendar.SATURDAY)
        return BackupScheduleConfig(mode = mode, hour = hour, minute = minute, dayOfWeek = dow)
    }

    private fun saveSchedule(prefix: String, cfg: BackupScheduleConfig) {
        prefs.edit()
            .putString("${prefix}_mode", cfg.mode.id)
            .putInt("${prefix}_hour", cfg.hour.coerceIn(0, 23))
            .putInt("${prefix}_minute", cfg.minute.coerceIn(0, 59))
            .putInt("${prefix}_dow", cfg.dayOfWeek.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY))
            .apply()
    }

    private fun loadInitialStatus(): AutoBackupStatus {
        val localFolderUri = prefs.getString(KEY_LOCAL_FOLDER_URI, null)
        val csvFolderUri = prefs.getString(KEY_CSV_FOLDER_URI, null)
        val csvHeaderStyle = CsvHeaderStyle.fromId(prefs.getString(KEY_CSV_HEADER_STYLE, CsvHeaderStyle.FRIENDLY_BN.id))
        val csvBackupFormat = CsvBackupFormat.fromId(prefs.getString(KEY_CSV_BACKUP_FORMAT, CsvBackupFormat.CSV.id))

        val localRetentionMode = BackupFileRetentionMode.fromId(prefs.getString(KEY_LOCAL_RETENTION_MODE, null))
        val driveRetentionMode = BackupFileRetentionMode.fromId(prefs.getString(KEY_DRIVE_RETENTION_MODE, null))
        val csvRetentionMode = BackupFileRetentionMode.fromId(prefs.getString(KEY_CSV_RETENTION_MODE, null))

        return AutoBackupStatus(
            localEnabled = prefs.getBoolean(KEY_LOCAL_ENABLED, true),
            localFolderUri = localFolderUri,
            localFolderLabel = buildFolderLabel(localFolderUri),
            localSchedule = loadSchedule(KEY_LOCAL_SCHED_PREFIX, BackupScheduleMode.ON_CHANGE, 21, 0),
            localRetentionMode = localRetentionMode,
            lastLocalBackupMs = prefs.getLong(KEY_LAST_LOCAL_MS, 0L),
            lastLocalFileName = prefs.getString(KEY_LAST_LOCAL_NAME, "").orEmpty(),

            driveEnabled = prefs.getBoolean(KEY_DRIVE_ENABLED, false),
            driveSchedule = loadSchedule(KEY_DRIVE_SCHED_PREFIX, BackupScheduleMode.DAILY, 22, 0),
            driveRetentionMode = driveRetentionMode,
            lastDriveBackupMs = prefs.getLong(KEY_LAST_DRIVE_MS, 0L),
            lastDriveFileName = prefs.getString(KEY_LAST_DRIVE_NAME, "").orEmpty(),

            csvEnabled = prefs.getBoolean(KEY_CSV_ENABLED, true),
            csvFolderUri = csvFolderUri,
            csvFolderLabel = buildFolderLabel(csvFolderUri),
            csvHeaderStyle = csvHeaderStyle,
            csvBackupFormat = csvBackupFormat,
            csvSchedule = loadSchedule(KEY_CSV_SCHED_PREFIX, BackupScheduleMode.DAILY, 21, 30),
            csvRetentionMode = csvRetentionMode,
            lastCsvBackupMs = prefs.getLong(KEY_LAST_CSV_MS, 0L),
            lastCsvFileName = prefs.getString(KEY_LAST_CSV_NAME, "").orEmpty()
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

    fun setLocalSchedule(config: BackupScheduleConfig) {
        saveSchedule(KEY_LOCAL_SCHED_PREFIX, config)
        _status.update { it.copy(localSchedule = config) }
    }

    fun setLocalRetentionMode(mode: BackupFileRetentionMode) {
        prefs.edit().putString(KEY_LOCAL_RETENTION_MODE, mode.id).apply()
        _status.update { it.copy(localRetentionMode = mode) }
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

    fun setDriveSchedule(config: BackupScheduleConfig) {
        saveSchedule(KEY_DRIVE_SCHED_PREFIX, config)
        _status.update { it.copy(driveSchedule = config) }
    }

    fun setDriveRetentionMode(mode: BackupFileRetentionMode) {
        prefs.edit().putString(KEY_DRIVE_RETENTION_MODE, mode.id).apply()
        _status.update { it.copy(driveRetentionMode = mode) }
    }

    fun setCsvAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CSV_ENABLED, enabled).apply()
        _status.update { it.copy(csvEnabled = enabled, lastCsvError = null) }
    }

    fun setCsvSchedule(config: BackupScheduleConfig) {
        saveSchedule(KEY_CSV_SCHED_PREFIX, config)
        _status.update { it.copy(csvSchedule = config) }
    }

    fun setCsvRetentionMode(mode: BackupFileRetentionMode) {
        prefs.edit().putString(KEY_CSV_RETENTION_MODE, mode.id).apply()
        _status.update { it.copy(csvRetentionMode = mode) }
    }

    fun setCsvHeaderStyle(style: CsvHeaderStyle) {
        prefs.edit().putString(KEY_CSV_HEADER_STYLE, style.id).apply()
        _status.update { it.copy(csvHeaderStyle = style) }
    }

    fun setCsvBackupFormat(format: CsvBackupFormat) {
        prefs.edit().putString(KEY_CSV_BACKUP_FORMAT, format.id).apply()
        _status.update { it.copy(csvBackupFormat = format) }
    }

    fun setCsvFolderUri(uri: Uri?) {
        val raw = uri?.toString()
        if (uri != null) {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        prefs.edit().putString(KEY_CSV_FOLDER_URI, raw).apply()
        _status.update {
            it.copy(
                csvFolderUri = raw,
                csvFolderLabel = buildFolderLabel(raw),
                lastCsvError = null
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

    fun refreshCsvSnapshots(schoolId: String) {
        val dir = schoolCsvBackupDir(schoolId)
        val entries = dir.listFiles { f -> f.isFile && (f.name.endsWith(".csv") || f.name.endsWith(".xlsx")) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { f ->
                LocalBackupEntry(
                    fileName = f.name,
                    path = f.absolutePath,
                    modifiedMs = f.lastModified(),
                    sizeBytes = f.length()
                )
            } ?: emptyList()
        _status.update { it.copy(csvSnapshots = entries) }
    }

    suspend fun runLocalBackupIfNeeded(
        schoolId: String,
        schoolCode: String,
        students: List<Student>,
        settings: Settings,
        force: Boolean = false,
        reportPrefs: ReportBackupPrefs? = Repo.loadReportBackupPrefs(appContext),
        sheetTemplates: List<BlankSheetQrMeta> = SheetTemplateCache(appContext).getAll()
    ): Result<String> = withContext(Dispatchers.IO) {
        localMutex.withLock {
            val st = _status.value
            if (!force && !st.localEnabled) {
                return@withLock Result.success("skipped")
            }
            val json = Repo.toJson(
                list = students,
                st = settings,
                reportPrefs = reportPrefs,
                sheetTemplates = sheetTemplates
            )
            val hash = sha256(json)
            val nowMs = System.currentTimeMillis()
            if (!force) {
                if (st.localSchedule.mode == BackupScheduleMode.ON_CHANGE) {
                    val prevHash = prefs.getString(KEY_LAST_LOCAL_HASH, "")
                    if (hash == prevHash) {
                        refreshLocalSnapshots(schoolId)
                        return@withLock Result.success("unchanged")
                    }
                } else {
                    val lastSlotMs = prefs.getLong(KEY_LAST_LOCAL_SLOT_MS, 0L)
                    if (!BackupScheduleCalculator.isScheduledDue(st.localSchedule, lastSlotMs, nowMs)) {
                        refreshLocalSnapshots(schoolId)
                        return@withLock Result.success("not_due")
                    }
                }
            }

            runCatching {
                val bytes = json.toByteArray(Charsets.UTF_8)
                val retentionMode = st.localRetentionMode
                val targetFileName = BackupFileNaming.resolveBackupFileName(
                    schoolCode = schoolCode,
                    extensionWithDot = ".json",
                    mode = retentionMode,
                    nowMs = nowMs
                )

                // 1. App-private rolling backup folder (atomic temp write)
                val dir = schoolBackupDir(schoolId)
                writeBytesAtomically(dir, "latest_backup.json", bytes)
                if (retentionMode == BackupFileRetentionMode.DAILY_HISTORY) {
                    writeBytesAtomically(dir, targetFileName, bytes)
                    pruneOldSnapshots(dir, prefix = "ResultMaker_", ext = ".json", keepDailyCount = 14)
                }

                // Also keep standard local Repo snapshot synchronized
                Repo.save(appContext, students, settings)

                // 2. Android 10+ Downloads/ResultMaker folder (user-accessible without permissions)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    writeToMediaStoreDownloads(
                        fileName = targetFileName,
                        mimeType = "application/json",
                        bytes = bytes
                    )
                }

                // 3. Optional user-picked SAF folder
                val folderUriStr = _status.value.localFolderUri
                if (!folderUriStr.isNullOrBlank()) {
                    writeToSafFolder(Uri.parse(folderUriStr), targetFileName, "application/json", bytes)
                }

                withContext(NonCancellable) {
                    prefs.edit()
                        .putString(KEY_LAST_LOCAL_HASH, hash)
                        .putLong(KEY_LAST_LOCAL_MS, nowMs)
                        .putLong(KEY_LAST_LOCAL_SLOT_MS, nowMs)
                        .putString(KEY_LAST_LOCAL_NAME, targetFileName)
                        .apply()

                    refreshLocalSnapshots(schoolId)
                    _status.update {
                        it.copy(
                            lastLocalBackupMs = nowMs,
                            lastLocalFileName = targetFileName,
                            lastLocalError = null
                        )
                    }
                }
                targetFileName
            }.onFailure { err ->
                _status.update {
                    it.copy(lastLocalError = err.localizedMessage ?: "লোকাল ব্যাকআপ ত্রুটি")
                }
            }
        }
    }

    suspend fun runCsvBackupIfNeeded(
        schoolId: String,
        schoolCode: String,
        students: List<Student>,
        settings: Settings,
        force: Boolean = false,
        formatOverride: CsvBackupFormat? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        csvMutex.withLock {
            val st = _status.value
            if (!force && !st.csvEnabled) {
                return@withLock Result.success("skipped")
            }
            val activeFormat = formatOverride ?: st.csvBackupFormat
            val combinedCsv = Repo.toCsv(
                list = students,
                customFields = settings.customFields,
                style = st.csvHeaderStyle,
                targetCls = null
            )
            val hash = sha256("${activeFormat.id}:${st.csvHeaderStyle.id}:$combinedCsv")
            val nowMs = System.currentTimeMillis()
            if (!force) {
                if (st.csvSchedule.mode == BackupScheduleMode.ON_CHANGE) {
                    val prevHash = prefs.getString(KEY_LAST_CSV_HASH, "")
                    if (hash == prevHash) {
                        refreshCsvSnapshots(schoolId)
                        return@withLock Result.success("unchanged")
                    }
                } else {
                    val lastSlotMs = prefs.getLong(KEY_LAST_CSV_SLOT_MS, 0L)
                    if (!BackupScheduleCalculator.isScheduledDue(st.csvSchedule, lastSlotMs, nowMs)) {
                        refreshCsvSnapshots(schoolId)
                        return@withLock Result.success("not_due")
                    }
                }
            }

            runCatching {
                val retentionMode = st.csvRetentionMode
                val dir = schoolCsvBackupDir(schoolId)
                val folderUriStr = _status.value.csvFolderUri

                // Remove any legacy per-class split files from earlier versions so only single clean files remain
                dir.listFiles { f ->
                    f.isFile && (f.name.startsWith("latest_class_") || f.name.contains("_Class"))
                }?.forEach { runCatching { it.delete() } }

                val outputName: String
                when (activeFormat) {
                    CsvBackupFormat.CSV -> {
                        val csvBytes = combinedCsv.toByteArray(Charsets.UTF_8)
                        val targetFileName = BackupFileNaming.resolveBackupFileName(
                            schoolCode = schoolCode,
                            extensionWithDot = ".csv",
                            mode = retentionMode,
                            nowMs = nowMs
                        )
                        writeBytesAtomically(dir, "latest_backup.csv", csvBytes)
                        if (retentionMode == BackupFileRetentionMode.DAILY_HISTORY) {
                            writeBytesAtomically(dir, targetFileName, csvBytes)
                            pruneOldSnapshots(dir, prefix = "ResultMaker_", ext = ".csv", keepDailyCount = 14)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            writeToMediaStoreDownloads(
                                fileName = targetFileName,
                                mimeType = "text/csv",
                                bytes = csvBytes
                            )
                        }
                        if (!folderUriStr.isNullOrBlank()) {
                            writeToSafFolder(Uri.parse(folderUriStr), targetFileName, "text/csv", csvBytes)
                        }
                        outputName = targetFileName
                    }
                    CsvBackupFormat.EXCEL -> {
                        val knownIds = (com.kgps.result.activeClassIds(settings) + students.map { it.cls }.filter { it in 1..20 })
                            .distinct()
                            .sorted()
                        val activeClasses = knownIds.filter { c -> students.any { it.cls == c } }.ifEmpty { knownIds }
                        val xlsxBytes = XlsxExporter.exportAllClassesWorkbook(
                            students = students,
                            customFields = settings.customFields,
                            style = st.csvHeaderStyle,
                            classes = activeClasses
                        )
                        val targetFileName = BackupFileNaming.resolveBackupFileName(
                            schoolCode = schoolCode,
                            extensionWithDot = ".xlsx",
                            mode = retentionMode,
                            nowMs = nowMs
                        )
                        val xlsxMime = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        writeBytesAtomically(dir, "latest_all_classes.xlsx", xlsxBytes)
                        if (retentionMode == BackupFileRetentionMode.DAILY_HISTORY) {
                            writeBytesAtomically(dir, targetFileName, xlsxBytes)
                            pruneOldSnapshots(dir, prefix = "ResultMaker_", ext = ".xlsx", keepDailyCount = 14)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            writeToMediaStoreDownloads(
                                fileName = targetFileName,
                                mimeType = xlsxMime,
                                bytes = xlsxBytes
                            )
                        }
                        if (!folderUriStr.isNullOrBlank()) {
                            writeToSafFolder(Uri.parse(folderUriStr), targetFileName, xlsxMime, xlsxBytes)
                        }
                        outputName = targetFileName
                    }
                }

                withContext(NonCancellable) {
                    prefs.edit()
                        .putString(KEY_LAST_CSV_HASH, hash)
                        .putLong(KEY_LAST_CSV_MS, nowMs)
                        .putLong(KEY_LAST_CSV_SLOT_MS, nowMs)
                        .putString(KEY_LAST_CSV_NAME, outputName)
                        .apply()

                    refreshCsvSnapshots(schoolId)
                    _status.update {
                        it.copy(
                            lastCsvBackupMs = nowMs,
                            lastCsvFileName = outputName,
                            lastCsvError = null
                        )
                    }
                }
                outputName
            }.onFailure { err ->
                _status.update {
                    it.copy(lastCsvError = err.localizedMessage ?: "ব্যাকআপ ত্রুটি")
                }
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
        force: Boolean = false,
        enableAutoOnSuccess: Boolean = false,
        reportPrefs: ReportBackupPrefs? = Repo.loadReportBackupPrefs(appContext),
        sheetTemplates: List<BlankSheetQrMeta> = SheetTemplateCache(appContext).getAll()
    ): DriveAuthOutcome = driveMutex.withLock {
        val st = _status.value
        if (!force && !st.driveEnabled) {
            return@withLock DriveAuthOutcome.Failed("disabled")
        }
        val json = Repo.toJson(
            list = students,
            st = settings,
            reportPrefs = reportPrefs,
            sheetTemplates = sheetTemplates
        )
        val hash = sha256(json)
        val prevHash = prefs.getString(KEY_LAST_DRIVE_HASH, "")
        val lastMs = prefs.getLong(KEY_LAST_DRIVE_MS, 0L)
        val nowMs = System.currentTimeMillis()
        val elapsedMs = nowMs - lastMs

        if (!force) {
            if (st.driveSchedule.mode == BackupScheduleMode.ON_CHANGE) {
                if (hash == prevHash) {
                    return@withLock DriveAuthOutcome.Authorized("unchanged")
                }
                if (elapsedMs < MIN_AUTO_DRIVE_INTERVAL_MS) {
                    return@withLock DriveAuthOutcome.Authorized("throttled")
                }
            } else {
                val lastSlotMs = prefs.getLong(KEY_LAST_DRIVE_SLOT_MS, 0L)
                if (!BackupScheduleCalculator.isScheduledDue(st.driveSchedule, lastSlotMs, nowMs)) {
                    return@withLock DriveAuthOutcome.Authorized("not_due")
                }
                if (hash == prevHash && lastMs > 0L) {
                    prefs.edit().putLong(KEY_LAST_DRIVE_SLOT_MS, nowMs).apply()
                    return@withLock DriveAuthOutcome.Authorized("unchanged")
                }
            }
        }

        _status.update { it.copy(driveBusy = true, lastDriveError = null) }
        try {
            when (val auth = GoogleDriveBackupClient.authorizeDrive(appContext)) {
                is DriveAuthOutcome.NeedsConsent -> {
                    _status.update { it.copy(driveNeedsConsent = true) }
                    auth
                }
                is DriveAuthOutcome.Failed -> {
                    _status.update { it.copy(lastDriveError = auth.message) }
                    auth
                }
                is DriveAuthOutcome.Authorized -> {
                    val uploadRes = uploadWithTokenLocked(
                        accessToken = auth.accessToken,
                        schoolCode = schoolCode,
                        jsonContent = json,
                        hash = hash,
                        enableAutoOnSuccess = enableAutoOnSuccess
                    )
                    if (uploadRes.isSuccess) {
                        auth
                    } else {
                        DriveAuthOutcome.Failed(
                            uploadRes.exceptionOrNull()?.localizedMessage ?: "Google Drive ব্যাকআপ ব্যর্থ হয়েছে"
                        )
                    }
                }
            }
        } finally {
            withContext(NonCancellable) {
                _status.update { it.copy(driveBusy = false) }
            }
        }
    }

    suspend fun uploadWithToken(
        accessToken: String,
        schoolCode: String,
        jsonContent: String,
        hash: String = sha256(jsonContent),
        enableAutoOnSuccess: Boolean = false
    ): Result<DriveBackupFile> = driveMutex.withLock {
        _status.update { it.copy(driveBusy = true, driveNeedsConsent = false, lastDriveError = null) }
        return@withLock try {
            uploadWithTokenLocked(
                accessToken = accessToken,
                schoolCode = schoolCode,
                jsonContent = jsonContent,
                hash = hash,
                enableAutoOnSuccess = enableAutoOnSuccess
            )
        } finally {
            withContext(NonCancellable) {
                _status.update { it.copy(driveBusy = false) }
            }
        }
    }

    private suspend fun uploadWithTokenLocked(
        accessToken: String,
        schoolCode: String,
        jsonContent: String,
        hash: String,
        enableAutoOnSuccess: Boolean
    ): Result<DriveBackupFile> {
        val fileName = BackupFileNaming.resolveBackupFileName(
            schoolCode = schoolCode,
            extensionWithDot = ".json",
            mode = _status.value.driveRetentionMode,
            nowMs = System.currentTimeMillis()
        )

        val result = GoogleDriveBackupClient.uploadBackupJson(accessToken, fileName, jsonContent)
        result.onSuccess { uploaded ->
            val nowMs = System.currentTimeMillis()
            withContext(NonCancellable) {
                prefs.edit().apply {
                    putString(KEY_LAST_DRIVE_HASH, hash)
                    putLong(KEY_LAST_DRIVE_MS, nowMs)
                    putLong(KEY_LAST_DRIVE_SLOT_MS, nowMs)
                    putString(KEY_LAST_DRIVE_NAME, uploaded.name)
                    if (enableAutoOnSuccess) {
                        putBoolean(KEY_DRIVE_ENABLED, true)
                    }
                }.apply()
                _status.update { cur ->
                    cur.copy(
                        driveEnabled = if (enableAutoOnSuccess) true else cur.driveEnabled,
                        driveBusy = false,
                        driveNeedsConsent = false,
                        lastDriveBackupMs = nowMs,
                        lastDriveFileName = uploaded.name,
                        lastDriveError = null
                    )
                }
            }
        }.onFailure { err ->
            withContext(NonCancellable) {
                _status.update {
                    it.copy(
                        driveBusy = false,
                        lastDriveError = err.localizedMessage ?: "Google Drive ব্যাকআপ ব্যর্থ হয়েছে"
                    )
                }
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

    private fun schoolCsvBackupDir(schoolId: String): File {
        val dir = File(schoolBackupDir(schoolId), "csv")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun pruneOldSnapshots(dir: File, prefix: String, ext: String, keepDailyCount: Int) {
        val dailyFiles = dir.listFiles { f ->
            f.isFile && f.name.startsWith(prefix) && f.name.endsWith(ext)
        }?.sortedByDescending { it.lastModified() } ?: return
        dailyFiles.drop(keepDailyCount).forEach { runCatching { it.delete() } }
    }

    private fun writeToMediaStoreDownloads(fileName: String, mimeType: String, bytes: ByteArray) {
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
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
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

    private fun writeBytesAtomically(dir: File, targetName: String, bytes: ByteArray) {
        if (!dir.exists()) dir.mkdirs()
        val target = File(dir, targetName)
        val tmp = File(dir, "$targetName.tmp")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    private fun writeToSafFolder(treeUri: Uri, fileName: String, mimeType: String, bytes: ByteArray) {
        val dirDoc = DocumentFile.fromTreeUri(appContext, treeUri)
        if (dirDoc == null || !dirDoc.exists() || !dirDoc.canWrite()) {
            throw IllegalStateException("নির্বাচিত ব্যাকআপ ফোল্ডারে লেখার অনুমতি নেই")
        }
        val existing = dirDoc.findFile(fileName)
        val target = existing ?: dirDoc.createFile(mimeType, fileName)
            ?: throw IllegalStateException("নির্বাচিত ফোল্ডারে $fileName তৈরি করা যায়নি")
        val stream = try {
            appContext.contentResolver.openOutputStream(target.uri, "wt")
        } catch (_: IllegalArgumentException) {
            appContext.contentResolver.openOutputStream(target.uri, "w")
        } catch (_: UnsupportedOperationException) {
            appContext.contentResolver.openOutputStream(target.uri, "w")
        } ?: appContext.contentResolver.openOutputStream(target.uri)
            ?: throw IllegalStateException("নির্বাচিত ফোল্ডারে ফাইল স্ট্রিম খোলা যায়নি")
        stream.use { out ->
            out.write(bytes)
            out.flush()
        }
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_LOCAL_ENABLED = "local_enabled"
        private const val KEY_LOCAL_FOLDER_URI = "local_folder_uri"
        private const val KEY_LOCAL_SCHED_PREFIX = "local_sched"
        private const val KEY_LOCAL_RETENTION_MODE = "local_retention_mode"
        private const val KEY_LAST_LOCAL_HASH = "last_local_hash"
        private const val KEY_LAST_LOCAL_MS = "last_local_ms"
        private const val KEY_LAST_LOCAL_SLOT_MS = "last_local_slot_ms"
        private const val KEY_LAST_LOCAL_NAME = "last_local_name"

        private const val KEY_DRIVE_ENABLED = "drive_enabled"
        private const val KEY_DRIVE_SCHED_PREFIX = "drive_sched"
        private const val KEY_DRIVE_RETENTION_MODE = "drive_retention_mode"
        private const val KEY_LAST_DRIVE_HASH = "last_drive_hash"
        private const val KEY_LAST_DRIVE_MS = "last_drive_ms"
        private const val KEY_LAST_DRIVE_SLOT_MS = "last_drive_slot_ms"
        private const val KEY_LAST_DRIVE_NAME = "last_drive_name"

        private const val KEY_CSV_ENABLED = "csv_enabled"
        private const val KEY_CSV_FOLDER_URI = "csv_folder_uri"
        private const val KEY_CSV_HEADER_STYLE = "csv_header_style"
        private const val KEY_CSV_BACKUP_FORMAT = "csv_backup_format"
        private const val KEY_CSV_SCHED_PREFIX = "csv_sched"
        private const val KEY_CSV_RETENTION_MODE = "csv_retention_mode"
        private const val KEY_LAST_CSV_HASH = "last_csv_hash"
        private const val KEY_LAST_CSV_MS = "last_csv_ms"
        private const val KEY_LAST_CSV_SLOT_MS = "last_csv_slot_ms"
        private const val KEY_LAST_CSV_NAME = "last_csv_name"

        private const val MIN_AUTO_DRIVE_INTERVAL_MS = 60_000L
    }
}
