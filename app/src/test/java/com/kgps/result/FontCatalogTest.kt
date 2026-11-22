package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FontCatalogTest {

    @Test
    fun `pdf and app font catalogs cover eleven official bengali fonts and english fonts`() {
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
        val expectedPdfIds = expectedBengaliIds + listOf("times_new_roman", "tinos", "arimo", "merriweather")
        assertEquals(expectedPdfIds, PDF_FONT_LIST.map { it.id })
        assertEquals("system", APP_FONT_LIST.first().id)
        listOf("roboto", "times_new_roman", "arimo", "plus_jakarta_sans").forEach { id ->
            assertTrue("Missing $id in APP_FONT_LIST", APP_FONT_LIST.any { it.id == id })
        }
        expectedBengaliIds.forEach { id ->
            assertTrue("Missing $id in APP_FONT_LIST", APP_FONT_LIST.any { it.id == id })
        }
        assertTrue(PDF_FONT_LIST.all { it.labelBn.isNotBlank() && it.descriptionBn.isNotBlank() })
        assertTrue(APP_FONT_LIST.all { it.labelBn.isNotBlank() && it.descriptionBn.isNotBlank() })
    }
}
