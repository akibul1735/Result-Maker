package com.kgps.result

import com.kgps.result.ai.GeminiApiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiConfigTest {

    @Test
    fun `default model and legacy placeholder migration behave safely`() {
        assertEquals("gemini-2.5-flash", GeminiApiConfig.DEFAULT_MODEL)
        assertEquals("gemini-2.5-flash-lite", GeminiApiConfig.FAST_FALLBACK_MODEL)
        assertEquals("gemini-2.5-flash", GeminiApiConfig.normalizeModelId(""))
        assertEquals("gemini-2.5-flash", GeminiApiConfig.normalizeModelId("gemini-3.8-flash"))
        assertEquals("gemini-2.5-flash", GeminiApiConfig.normalizeModelId("models/gemini-4-argon"))
        assertEquals("gemini-2.0-flash", GeminiApiConfig.normalizeModelId("models/gemini-2.0-flash"))
        // If API explicitly returns a model in cachedIds, allow it
        assertEquals(
            "gemini-3.8-flash",
            GeminiApiConfig.normalizeModelId("gemini-3.8-flash", cachedIds = setOf("gemini-3.8-flash"))
        )
    }

    @Test
    fun `default fallbacks provide 3 distinct models excluding primary`() {
        val forDefault = GeminiApiConfig.defaultFallbacksFor("gemini-2.5-flash")
        assertEquals(3, forDefault.size)
        assertFalse("gemini-2.5-flash" in forDefault)
        assertEquals(listOf("gemini-2.5-flash-lite", "gemini-2.0-flash", "gemini-2.5-pro"), forDefault)

        val forLite = GeminiApiConfig.defaultFallbacksFor("gemini-2.5-flash-lite")
        assertEquals(3, forLite.size)
        assertFalse("gemini-2.5-flash-lite" in forLite)
        assertTrue("gemini-2.5-flash" in forLite)
    }

    @Test
    fun `normalizeFallbackList deduplicates removes primary and caps at 3`() {
        val normalized = GeminiApiConfig.normalizeFallbackList(
            rawFallbacks = listOf(
                "models/gemini-2.5-flash", // primary -> must be excluded
                "gemini-2.0-flash",
                "gemini-3.8-flash", // legacy bogus -> must be excluded
                "gemini-2.0-flash", // duplicate
                "gemini-2.5-flash-lite",
                "gemini-2.5-pro",
                "gemini-2.0-flash-lite" // 4th -> must be capped at 3
            ),
            primaryModel = "gemini-2.5-flash"
        )
        assertEquals(
            listOf("gemini-2.0-flash", "gemini-2.5-flash-lite", "gemini-2.5-pro"),
            normalized
        )
    }

    @Test
    fun `assignFallbackSlot swaps existing slot and clears cleanly`() {
        val start = listOf("gemini-2.5-flash-lite", "gemini-2.0-flash", "gemini-2.5-pro")

        // Picking slot 3's model for slot 1 swaps positions 0 and 2
        val swapped = GeminiApiConfig.assignFallbackSlot(
            current = start,
            slotIndex = 0,
            rawModelId = "gemini-2.5-pro",
            primaryModel = "gemini-2.5-flash"
        )
        assertEquals(listOf("gemini-2.5-pro", "gemini-2.0-flash", "gemini-2.5-flash-lite"), swapped)

        // Clearing slot 2 removes index 1 and keeps remaining order
        val cleared = GeminiApiConfig.assignFallbackSlot(
            current = swapped,
            slotIndex = 1,
            rawModelId = null,
            primaryModel = "gemini-2.5-flash"
        )
        assertEquals(listOf("gemini-2.5-pro", "gemini-2.5-flash-lite"), cleared)

        // Assigning to empty 3rd slot appends cleanly
        val appended = GeminiApiConfig.assignFallbackSlot(
            current = cleared,
            slotIndex = 2,
            rawModelId = "gemini-2.0-flash-lite",
            primaryModel = "gemini-2.5-flash"
        )
        assertEquals(listOf("gemini-2.5-pro", "gemini-2.5-flash-lite", "gemini-2.0-flash-lite"), appended)
    }

    @Test
    fun `moveFallbackSlot reorders within bounds and ignores invalid moves`() {
        val list = listOf("m1", "m2", "m3")
        assertEquals(listOf("m2", "m1", "m3"), GeminiApiConfig.moveFallbackSlot(list, 1, -1))
        assertEquals(listOf("m1", "m3", "m2"), GeminiApiConfig.moveFallbackSlot(list, 1, +1))
        assertEquals(list, GeminiApiConfig.moveFallbackSlot(list, 0, -1))
        assertEquals(list, GeminiApiConfig.moveFallbackSlot(list, 2, +1))
    }

    @Test
    fun `isExcludedModelId filters non-chat specialized models only`() {
        assertTrue(GeminiApiConfig.isExcludedModelId("gemini-2.5-flash-preview-tts"))
        assertTrue(GeminiApiConfig.isExcludedModelId("gemini-2.5-flash-image"))
        assertTrue(GeminiApiConfig.isExcludedModelId("gemini-2.5-flash-native-audio-preview"))
        assertTrue(GeminiApiConfig.isExcludedModelId("text-embedding-004"))
        assertFalse(GeminiApiConfig.isExcludedModelId("gemini-2.5-flash"))
        assertFalse(GeminiApiConfig.isExcludedModelId("gemini-2.0-flash-lite"))
        assertFalse(GeminiApiConfig.isExcludedModelId("gemma-3-27b-it"))
    }
}
