package com.kgps.result.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kgps.result.BackupBundle
import com.kgps.result.DEFAULT_SETTINGS
import com.kgps.result.Repo
import com.kgps.result.cloud.SheetTemplateCache
import java.io.File
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val kindName = inputData.getString(KEY_KIND) ?: return Result.success()
        val kind = runCatching { BackupSystemKind.valueOf(kindName) }.getOrNull() ?: return Result.success()

        val metaPrefs = applicationContext.getSharedPreferences(PREFS_META, Context.MODE_PRIVATE)
        val userId = inputData.getString(KEY_USER_ID)?.takeIf { it.isNotBlank() }
            ?: metaPrefs.getString(KEY_META_USER_ID, "")?.takeIf { it.isNotBlank() }
            ?: return Result.success()
        val schoolId = inputData.getString(KEY_SCHOOL_ID)?.takeIf { it.isNotBlank() }
            ?: metaPrefs.getString(KEY_META_SCHOOL_ID, "")?.takeIf { it.isNotBlank() }
            ?: return Result.success()
        val schoolCode = inputData.getString(KEY_SCHOOL_CODE)?.takeIf { it.isNotBlank() }
            ?: metaPrefs.getString(KEY_META_SCHOOL_CODE, "")?.takeIf { it.isNotBlank() }
            ?: "SCHOOL"

        val bundle = loadBestCachedData(schoolId) ?: return Result.success()
        val students = bundle.students
        val settings = bundle.settings
        if (students.isEmpty() && settings == DEFAULT_SETTINGS) return Result.success()

        val effectiveReportPrefs = bundle.reportPrefs ?: Repo.loadReportBackupPrefs(applicationContext)
        val effectiveTemplates = (bundle.sheetTemplates + SheetTemplateCache(applicationContext).getAll())
            .distinctBy { it.templateId.trim().uppercase() }

        val manager = AutoBackupManager(applicationContext, userId)
        val st = manager.status.value

        when (kind) {
            BackupSystemKind.LOCAL -> {
                if (st.localEnabled && st.localSchedule.mode != BackupScheduleMode.ON_CHANGE) {
                    manager.runLocalBackupIfNeeded(
                        schoolId = schoolId,
                        schoolCode = schoolCode,
                        students = students,
                        settings = settings,
                        force = false,
                        reportPrefs = effectiveReportPrefs,
                        sheetTemplates = effectiveTemplates
                    )
                    BackupWorkScheduler.scheduleKind(
                        context = applicationContext,
                        kind = kind,
                        userId = userId,
                        schoolId = schoolId,
                        schoolCode = schoolCode,
                        enabled = st.localEnabled,
                        schedule = st.localSchedule
                    )
                }
            }
            BackupSystemKind.DRIVE -> {
                if (st.driveEnabled && st.driveSchedule.mode != BackupScheduleMode.ON_CHANGE) {
                    manager.runDriveBackupIfNeeded(
                        schoolCode = schoolCode,
                        students = students,
                        settings = settings,
                        force = false,
                        enableAutoOnSuccess = false,
                        reportPrefs = effectiveReportPrefs,
                        sheetTemplates = effectiveTemplates
                    )
                    BackupWorkScheduler.scheduleKind(
                        context = applicationContext,
                        kind = kind,
                        userId = userId,
                        schoolId = schoolId,
                        schoolCode = schoolCode,
                        enabled = st.driveEnabled,
                        schedule = st.driveSchedule
                    )
                }
            }
            BackupSystemKind.CSV -> {
                if (st.csvEnabled && st.csvSchedule.mode != BackupScheduleMode.ON_CHANGE) {
                    manager.runCsvBackupIfNeeded(
                        schoolId = schoolId,
                        schoolCode = schoolCode,
                        students = students,
                        settings = settings,
                        force = false
                    )
                    BackupWorkScheduler.scheduleKind(
                        context = applicationContext,
                        kind = kind,
                        userId = userId,
                        schoolId = schoolId,
                        schoolCode = schoolCode,
                        enabled = st.csvEnabled,
                        schedule = st.csvSchedule
                    )
                }
            }
        }
        return Result.success()
    }

    private fun loadBestCachedData(schoolId: String): BackupBundle? = runCatching {
        val safeId = schoolId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "default" }
        val schoolLatest = File(applicationContext.filesDir, "auto_backups/$safeId/latest_backup.json")
        if (schoolLatest.exists()) {
            Repo.fromBackupJson(schoolLatest.readText(Charsets.UTF_8))
        } else {
            val dataFile = File(applicationContext.filesDir, "data.json")
            if (dataFile.exists()) Repo.fromBackupJson(dataFile.readText(Charsets.UTF_8)) else null
        }
    }.getOrNull()

    companion object {
        const val KEY_KIND = "backup_kind"
        const val KEY_USER_ID = "user_id"
        const val KEY_SCHOOL_ID = "school_id"
        const val KEY_SCHOOL_CODE = "school_code"
        const val PREFS_META = "rm_backup_scheduler_meta"
        const val KEY_META_USER_ID = "meta_user_id"
        const val KEY_META_SCHOOL_ID = "meta_school_id"
        const val KEY_META_SCHOOL_CODE = "meta_school_code"
    }
}

