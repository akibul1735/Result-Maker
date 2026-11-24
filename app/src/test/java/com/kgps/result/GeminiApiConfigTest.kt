package com.kgps.result

import com.kgps.result.ai.GeminiApiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiApiConfigTest {

    @Test
    fun `normalizeModelId returns empty on blank or invalid without preset fallback`() {
        assertEquals("", GeminiApiConfig.normalizeModelId(""))
        assertEquals("", GeminiApiConfig.normalizeModelId("   "))
        assertEquals("", GeminiApiConfig.normalizeModelId("gemini-3.8-flash"))
        assertEquals("", GeminiApiConfig.normalizeModelId("models/gemini-4-argon"))
        assertEquals("", GeminiApiConfig.normalizeModelId("gemini-2.5-flash-tts"))
        assertEquals("gemini-2.0-flash", GeminiApiConfig.normalizeModelId("models/gemini-2.0-flash"))
        // If API explicitly returns a model in cachedIds, allow it
        assertEquals(
            "gemini-3.8-flash",
            GeminiApiConfig.normalizeModelId("gemini-3.8-flash", cachedIds = setOf("gemini-3.8-flash"))
        )
    }

    @Test
    fun `normalizeOrderedModels deduplicates excludes invalid and caps at 3`() {
        val normalized = GeminiApiConfig.normalizeOrderedModels(
            rawModels = listOf(
                "models/gemini-2.5-flash",
                "",
                "gemini-2.0-flash",
                "gemini-3.8-flash", // legacy bogus -> excluded
                "gemini-2.0-flash", // duplicate
                "gemini-2.5-flash-lite",
                "gemini-2.5-pro" // 4th -> capped at 3
            )
        )
        assertEquals(
            listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-2.5-flash-lite"),
            normalized
        )
    }

    @Test
    fun `toggleOrderedModel adds removes and enforces max 3 limit`() {
        val (one, ok1) = GeminiApiConfig.toggleOrderedModel(emptyList(), "gemini-2.5-flash")
        assertTrue(ok1)
        assertEquals(listOf("gemini-2.5-flash"), one)

        val (two, ok2) = GeminiApiConfig.toggleOrderedModel(one, "gemini-2.5-flash-lite")
        assertTrue(ok2)
        val (three, ok3) = GeminiApiConfig.toggleOrderedModel(two, "gemini-2.5-pro")
        assertTrue(ok3)
        assertEquals(listOf("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.5-pro"), three)

        // Adding a 4th fails cleanly without changing selection
        val (blocked, ok4) = GeminiApiConfig.toggleOrderedModel(three, "gemini-2.0-flash")
        assertFalse(ok4)
        assertEquals(three, blocked)

        // Toggling an existing one removes it cleanly
        val (removed, ok5) = GeminiApiConfig.toggleOrderedModel(three, "gemini-2.5-flash-lite")
        assertTrue(ok5)
        assertEquals(listOf("gemini-2.5-flash", "gemini-2.5-pro"), removed)
    }

    @Test
    fun `assignOrderedModelSlot swaps existing slot and clears cleanly`() {
        val start = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-2.5-pro")

        // Picking slot 3's model for slot 1 swaps positions 0 and 2
        val swapped = GeminiApiConfig.assignOrderedModelSlot(
            current = start,
            slotIndex = 0,
            rawModelId = "gemini-2.5-pro"
        )
        assertEquals(listOf("gemini-2.5-pro", "gemini-2.0-flash", "gemini-2.5-flash"), swapped)

        // Clearing slot 2 removes index 1 and keeps remaining order
        val cleared = GeminiApiConfig.assignOrderedModelSlot(
            current = swapped,
            slotIndex = 1,
            rawModelId = null
        )
        assertEquals(listOf("gemini-2.5-pro", "gemini-2.5-flash"), cleared)

        // Assigning to empty 3rd slot appends cleanly
        val appended = GeminiApiConfig.assignOrderedModelSlot(
            current = cleared,
            slotIndex = 2,
            rawModelId = "gemini-2.0-flash-lite"
        )
        assertEquals(listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.0-flash-lite"), appended)
    }

    @Test
    fun `moveOrderedModelSlot reorders within bounds and ignores invalid moves`() {
        val list = listOf("m1", "m2", "m3")
        assertEquals(listOf("m2", "m1", "m3"), GeminiApiConfig.moveOrderedModelSlot(list, 1, -1))
        assertEquals(listOf("m1", "m3", "m2"), GeminiApiConfig.moveOrderedModelSlot(list, 1, +1))
        assertEquals(list, GeminiApiConfig.moveOrderedModelSlot(list, 0, -1))
        assertEquals(list, GeminiApiConfig.moveOrderedModelSlot(list, 2, +1))
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
