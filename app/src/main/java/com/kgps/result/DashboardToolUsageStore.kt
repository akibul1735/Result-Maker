package com.kgps.result

import android.content.Context

enum class DashboardQuickToolId(
    val key: String,
    val defaultPriority: Int
) {
    TABULATION_PDF("tabulation_pdf", 0),
    ADMIT_CARD_PDF("admit_card_pdf", 1),
    BLANK_SHEET_PDF("blank_sheet_pdf", 2),
    AI_SCAN("ai_scan", 3),
    ALL_PDF("all_pdf", 4),
    BATCH_PRINT("batch_print", 5),
    SEAT_PLAN_PDF("seat_plan_pdf", 6),
    BACKUP_NOW("backup_now", 7),
    BATCH_EDIT("batch_edit", 8),
    CSV_IMPORT("csv_import", 9),
    BASIC_IMPORT("basic_import", 10)
}

data class ToolUsageStat(
    val count: Int = 0,
    val lastUsedMs: Long = 0L
)

fun partitionQuickTools(
    availableIds: List<DashboardQuickToolId>,
    stats: Map<DashboardQuickToolId, ToolUsageStat>,
    primaryLimit: Int = 4
): Pair<List<DashboardQuickToolId>, List<DashboardQuickToolId>> {
    val sorted = availableIds.sortedWith(
        compareByDescending<DashboardQuickToolId> { stats[it]?.count ?: 0 }
            .thenByDescending { stats[it]?.lastUsedMs ?: 0L }
            .thenBy { it.defaultPriority }
    )
    return sorted.take(primaryLimit) to sorted.drop(primaryLimit)
}

object DashboardToolUsageStore {
    private const val PREFS_NAME = "kgps_dashboard_tool_usage"
    private const val KEY_COUNT_PREFIX = "count_"
    private const val KEY_LAST_PREFIX = "last_"

    fun loadStats(context: Context): Map<DashboardQuickToolId, ToolUsageStat> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return DashboardQuickToolId.entries.associateWith { id ->
            ToolUsageStat(
                count = prefs.getInt(KEY_COUNT_PREFIX + id.key, 0).coerceAtLeast(0),
                lastUsedMs = prefs.getLong(KEY_LAST_PREFIX + id.key, 0L).coerceAtLeast(0L)
            )
        }
    }

    fun recordUsage(
        context: Context,
        toolId: DashboardQuickToolId,
        nowMs: Long = System.currentTimeMillis()
    ): Map<DashboardQuickToolId, ToolUsageStat> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextCount = prefs.getInt(KEY_COUNT_PREFIX + toolId.key, 0).coerceAtLeast(0) + 1
        prefs.edit()
            .putInt(KEY_COUNT_PREFIX + toolId.key, nextCount)
            .putLong(KEY_LAST_PREFIX + toolId.key, nowMs)
            .apply()
        return loadStats(context)
    }
}