object BackupWorkScheduler {
    fun saveActiveMeta(context: Context, userId: String, schoolId: String, schoolCode: String) {
        context.applicationContext
            .getSharedPreferences(AutoBackupWorker.PREFS_META, Context.MODE_PRIVATE)
            .edit()
            .putString(AutoBackupWorker.KEY_META_USER_ID, userId)
            .putString(AutoBackupWorker.KEY_META_SCHOOL_ID, schoolId)
            .putString(AutoBackupWorker.KEY_META_SCHOOL_CODE, schoolCode)
            .apply()
    }

    fun syncAllSchedules(
        context: Context,
        userId: String,
        schoolId: String,
        schoolCode: String,
        status: AutoBackupStatus
    ) {
        saveActiveMeta(context, userId, schoolId, schoolCode)
        scheduleKind(context, BackupSystemKind.LOCAL, userId, schoolId, schoolCode, status.localEnabled, status.localSchedule)
        scheduleKind(context, BackupSystemKind.DRIVE, userId, schoolId, schoolCode, status.driveEnabled, status.driveSchedule)
        scheduleKind(context, BackupSystemKind.CSV, userId, schoolId, schoolCode, status.csvEnabled, status.csvSchedule)
    }

    fun scheduleKind(
        context: Context,
        kind: BackupSystemKind,
        userId: String,
        schoolId: String,
        schoolCode: String,
        enabled: Boolean,
        schedule: BackupScheduleConfig
    ) {
        runCatching {
            val wm = WorkManager.getInstance(context.applicationContext)
            val uniqueName = "rm_auto_backup_${kind.name.lowercase()}_$userId"
            if (!enabled || schedule.mode == BackupScheduleMode.ON_CHANGE) {
                wm.cancelUniqueWork(uniqueName)
                return@runCatching
            }

            val nowMs = System.currentTimeMillis()
            val nextMs = BackupScheduleCalculator.nextScheduledSlotMs(schedule, nowMs)
            val delayMs = (nextMs - nowMs).coerceAtLeast(15_000L)

            val input = Data.Builder()
                .putString(AutoBackupWorker.KEY_KIND, kind.name)
                .putString(AutoBackupWorker.KEY_USER_ID, userId)
                .putString(AutoBackupWorker.KEY_SCHOOL_ID, schoolId)
                .putString(AutoBackupWorker.KEY_SCHOOL_CODE, schoolCode)
                .build()

            val constraintsBuilder = Constraints.Builder()
            if (kind == BackupSystemKind.DRIVE) {
                constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)
            }

            val req = OneTimeWorkRequestBuilder<AutoBackupWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .setConstraints(constraintsBuilder.build())
                .build()

            wm.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, req)
        }
    }
}
