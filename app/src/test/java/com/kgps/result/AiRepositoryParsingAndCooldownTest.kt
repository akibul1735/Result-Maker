package com.kgps.result

import com.kgps.result.ai.AutoScanResult
import com.kgps.result.ai.ClassSheetKind
import com.kgps.result.ai.DirectGeminiAiRepository
import com.kgps.result.ai.GeminiApiConfig
import com.kgps.result.ai.parseAutoScanJson
import com.kgps.result.ai.parseClassSheetJsonRobust
import com.kgps.result.ai.parseExtractedClassSheetJson
import com.kgps.result.ai.parseMarksheetJsonRobust
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRepositoryParsingAndCooldownTest {

    @Test(expected = IllegalStateException::class)
    fun `extractCandidateText throws when candidates missing due to blockReason`() {
        val json = JSONObject("""{"promptFeedback":{"blockReason":"SAFETY"}}""")
        DirectGeminiAiRepository.extractCandidateText(json)
    }

    @Test(expected = IllegalStateException::class)
    fun `extractCandidateText throws when parts contain only thought summaries`() {
        val json = JSONObject(
            """{"candidates":[{"finishReason":"STOP","content":{"parts":[{"thought":true,"text":"thinking..."}]}}]}"""
        )
        DirectGeminiAiRepository.extractCandidateText(json)
    }

    @Test(expected = IllegalStateException::class)
    fun `extractCandidateText throws on IMAGE_SAFETY finishReason`() {
        val json = JSONObject("""{"candidates":[{"finishReason":"IMAGE_SAFETY"}]}""")
        DirectGeminiAiRepository.extractCandidateText(json)
    }

    @Test
    fun `extractCandidateText skips thought part and returns JSON payload`() {
        val json = JSONObject(
            """{"candidates":[{"finishReason":"STOP","content":{"parts":[{"thought":true,"text":"plan"},{"text":"{\"rows\":[]}"}]}}]}"""
        )
        assertEquals("""{"rows":[]}""", DirectGeminiAiRepository.extractCandidateText(json))
    }

    @Test
    fun `parseClassSheetJsonRobust unwraps top-level array and alternative row keys`() {
        val topLevelArray = """
            ```json
            [
              {"roll":"01","name":"যারীন তাসনিম","marks":{}},
              {"roll":"02","name":"মোসাঃ মরিয়ম ইসলাম","marks":{}}
            ]
            ```
        """.trimIndent()
        val parsedArraySheet = parseExtractedClassSheetJson(
            json = parseClassSheetJsonRobust(topLevelArray, clsHint = 4, termHint = null, subKeyHint = "BAN"),
            clsHint = 4,
            termHint = null,
            subKeyHint = "BAN",
            sheetKindHint = ClassSheetKind.THREE_TERM_MARKS
        )
        assertEquals(2, parsedArraySheet.rows.size)
        assertEquals("যারীন তাসনিম", parsedArraySheet.rows[0].name)
        assertEquals("02", parsedArraySheet.rows[1].rollText)
        assertEquals(ClassSheetKind.THREE_TERM_MARKS, parsedArraySheet.sheetKind)

        val wrappedPayload = """{"result":{"cls":4,"subKey":"BAN","records":[{"rollNo":"03","studentName":"রাহাজুল আলম রিয়াদ","marks":{"T1_CONT":"25"}}]}}"""
        val autoRes = parseAutoScanJson(wrappedPayload, clsHint = 4, termHint = null, subKeyHint = "BAN")
        assertTrue(autoRes is AutoScanResult.ClassSheet)
        val sheet = (autoRes as AutoScanResult.ClassSheet).sheet
        assertEquals(1, sheet.rows.size)
        assertEquals("03", sheet.rows[0].rollText)
        assertEquals("রাহাজুল আলম রিয়াদ", sheet.rows[0].name)
        assertEquals("25", sheet.rows[0].marksText["T1_CONT"])
    }

    @Test
    fun `parseClassSheetJsonRobust salvages complete rows from truncated students array`() {
        val truncated = """{"cls":4,"term":1,"subKey":"BAN","students":[{"roll":"1","name":"যারীন তাসনিম","marks":{"T1_CONT":"28"}},{"roll":"2","name":"মোসাঃ মরিয়ম","marks":{"T1_CON"""
        val salvaged = parseClassSheetJsonRobust(truncated, clsHint = 4, termHint = 1, subKeyHint = "BAN")
        assertEquals(1, salvaged.getJSONArray("rows").length())
    }

    @Test
    fun `parseMarksheetJsonRobust unwraps single item array or result wrapper`() {
        val arrResp = """[{"term":2,"student":{"name":"লামিয়া","cls":4,"roll":11},"marks":[{"key":"BAN_2_WR","value":62}]}]"""
        val obj = parseMarksheetJsonRobust(arrResp)
        assertEquals(2, obj.getInt("term"))
        assertEquals(11, obj.getJSONObject("student").getInt("roll"))
    }

    @Test
    fun `buildModelQueuePlan excludes cooling models and reports shortest wait`() {
        val now = 100_000L
        val cooldowns = mapOf(
            "gemini-2.5-pro" to now + 45_000L,
            "gemini-2.5-flash" to now - 1_000L,
            "gemini-2.5-flash-lite" to now + 18_000L
        )
        val plan = DirectGeminiAiRepository.buildModelQueuePlan(
            userOrdered = listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.5-flash-lite"),
            cooldownUntilMs = cooldowns,
            nowMs = now
        )
        // Only healthy model should be scheduled; cooling models must NOT be appended
        assertEquals(listOf("gemini-2.5-flash"), plan.healthyModels)
        assertEquals(listOf("gemini-2.5-flash"), plan.expiredKeysToPrune)
        assertEquals(18, plan.shortestCooldownSec)

        // When ALL models are cooling, healthyModels must be empty (0 network calls allowed)
        val allCooling = DirectGeminiAiRepository.buildModelQueuePlan(
            userOrdered = listOf("gemini-2.5-pro", "gemini-2.5-flash-lite"),
            cooldownUntilMs = cooldowns,
            nowMs = now
        )
        assertTrue(allCooling.healthyModels.isEmpty())
        assertEquals(18, allCooling.shortestCooldownSec)
    }

    @Test
    fun `error classification helpers detect zero quota thinking config and text-only models`() {
        assertTrue(DirectGeminiAiRepository.isZeroQuotaError("Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 0"))
        assertFalse(DirectGeminiAiRepository.isZeroQuotaError("Resource has been exhausted (e.g. check quota). Please retry in 22.5s."))

        assertEquals(
            DirectGeminiAiRepository.Companion.Config400Kind.THINKING_CONFIG,
            DirectGeminiAiRepository.classify400Error("""{"error":{"message":"Unknown name \"thinkingBudget\" at generationConfig.thinkingConfig"}}""")
        )
        assertEquals(
            DirectGeminiAiRepository.Companion.Config400Kind.RESPONSE_MIME_TYPE,
            DirectGeminiAiRepository.classify400Error("""{"error":{"message":"JSON mode is not enabled for responseMimeType application/json"}}""")
        )
        assertEquals(
            DirectGeminiAiRepository.Companion.Config400Kind.OTHER,
            DirectGeminiAiRepository.classify400Error("""{"error":{"message":"Unable to process input image payload"}}""")
        )

        assertTrue(DirectGeminiAiRepository.isKnownTextOnlyModel("gemma-3-1b-it"))
        assertTrue(DirectGeminiAiRepository.isKnownTextOnlyModel("gemma-2-9b-it"))
        assertFalse(DirectGeminiAiRepository.isKnownTextOnlyModel("gemma-3-27b-it"))
        assertFalse(DirectGeminiAiRepository.isKnownTextOnlyModel("gemini-2.5-flash"))
    }

    @Test
    fun `gemini-3_5-flash-lite stays first in config normalization and execution queue`() {
        assertEquals(
            "gemini-3.5-flash-lite",
            GeminiApiConfig.normalizeModelId("gemini-3.5-flash-lite")
        )
        val ordered = GeminiApiConfig.normalizeOrderedModels(
            listOf("gemini-3.5-flash-lite", "gemini-3.1-flash-lite")
        )
        assertEquals(listOf("gemini-3.5-flash-lite", "gemini-3.1-flash-lite"), ordered)

        val plan = DirectGeminiAiRepository.buildModelQueuePlan(
            userOrdered = ordered,
            cooldownUntilMs = emptyMap(),
            nowMs = 100_000L
        )
        assertEquals(listOf("gemini-3.5-flash-lite", "gemini-3.1-flash-lite"), plan.healthyModels)

        // Cached live model IDs passed into queue planning must never be silently dropped either
        val customPlan = DirectGeminiAiRepository.buildModelQueuePlan(
            userOrdered = listOf("gemini-3.8-flash", "gemini-3.1-flash-lite"),
            cooldownUntilMs = emptyMap(),
            nowMs = 100_000L,
            cachedIds = setOf("gemini-3.8-flash", "gemini-3.1-flash-lite")
        )
        assertEquals(listOf("gemini-3.8-flash", "gemini-3.1-flash-lite"), customPlan.healthyModels)
    }
}
