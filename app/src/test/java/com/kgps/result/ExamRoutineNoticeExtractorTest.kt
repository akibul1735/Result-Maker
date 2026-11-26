package com.kgps.result

import com.kgps.result.ai.ExamRoutineNoticeExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamRoutineNoticeExtractorTest {

    @Test
    fun parseNoticeJson_handlesGroupedMatrixOfficialNoticeFormat() {
        val settings = Settings()
        val allowed = listOf(1, 2, 3, 4, 5)
        val rawJson = """
            ```json
            {
              "examTitle": "প্রাথমিক বিদ্যালয়ের তৃতীয় প্রান্তিক মূল্যায়ন-২০২৬ এর সময়সূচি",
              "term": 3,
              "year": "২০২৬",
              "layoutType": "MATRIX_GROUPED_CLASSES",
              "footnote": "*** বিঃদ্রঃ ১৯/১১/২০২৬ হতে ৩০/১১/২০২৬ খ্রিঃ তারিখের মধ্যে সুবিধাজনক সময়ে প্রধান শিক্ষক মৌখিক ও ধারাবাহিক মূল্যায়ন কার্যক্রম সম্পন্ন করার ব্যবস্থা করবেন।",
              "warnings": [],
              "classRoutines": [
                {
                  "classes": [1, 2],
                  "classGroupLabel": "১ম ও ২য় শ্রেণি",
                  "examTime": "সকাল ১০:০০ থেকে ১২:০০ টা",
                  "rows": [
                    {"date": "০১/১২/২০২৬", "day": "মঙ্গলবার", "subjects": ["ইংরেজি"]},
                    {"date": "০২/১২/২০২৬", "day": "বুধবার", "subjects": ["বাংলা"]},
                    {"date": "০৩/১২/২০২৬", "day": "বৃহস্পতিবার", "subjects": ["প্রাথমিক গণিত"]},
                    {"date": "০৬/১২/২০২৬", "day": "রবিবার", "subjects": ["-"]},
                    {"date": "০৭/১২/২০২৬", "day": "সোমবার", "subjects": [""]}
                  ]
                },
                {
                  "classes": [3],
                  "classGroupLabel": "৩য় শ্রেণি",
                  "examTime": "বেলা ১০:০০টা হতে ১২:৩০টা",
                  "rows": [
                    {"date": "01/12/2026", "day": "", "subjects": ["ইংরেজি"]},
                    {"date": "02/12/2026", "day": "", "subjects": ["বাংলা"]},
                    {"date": "03/12/2026", "day": "", "subjects": ["বাংলাদেশ ও বিশ্বপরিচয়"]},
                    {"date": "06/12/2026", "day": "", "subjects": ["প্রাথমিক গণিত"]},
                    {"date": "07/12/2026", "day": "", "subjects": ["ধর্ম শিক্ষা"]},
                    {"date": "08/12/2026", "day": "", "subjects": ["প্রাথমিক বিজ্ঞান"]}
                  ]
                },
                {
                  "classes": [4, 5],
                  "classGroupLabel": "৪র্থ ও ৫ম শ্রেণি",
                  "examTime": "বেলা ০১:০০টা হতে ০৩:৩০টা",
                  "rows": [
                    {"date": "০১/১২/২০২৬", "day": "মঙ্গলবার", "subjects": ["ইংরেজি"]},
                    {"date": "০২/১২/২০২৬", "day": "বুধবার", "subjects": ["বাংলা"]},
                    {"date": "০৩/১২/২০২৬", "day": "বৃহস্পতিবার", "subjects": ["বাংলাদেশ ও বিশ্বপরিচয়"]},
                    {"date": "০৬/১২/২০২৬", "day": "রবিবার", "subjects": ["প্রাথমিক গণিত"]},
                    {"date": "০৭/১২/২০২৬", "day": "সোমবার", "subjects": ["ধর্ম শিক্ষা"]},
                    {"date": "০৮/১২/২০২৬", "day": "মঙ্গলবার", "subjects": ["প্রাথমিক বিজ্ঞান"]}
                  ]
                }
              ]
            }
            ```
        """.trimIndent()

        val parsed = ExamRoutineNoticeExtractor.parseNoticeJson(rawJson, allowed, settings, "gemini-2.5-flash")
        assertEquals(3, parsed.detectedTerm)
        assertEquals("২০২৬", parsed.detectedYear)
        assertEquals("MATRIX_GROUPED_CLASSES", parsed.layoutType)
        assertTrue(parsed.footnote.startsWith("১৯/১১/২০২৬"))
        assertEquals(3, parsed.groups.size)

        val g12 = parsed.groups[0]
        assertEquals(listOf(1, 2), g12.classes)
        assertEquals("সকাল ১০:০০-১২:০০ টা", g12.examTime)
        assertEquals(3, g12.rows.size) // blank & '-' rows filtered out

        val g3 = parsed.groups[1]
        assertEquals(listOf(3), g3.classes)
        assertEquals("বেলা ১০:০০-১২:৩০ টা", g3.examTime)
        assertEquals(6, g3.rows.size)
        assertEquals("০১/১২/২০২৬", g3.rows[0].date)
        assertEquals("মঙ্গলবার", g3.rows[0].day) // auto inferred weekday

        assertEquals(
            "ধর্ম ও নৈতিক শিক্ষা",
            ExamRoutineNoticeExtractor.canonicalizeSubjectForClass("ধর্ম শিক্ষা", 5, settings)
        )
    }

    @Test
    fun parseNoticeJson_handlesAlternativeLayouts_rangeLabels_andSameDateMerges() {
        val settings = Settings()
        val allowed = listOf(1, 2, 3, 4, 5)
        val rawJson = """
            {
              "examTitle": "বার্ষিক পরীক্ষা-২০২৬",
              "term": 0,
              "layoutType": "TRANSPOSED_TABLE",
              "classRoutines": [
                {
                  "classes": [],
                  "classGroupLabel": "৩য়-৫ম শ্রেণি",
                  "examTime": "০১:০০ - ০৩:৩০",
                  "rows": [
                    {"date": "1-12-2026", "day": "মঙ্গল", "subjects": ["বাংলা / ধর্ম শিক্ষা"]},
                    {"date": "০১/১২/২০২৬", "day": "মঙ্গলবার", "subjects": ["ইংরেজি"]},
                    {"date": "02.12.2026", "day": "", "subjects": ["গণিত"]},
                    {"date": "03/12/2026", "day": "", "subjects": ["ছুটি"]}
                  ]
                }
              ]
            }
        """.trimIndent()

        val parsed = ExamRoutineNoticeExtractor.parseNoticeJson(rawJson, allowed, settings)
        assertEquals(3, parsed.detectedTerm) // inferred from বার্ষিক
        assertEquals(1, parsed.groups.size)
        val grp = parsed.groups.first()
        assertEquals(listOf(3, 4, 5), grp.classes)
        assertEquals("০১:০০-০৩:৩০ টা", grp.examTime)
        assertEquals(2, grp.rows.size) // holiday skipped, duplicate 01/12 merged
        assertEquals(listOf("বাংলা", "ধর্ম শিক্ষা", "ইংরেজি"), grp.rows[0].nonEmptySubjects)
        assertEquals("০২/১২/২০২৬", grp.rows[1].date)
        assertEquals("বুধবার", grp.rows[1].day)
    }
}
