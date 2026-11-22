package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FontCatalogTest {

    @Test
    fun `pdf and app font catalogs separate bengali and english fonts cleanly`() {
        val expectedBengaliIds = listOf(
            "noto_serif",
            "nikosh",
            "sutonny_mj",
            "kalpurush",
            "solaiman_lipi",
            "tiro_bangla",
            "noto_sans",
            "hind_siliguri",
            "anek_bangla",
            "mina",
            "baloo_da_2"
        )
        val expectedEnglishIds = listOf(
            "times_new_roman",
            "tinos",
            "arimo",
            "roboto",
            "merriweather",
            "plus_jakarta_sans"
        )
        assertEquals(expectedBengaliIds, BENGALI_PDF_FONT_LIST.map { it.id })
        assertEquals(expectedEnglishIds, ENGLISH_PDF_FONT_LIST.map { it.id })
        assertEquals(expectedBengaliIds + expectedEnglishIds, PDF_FONT_LIST.map { it.id })

        assertEquals("system", BENGALI_APP_FONT_LIST.first().id)
        assertEquals("system", ENGLISH_APP_FONT_LIST.first().id)
        assertEquals("system", APP_FONT_LIST.first().id)
        listOf("roboto", "times_new_roman", "arimo", "plus_jakarta_sans", "tinos", "merriweather").forEach { id ->
            assertTrue("Missing $id in ENGLISH_APP_FONT_LIST", ENGLISH_APP_FONT_LIST.any { it.id == id })
            assertTrue("Missing $id in APP_FONT_LIST", APP_FONT_LIST.any { it.id == id })
        }
        expectedBengaliIds.forEach { id ->
            assertTrue("Missing $id in BENGALI_APP_FONT_LIST", BENGALI_APP_FONT_LIST.any { it.id == id })
            assertTrue("Missing $id in APP_FONT_LIST", APP_FONT_LIST.any { it.id == id })
        }
        assertTrue(PDF_FONT_LIST.all { it.labelBn.isNotBlank() && it.descriptionBn.isNotBlank() })
        assertTrue(APP_FONT_LIST.all { it.labelBn.isNotBlank() && it.descriptionBn.isNotBlank() })
    }

    @Test
    fun `encode and decode pdf font pairs stay within firestore length limits and preserve legacy ids`() {
        BENGALI_PDF_FONT_LIST.forEach { bn ->
            ENGLISH_PDF_FONT_LIST.forEach { en ->
                val encoded = encodePdfFontPair(bn.id, en.id)
                assertTrue("Encoded font pair exceeds 40 chars: $encoded", encoded.length <= 40)
                val decoded = decodePdfFontPair(encoded)
                assertEquals(bn.id, decoded.bengaliFontId)
                assertEquals(en.id, decoded.englishFontId)
            }
        }

        val legacyBn = decodePdfFontPair("nikosh")
        assertEquals("nikosh", legacyBn.bengaliFontId)
        assertEquals("times_new_roman", legacyBn.englishFontId)

        val legacyEn = decodePdfFontPair("tinos")
        assertEquals("noto_serif", legacyEn.bengaliFontId)
        assertEquals("tinos", legacyEn.englishFontId)

        val explicitOverride = decodePdfFontPair("nikosh|times_new_roman", explicitEnglishFont = "arimo")
        assertEquals("nikosh", explicitOverride.bengaliFontId)
        assertEquals("arimo", explicitOverride.englishFontId)
    }

    @Test
    fun `splitMixedScriptRuns separates bengali and english segments accurately`() {
        assertEquals(
            listOf(MixedScriptRun("প্রাথমিক বিদ্যালয়", true)),
            splitMixedScriptRuns("প্রাথমিক বিদ্যালয়")
        )
        assertEquals(
            listOf(MixedScriptRun("REF:C3-BAN-T1", false)),
            splitMixedScriptRuns("REF:C3-BAN-T1")
        )

        val mixed = splitMixedScriptRuns("রোল 01 — মিতু (MATH)")
        assertEquals(
            listOf(
                MixedScriptRun("রোল ", true),
                MixedScriptRun("01 — ", false),
                MixedScriptRun("মিতু ", true),
                MixedScriptRun("(MATH)", false)
            ),
            mixed
        )

        val bnBracket = splitMixedScriptRuns("শ্রেণি: Class 3 (তৃতীয়)")
        assertEquals(
            listOf(
                MixedScriptRun("শ্রেণি: ", true),
                MixedScriptRun("Class 3 ", false),
                MixedScriptRun("(তৃতীয়)", true)
            ),
            bnBracket
        )

        val conjunct = splitMixedScriptRuns("র\u200D্যাজ + 100")
        assertTrue(conjunct.first().isBengali)
        assertTrue(conjunct.first().text.contains("\u200D"))
        assertEquals(false, conjunct.last().isBengali)
    }
}
