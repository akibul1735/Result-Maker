package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardToolUsageTest {

    @Test
    fun `partitionQuickTools keeps default top 4 when no usage exists`() {
        val all = DashboardQuickToolId.entries.toList()
        val (primary, more) = partitionQuickTools(all, emptyMap())

        assertEquals(
            listOf(
                DashboardQuickToolId.TABULATION_PDF,
                DashboardQuickToolId.ADMIT_CARD_PDF,
                DashboardQuickToolId.BLANK_SHEET_PDF,
                DashboardQuickToolId.AI_SCAN
            ),
            primary
        )
        assertEquals(
            listOf(
                DashboardQuickToolId.REPORT_CARD,
                DashboardQuickToolId.SEAT_PLAN_PDF,
                DashboardQuickToolId.TESTIMONIAL_PDF,
                DashboardQuickToolId.BATCH_EDIT,
                DashboardQuickToolId.DATA_IMPORT
            ),
            more
        )
    }

    @Test
    fun `more frequently used tool from overflow promotes into top 4`() {
        val all = DashboardQuickToolId.entries.toList()
        val stats = mapOf(
            DashboardQuickToolId.SEAT_PLAN_PDF to ToolUsageStat(count = 5, lastUsedMs = 5000L),
            DashboardQuickToolId.DATA_IMPORT to ToolUsageStat(count = 3, lastUsedMs = 4000L),
            DashboardQuickToolId.TABULATION_PDF to ToolUsageStat(count = 2, lastUsedMs = 3000L),
            DashboardQuickToolId.ADMIT_CARD_PDF to ToolUsageStat(count = 1, lastUsedMs = 2000L)
        )

        val (primary, more) = partitionQuickTools(all, stats)

        assertEquals(
            listOf(
                DashboardQuickToolId.SEAT_PLAN_PDF,
                DashboardQuickToolId.DATA_IMPORT,
                DashboardQuickToolId.TABULATION_PDF,
                DashboardQuickToolId.ADMIT_CARD_PDF
            ),
            primary
        )
        assertTrue(DashboardQuickToolId.BLANK_SHEET_PDF in more)
        assertTrue(DashboardQuickToolId.AI_SCAN in more)
    }

    @Test
    fun `equal usage count breaks ties by most recently used tool`() {
        val all = DashboardQuickToolId.entries.toList()
        val stats = mapOf(
            DashboardQuickToolId.REPORT_CARD to ToolUsageStat(count = 2, lastUsedMs = 9000L),
            DashboardQuickToolId.TABULATION_PDF to ToolUsageStat(count = 2, lastUsedMs = 1000L),
            DashboardQuickToolId.ADMIT_CARD_PDF to ToolUsageStat(count = 2, lastUsedMs = 2000L),
            DashboardQuickToolId.BLANK_SHEET_PDF to ToolUsageStat(count = 2, lastUsedMs = 3000L),
            DashboardQuickToolId.AI_SCAN to ToolUsageStat(count = 2, lastUsedMs = 4000L)
        )

        val (primary, more) = partitionQuickTools(all, stats)

        assertEquals(
            listOf(
                DashboardQuickToolId.REPORT_CARD,
                DashboardQuickToolId.AI_SCAN,
                DashboardQuickToolId.BLANK_SHEET_PDF,
                DashboardQuickToolId.ADMIT_CARD_PDF
            ),
            primary
        )
        assertTrue(DashboardQuickToolId.TABULATION_PDF in more)
    }

    @Test
    fun `restricted tool list respects availability and caps at 4`() {
        val subset = listOf(
            DashboardQuickToolId.AI_SCAN,
            DashboardQuickToolId.DATA_IMPORT
        )
        val stats = mapOf(
            DashboardQuickToolId.DATA_IMPORT to ToolUsageStat(count = 3, lastUsedMs = 1000L)
        )

        val (primary, more) = partitionQuickTools(subset, stats)

        assertEquals(
            listOf(DashboardQuickToolId.DATA_IMPORT, DashboardQuickToolId.AI_SCAN),
            primary
        )
        assertTrue(more.isEmpty())
    }
}
