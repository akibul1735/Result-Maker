package com.kgps.result

import com.kgps.result.backup.AutoBackupStatus
import com.kgps.result.backup.BackupFileNaming
import com.kgps.result.backup.BackupFileRetentionMode
import com.kgps.result.backup.CsvBackupFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AutoBackupRetentionTest {

    @Test
    fun `retention mode defaults to single latest on null or unknown id`() {
        assertEquals(BackupFileRetentionMode.SINGLE_LATEST, BackupFileRetentionMode.fromId(null))
        assertEquals(BackupFileRetentionMode.SINGLE_LATEST, BackupFileRetentionMode.fromId(""))
        assertEquals(BackupFileRetentionMode.SINGLE_LATEST, BackupFileRetentionMode.fromId("unknown"))
        assertEquals(BackupFileRetentionMode.SINGLE_LATEST, BackupFileRetentionMode.fromId("single"))
        assertEquals(BackupFileRetentionMode.DAILY_HISTORY, BackupFileRetentionMode.fromId("daily"))
    }

    @Test
    fun `auto backup status defaults all three systems to single latest update mode`() {
        val status = AutoBackupStatus()
        assertEquals(BackupFileRetentionMode.SINGLE_LATEST, status.localRetentionMode)
        assertEquals(BackupFileRetentionMode.SINGLE_LATEST, status.driveRetentionMode)
        assertEquals(BackupFileRetentionMode.SINGLE_LATEST, status.csvRetentionMode)
    }

    @Test
    fun `single latest naming uses stable fixed filename across days for json csv and xlsx`() {
        val dayOneMs = utcMillis(2026, Calendar.NOVEMBER, 28, 10, 0)
        val dayTwoMs = utcMillis(2026, Calendar.NOVEMBER, 29, 22, 30)

        assertEquals(
            "ResultMaker_KGPS01_latest.json",
            BackupFileNaming.resolveBackupFileName("KGPS-01", ".json", BackupFileRetentionMode.SINGLE_LATEST, dayOneMs)
        )
        assertEquals(
            "ResultMaker_KGPS01_latest.json",
            BackupFileNaming.resolveBackupFileName("KGPS-01", ".json", BackupFileRetentionMode.SINGLE_LATEST, dayTwoMs)
        )
        assertEquals(
            "ResultMaker_KGPS01_latest.csv",
            BackupFileNaming.resolveBackupFileName("KGPS-01", ".csv", BackupFileRetentionMode.SINGLE_LATEST, dayOneMs)
        )
        assertEquals(
            "ResultMaker_KGPS01_latest.xlsx",
            BackupFileNaming.resolveBackupFileName("KGPS-01", ".xlsx", BackupFileRetentionMode.SINGLE_LATEST, dayTwoMs)
        )
    }

    @Test
    fun `daily history naming includes yyyy-MM-dd date stamp and sanitizes blank school code`() {
        val sampleMs = utcMillis(2026, Calendar.DECEMBER, 5, 9, 15)

        val expectedTag = BackupFileNaming.formatDateTag(sampleMs)
        assertEquals(
            "ResultMaker_SCHOOL_$expectedTag.json",
            BackupFileNaming.resolveBackupFileName("...", ".json", BackupFileRetentionMode.DAILY_HISTORY, sampleMs)
        )
        assertEquals(
            "ResultMaker_ABC12_$expectedTag.csv",
            BackupFileNaming.resolveBackupFileName("ABC 12", "csv", BackupFileRetentionMode.DAILY_HISTORY, sampleMs)
        )
        assertEquals(
            "ResultMaker_ABC12_$expectedTag.xlsx",
            BackupFileNaming.resolveBackupFileName("ABC 12", ".xlsx", BackupFileRetentionMode.DAILY_HISTORY, sampleMs)
        )
        assertEquals(CsvBackupFormat.CSV, CsvBackupFormat.fromId(null))
    }

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, month, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
