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
    REPORT_CARD("report_card", 4),
    SEAT_PLAN_PDF("seat_plan_pdf", 5),
    TESTIMONIAL_PDF("testimonial_pdf", 6),
    BATCH_EDIT("batch_edit", 7),
    DATA_IMPORT("data_import", 8)
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

    @Volatile
    private var sessionSnapshot: Map<DashboardQuickToolId, ToolUsageStat>? = null

    /**
     * Loads and freezes tool usage stats for the current app session so quick-action positions
     * remain stable while working, and updates take effect on the next app session.
     */
    fun getSessionSnapshotStats(context: Context): Map<DashboardQuickToolId, ToolUsageStat> {
        sessionSnapshot?.let { return it }
        return synchronized(this) {
            sessionSnapshot ?: loadFromPrefs(context).also { sessionSnapshot = it }
        }
    }

    fun loadFromPrefs(context: Context): Map<DashboardQuickToolId, ToolUsageStat> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return DashboardQuickToolId.entries.associateWith { id ->
            when (id) {
                DashboardQuickToolId.REPORT_CARD -> {
                    val c = prefs.getInt(KEY_COUNT_PREFIX + id.key, 0).coerceAtLeast(0) +
                        prefs.getInt(KEY_COUNT_PREFIX + "batch_print", 0).coerceAtLeast(0) +
                        prefs.getInt(KEY_COUNT_PREFIX + "all_pdf", 0).coerceAtLeast(0)
                    val l = maxOf(
                        prefs.getLong(KEY_LAST_PREFIX + id.key, 0L).coerceAtLeast(0L),
                        prefs.getLong(KEY_LAST_PREFIX + "batch_print", 0L).coerceAtLeast(0L),
                        prefs.getLong(KEY_LAST_PREFIX + "all_pdf", 0L).coerceAtLeast(0L)
                    )
                    ToolUsageStat(count = c, lastUsedMs = l)
                }

                DashboardQuickToolId.DATA_IMPORT -> {
                    val c = prefs.getInt(KEY_COUNT_PREFIX + id.key, 0).coerceAtLeast(0) +
                        prefs.getInt(KEY_COUNT_PREFIX + "csv_import", 0).coerceAtLeast(0) +
                        prefs.getInt(KEY_COUNT_PREFIX + "basic_import", 0).coerceAtLeast(0)
                    val l = maxOf(
                        prefs.getLong(KEY_LAST_PREFIX + id.key, 0L).coerceAtLeast(0L),
                        prefs.getLong(KEY_LAST_PREFIX + "csv_import", 0L).coerceAtLeast(0L),
                        prefs.getLong(KEY_LAST_PREFIX + "basic_import", 0L).coerceAtLeast(0L)
                    )
                    ToolUsageStat(count = c, lastUsedMs = l)
                }

                else -> ToolUsageStat(
                    count = prefs.getInt(KEY_COUNT_PREFIX + id.key, 0).coerceAtLeast(0),
                    lastUsedMs = prefs.getLong(KEY_LAST_PREFIX + id.key, 0L).coerceAtLeast(0L)
                )
            }
        }
    }

    /**
     * Persists the incremented usage count to SharedPreferences without mutating the current
     * session snapshot, so reordering only applies on the next session.
     */
    fun recordUsage(
        context: Context,
        toolId: DashboardQuickToolId,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextCount = prefs.getInt(KEY_COUNT_PREFIX + toolId.key, 0).coerceAtLeast(0) + 1
        prefs.edit()
            .putInt(KEY_COUNT_PREFIX + toolId.key, nextCount)
            .putLong(KEY_LAST_PREFIX + toolId.key, nowMs)
            .apply()
    }

    internal fun resetSessionSnapshotForTest() {
        sessionSnapshot = null
    }
}
