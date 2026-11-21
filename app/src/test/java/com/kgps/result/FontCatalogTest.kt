package com.kgps.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FontCatalogTest {

    @Test
    fun `pdf and app font catalogs cover eleven official bengali fonts`() {
        val expectedPdfIds = listOf(
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
        assertEquals(expectedPdfIds, PDF_FONT_LIST.map { it.id })
        assertEquals("system", APP_FONT_LIST.first().id)
        expectedPdfIds.forEach { id ->
            assertTrue("Missing $id in APP_FONT_LIST", APP_FONT_LIST.any { it.id == id })
        }
        assertTrue(PDF_FONT_LIST.all { it.labelBn.isNotBlank() && it.descriptionBn.isNotBlank() })
        assertTrue(APP_FONT_LIST.all { it.labelBn.isNotBlank() && it.descriptionBn.isNotBlank() })
    }
}
