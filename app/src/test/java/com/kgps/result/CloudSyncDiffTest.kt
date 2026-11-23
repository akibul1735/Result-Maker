package com.kgps.result

import com.kgps.result.cloud.StudentSyncDiffHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncDiffTest {

    private val deleteSentinel = Any()
    private val updatedAtSentinel = "SERVER_TIME"

    @Test
    fun unchangedStudentProducesNullUpdateDiff() {
        val base = Student(
            id = "stu-1",
            cls = 3,
            roll = 5,
            name = "রিফাত হোসেন",
            guardian = "কামাল হোসেন",
            village = "পাঠানপাড়া",
            union = "সদরপুর",
            post = "সদর-৭১০০",
            upazila = DEFAULT_UPAZILA,
            district = DEFAULT_DISTRICT,
            mobile = "01711112222",
            religion = "ইসলাম",
            m = mapOf("BAN_1_WR" to 42.0, "MATH_1_WR" to 48.0),
            x = mapOf("DAYS_1" to "60", "PRES_1" to "58")
        )

        val diff = StudentSyncDiffHelper.buildUpdateDiff(
            prev = base,
            next = base.copy(),
            uid = "teacher-a",
            userName = "শিক্ষক ক",
            deleteSentinel = deleteSentinel,
            updatedAtValue = updatedAtSentinel
        )

        assertNull(diff)
    }

    @Test
    fun granularUpdateOnlyEmitsChangedFieldsAndDeletesRemovedKeysWithoutOverwritingSiblings() {
        val prev = Student(
            id = "stu-2",
            cls = 4,
            roll = 2,
            name = "অর্পা দাস",
            union = "পুরাতন ইউনিয়ন",
            post = "পুরাতন পোস্ট",
            m = mapOf(
                "BAN_1_TB" to 8.0,
                "BAN_1_WR" to 40.0,
                "BAN_1_TOTAL" to 88.0,
                "MATH_1_WR" to 46.0
            ),
            x = mapOf(
                "REM_1" to "ভালো",
                "DAYS_1" to "55"
            )
        )
        val next = prev.copy(
            union = "নতুন ইউনিয়ন",
            m = mapOf(
                "BAN_1_TB" to 9.5,
                "BAN_1_WR" to 40.0,
                "MATH_1_WR" to 46.0
            ),
            x = mapOf(
                "REM_1" to "খুব ভালো"
            )
        )

        val diff = StudentSyncDiffHelper.buildUpdateDiff(
            prev = prev,
            next = next,
            uid = "teacher-b",
            userName = "শিক্ষক খ",
            deleteSentinel = deleteSentinel,
            updatedAtValue = updatedAtSentinel
        )

        assertNotNull(diff)
        val map = diff!!

        // Modified mark emitted as granular dotted field path; untouched sibling keys excluded
        assertEquals(9.5, map["m.BAN_1_TB"] as Double, 0.001)
        assertFalse(map.containsKey("m.BAN_1_WR"))
        assertFalse(map.containsKey("m.MATH_1_WR"))
        assertEquals(deleteSentinel, map["m.BAN_1_TOTAL"])

        // Modified x keys and union emitted as granular dotted field paths
        assertEquals("খুব ভালো", map["x.REM_1"])
        assertEquals("নতুন ইউনিয়ন", map["x.UNION"])
        assertEquals(deleteSentinel, map["x.DAYS_1"])

        // Post unchanged so excluded from diff
        assertFalse(map.containsKey("post"))
        assertFalse(map.containsKey("m"))
        assertFalse(map.containsKey("x"))
    }

    @Test
    fun cloudMapNormalizationDropsInvalidDotKeysNonFiniteValuesAndRespectsRuleCaps() {
        val dirtyMarks = buildMap<String, Double> {
            put("bad.key", 10.0)
            put("", 12.0)
            put("inf_key", Double.POSITIVE_INFINITY)
            put("nan_key", Double.NaN)
            for (i in 1..210) {
                put("SUB_${i}_WR", i.toDouble())
            }
        }
        val normalizedM = StudentSyncDiffHelper.normalizeMarksForCloud(dirtyMarks)
        assertEquals(200, normalizedM.size)
        assertFalse(normalizedM.containsKey("bad.key"))
        assertFalse(normalizedM.containsKey(""))
        assertFalse(normalizedM.containsKey("inf_key"))
        assertFalse(normalizedM.containsKey("nan_key"))

        val dirtyX = buildMap<String, String> {
            put("bad.x", "abc")
            put("UNION", "ignored-direct-union")
            for (i in 1..130) {
                put("K_$i", "val_$i")
            }
        }
        val normalizedX = StudentSyncDiffHelper.normalizeXForCloud(dirtyX, union = "প্রকৃত ইউনিয়ন")
        assertEquals(120, normalizedX.size)
        assertEquals("প্রকৃত ইউনিয়ন", normalizedX["UNION"])
        assertFalse(normalizedX.containsKey("bad.x"))
    }
}
