package com.kgps.result

import com.kgps.result.util.CommentCategory
import com.kgps.result.util.CommentGenerator
import com.kgps.result.util.CommentGradeBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentGeneratorTest {

    private val genderedTokens = listOf("সে ", "তার ", "ছেলে", "মেয়ে", "ছাত্র ", "ছাত্রী ")

    private fun sampleClass4Student(roll: Int = 1): Student = Student(
        id = "test-$roll",
        cls = 4,
        roll = roll,
        name = "আরিফ",
        m = mapOf(
            "BAN_1_TB" to 9.0, "BAN_1_ACT" to 5.0, "BAN_1_LANG" to 5.0, "BAN_1_CT" to 9.0, "BAN_1_WR" to 60.0, // 88
            "ENG_1_TB" to 4.0, "ENG_1_ACT" to 2.0, "ENG_1_LANG" to 2.0, "ENG_1_CT" to 4.0, "ENG_1_WR" to 28.0, // 40 (weak)
            "MATH_1_TB" to 9.0, "MATH_1_ACT" to 5.0, "MATH_1_LANG" to 4.0, "MATH_1_CT" to 9.0, "MATH_1_WR" to 58.0, // 85
            "SCI_1_TB" to 7.0, "SCI_1_ACT" to 4.0, "SCI_1_LANG" to 4.0, "SCI_1_CT" to 7.0, "SCI_1_WR" to 45.0, // 67
            "BGW_1_TB" to 6.0, "BGW_1_ACT" to 3.0, "BGW_1_LANG" to 3.0, "BGW_1_CT" to 6.0, "BGW_1_WR" to 30.0, // 48 (weak)
            "ART_1_TOTAL" to 44.0 // 88%
        ),
        x = mapOf(
            "DAYS_1" to "60",
            "PRES_1" to "51", // 85%
            "ACT_1" to "ক",
            "DISC_1" to "ক",
            "COND_1" to "খ"
        )
    )

    @Test
    fun `empty term returns null comment`() {
        val empty = createEmptyStudent(cls = 3, roll = 4)
        assertNull(CommentGenerator.generateComment(empty, term = 1))
    }

    @Test
    fun `generated comments stay at most 30 words and gender-neutral even under heavy data`() {
        val base = sampleClass4Student(roll = 1).copy(
            x = mapOf(
                "DAYS_1" to "60",
                "PRES_1" to "35", // <75% poor attendance
                "ACT_1" to "গ",
                "DISC_1" to "গ",
                "COND_1" to "খ"
            )
        )
        for (roll in 1..6) {
            val st = base.copy(id = "r-$roll", roll = roll)
            val comment = CommentGenerator.generateComment(st, term = 1)
            assertNotNull(comment)
            val wc = CommentGenerator.countWords(comment!!)
            assertTrue("Comment exceeded 30 words ($wc): $comment", wc <= CommentGenerator.MAX_WORDS)
            assertTrue("Comment must contain poor attendance note: $comment", comment.contains("উপস্থিতি"))
            assertTrue("Comment must mention weak trait: $comment", comment.contains("সক্রিয়তা") || comment.contains("শৃঙ্খলা"))
            genderedTokens.forEach { tok ->
                assertFalse("Comment must be gender-neutral, found '$tok' in: $comment", comment.contains(tok))
            }
        }
    }

    @Test
    fun `roll seed produces varied wording for similar profiles`() {
        val outputs = (1..5).mapNotNull { r ->
            CommentGenerator.generateComment(sampleClass4Student(roll = r), term = 1)
        }.toSet()
        assertTrue("Expected at least 2 distinct phrasings across rolls 1..5", outputs.size >= 2)
    }

    @Test
    fun `art uses only total and term 2 stays independent of term 1`() {
        val st = Student(
            id = "s-art",
            cls = 2,
            roll = 3,
            name = "মীম",
            m = mapOf(
                "ART_1_TOTAL" to 45.0, // 90% in term 1
                "BAN_2_TB" to 8.0, "BAN_2_ACT" to 2.0, "BAN_2_LANG" to 2.0, "BAN_2_CT" to 8.0, "BAN_2_WR" to 15.0 // 35% in term 2
            )
        )
        val a1 = CommentGenerator.analyze(st, term = 1)
        val a2 = CommentGenerator.analyze(st, term = 2)
        assertEquals(CommentGradeBand.OTI_UTTOM, a1.gradeBand)
        assertEquals(CommentGradeBand.SOHAYOTA_PROYOJON, a2.gradeBand)
        assertEquals(1, a1.enteredCount)
        assertEquals(1, a2.enteredCount)
    }

    @Test
    fun `detailed alternatives provide rich subject and component insights within 30 words`() {
        val st = sampleClass4Student(roll = 2)
        val alts = CommentGenerator.generateDetailedAlternatives(st, term = 1)
        assertTrue("Should generate rich alternatives (>= 20), got ${alts.size}", alts.size >= 20)
        assertTrue(
            "Should include subject-by-subject analysis",
            alts.any { it.category == CommentCategory.SUBJECT_ANALYSIS }
        )
        alts.forEach { opt ->
            assertTrue("Option '${opt.id}' exceeds 30 words (${opt.wordCount}): ${opt.text}", opt.wordCount <= 30)
            genderedTokens.forEach { tok ->
                assertFalse("Option '${opt.id}' has gendered word '$tok'", opt.text.contains(tok))
            }
        }
    }

    @Test
    fun `teacher remark engine integrates comment generator and exposes abundant alternatives`() {
        val st = sampleClass4Student(roll = 1)
        val prefill = TeacherRemarkEngine.autoPrefillIfApplicable(st, term = 1)
        assertNotNull(prefill)
        assertTrue(CommentGenerator.countWords(prefill!!) <= 30)

        val allAlts = TeacherRemarkEngine.getRankedAlternatives(st, term = 1)
        assertTrue("Expected >= 40 total ranked alternatives, got ${allAlts.size}", allAlts.size >= 40)
        val subjectAlts = TeacherRemarkEngine.getRankedAlternatives(st, term = 1, RemarkCategory.SUBJECT_DETAIL)
        assertTrue("Expected subject detail alternatives", subjectAlts.isNotEmpty())
    }

    @Test
    fun `official circular marks distribution matches table 2_2 and 2_3 for classes 1-2 and 3-5`() {
        for (cls in 1..2) {
            val subs = subjectsFor(cls, "ইসলাম")
            assertEquals("Class $cls total full marks must be 500", 500, subs.sumOf { it.max })
            assertEquals(listOf("BAN", "ENG", "MATH", "SOC", "REL", "PE", "ART"), subs.map { it.key })

            subs.filter { it.key in setOf("BAN", "ENG", "MATH") }.forEach { s ->
                assertEquals(100, s.max)
                assertEquals(50, s.written)
                assertEquals(50, s.comps.sumOf { it.max })
                assertEquals(mapOf("TB" to 20, "ACT" to 5, "LANG" to 5, "CT" to 20), s.comps.associate { it.key to it.max })
            }
            subs.filter { it.key in setOf("SOC", "REL", "ART", "PE") }.forEach { s ->
                assertEquals(50, s.max)
                assertEquals(0, s.written)
                assertEquals(50, s.comps.sumOf { it.max })
                assertEquals(mapOf("TB" to 20, "ACT" to 5, "LANG" to 5, "CT" to 20), s.comps.associate { it.key to it.max })
            }
        }

        for (cls in 3..5) {
            val subs = subjectsFor(cls, "ইসলাম")
            assertEquals("Class $cls total full marks must be 700", 700, subs.sumOf { it.max })
            assertEquals(listOf("BAN", "ENG", "MATH", "BGW", "SCI", "REL", "PE", "ART"), subs.map { it.key })

            subs.filter { it.key in setOf("BAN", "ENG", "MATH", "BGW", "SCI", "REL") }.forEach { s ->
                assertEquals(100, s.max)
                assertEquals(70, s.written)
                assertEquals(30, s.comps.sumOf { it.max })
                assertEquals(mapOf("TB" to 10, "ACT" to 5, "LANG" to 5, "CT" to 10), s.comps.associate { it.key to it.max })
            }
            subs.filter { it.key in setOf("ART", "PE") }.forEach { s ->
                assertEquals(50, s.max)
                assertEquals(0, s.written)
                assertEquals(50, s.comps.sumOf { it.max })
                assertEquals(mapOf("TB" to 20, "ACT" to 5, "LANG" to 5, "CT" to 20), s.comps.associate { it.key to it.max })
            }
        }
    }

    @Test
    fun `art supports 4 continuous components alongside legacy total fallback`() {
        val stNew = Student(
            id = "art-4comp",
            cls = 3,
            roll = 4,
            name = "তাসনিম",
            m = mapOf(
                "ART_1_TB" to 18.0, "ART_1_ACT" to 4.0, "ART_1_LANG" to 4.0, "ART_1_CT" to 19.0
            )
        )
        val artSub = stNew.subs().first { it.key == "ART" }
        assertEquals(45.0, stNew.cont(artSub, 1), 0.001)
        assertEquals(0.0, stNew.wr(artSub, 1), 0.001)
        assertEquals(45.0, stNew.total(artSub, 1), 0.001)

        val analysis = CommentGenerator.analyze(stNew, term = 1)
        assertEquals(90.0, analysis.averagePercent ?: 0.0, 0.001)
        assertEquals(CommentGradeBand.OTI_UTTOM, analysis.gradeBand)
        assertEquals(90.0, analysis.subjectEvals.first().contPercent ?: 0.0, 0.001)
    }

    @Test
    fun `primary core subjects BAN ENG MATH take priority over co-curricular and other subjects`() {
        val st = Student(
            id = "core-prio",
            cls = 4,
            roll = 3,
            name = "নাবিলা",
            m = mapOf(
                // Core strong: BAN 85%, ENG 82%
                "BAN_1_TB" to 9.0, "BAN_1_ACT" to 5.0, "BAN_1_LANG" to 4.0, "BAN_1_CT" to 9.0, "BAN_1_WR" to 58.0,
                "ENG_1_TB" to 9.0, "ENG_1_ACT" to 4.0, "ENG_1_LANG" to 4.0, "ENG_1_CT" to 9.0, "ENG_1_WR" to 56.0,
                // Core weak: MATH 40% (even though ART/PE or SCI might have higher/lower pcts)
                "MATH_1_TB" to 4.0, "MATH_1_ACT" to 2.0, "MATH_1_LANG" to 2.0, "MATH_1_CT" to 4.0, "MATH_1_WR" to 28.0,
                // SCI weak with lower % (35%) and ART/PE very high (96%)
                "SCI_1_TB" to 3.0, "SCI_1_ACT" to 2.0, "SCI_1_LANG" to 2.0, "SCI_1_CT" to 3.0, "SCI_1_WR" to 25.0,
                "ART_1_TB" to 20.0, "ART_1_ACT" to 5.0, "ART_1_LANG" to 4.0, "ART_1_CT" to 19.0,
                "PE_1_TB" to 20.0, "PE_1_ACT" to 5.0, "PE_1_LANG" to 5.0, "PE_1_CT" to 19.0
            ),
            x = mapOf("DAYS_1" to "60", "PRES_1" to "54", "ACT_1" to "ক", "DISC_1" to "খ", "COND_1" to "ক")
        )

        val analysis = CommentGenerator.analyze(st, term = 1)
        assertEquals(listOf("BAN", "ENG"), analysis.strongSubjects.take(2).map { it.sub.key })
        assertEquals("MATH", analysis.weakSubjects.first().sub.key)

        val engineProfile = TeacherRemarkEngine.analyzeProfile(st, term = 1)
        assertEquals(listOf("বাংলা", "ইংরেজি"), engineProfile.strongSubjects.take(2))
        assertEquals("গণিত", engineProfile.weakSubjects.first())

        val comment = CommentGenerator.generateComment(st, term = 1)
        assertNotNull(comment)
        assertTrue(CommentGenerator.countWords(comment!!) <= CommentGenerator.MAX_WORDS)
        assertTrue("Comment should prioritize MATH weakness: $comment", comment.contains("গণিত"))
        genderedTokens.forEach { tok ->
            assertFalse("Comment must be gender-neutral, found '$tok'", comment.contains(tok))
        }

        val alts = CommentGenerator.generateDetailedAlternatives(st, term = 1)
        assertTrue(
            "Should expose dedicated core-3 diagnostic option",
            alts.any { it.id.startsWith("gen_core3_") || it.id == "gen_core_vs_noncore" }
        )
    }
}
