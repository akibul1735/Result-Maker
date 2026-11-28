package com.kgps.result

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object TestimonialLocalStore {
    private const val PREFS_NAME = "testimonial_maker_prefs"
    private const val CONFIG_KEY = "testimonial_config_v1"
    private const val OVERRIDES_KEY = "testimonial_overrides_v1"

    fun saveConfig(ctx: Context, cfg: TestimonialConfig) {
        val obj = JSONObject().apply {
            put("selectedClass", cfg.selectedClass)
            put("selectedClasses", JSONArray(cfg.resolvedSelectedClasses))
            put("rollOrder", cfg.rollOrder.name)
            put("rollFrom", cfg.rollFrom ?: JSONObject.NULL)
            put("rollTo", cfg.rollTo ?: JSONObject.NULL)
            put("extraBlankCount", cfg.extraBlankCount)
            put("testimonialType", cfg.testimonialType.name)
            put("customBannerTitle", cfg.customBannerTitle)
            put("bodyPreset", cfg.bodyPreset.name)
            put("customBodyTemplate", cfg.customBodyTemplate)
            put("customClosingLine", cfg.customClosingLine)
            put("issueDate", cfg.issueDate)
            put("sessionYear", cfg.sessionYear)
            put("establishedYear", cfg.establishedYear)
            put("customSchoolName", cfg.customSchoolName)
            put("customSchoolAddress", cfg.customSchoolAddress)
            put("customSignPlace", cfg.customSignPlace)
            put("showGovtHeader", cfg.showGovtHeader)
            put("govtLine1", cfg.govtLine1)
            put("govtLine2", cfg.govtLine2)
            put("govtLine3", cfg.govtLine3)
            put("showGovtSeal", cfg.showGovtSeal)
            put("showDpeLogo", cfg.showDpeLogo)
            put("showWatermark", cfg.showWatermark)
            put("showCounterfoil", cfg.showCounterfoil)
            put("borderStyle", cfg.borderStyle.name)
            put("serialMode", cfg.serialMode.name)
            put("serialStartOrPrefix", cfg.serialStartOrPrefix)
            put("padRollTwoDigits", cfg.padRollTwoDigits)
            put("numeralMode", cfg.numeralMode.name)
            put("bnFontId", cfg.bnFontId)
            put("enFontId", cfg.enFontId)
            put("marginInches", cfg.marginInches.toDouble())
            put("headTeacherTitle", cfg.headTeacherTitle)
            put("showSignatureImage", cfg.showSignatureImage)
            put("signatureScale", cfg.signatureScale.toDouble())
            put("motherFieldKeyHint", cfg.motherFieldKeyHint)
            put("dobFieldKeyHint", cfg.dobFieldKeyHint)
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(CONFIG_KEY, obj.toString())
            .apply()
    }

    fun loadConfig(ctx: Context, fallback: TestimonialConfig): TestimonialConfig {
        val raw = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(CONFIG_KEY, null)
            ?: return fallback
        return try {
            val o = JSONObject(raw)
            val clsArr = o.optJSONArray("selectedClasses")
            val parsedClasses = if (clsArr != null && clsArr.length() > 0) {
                buildList {
                    for (i in 0 until clsArr.length()) {
                        val v = clsArr.optInt(i, -1)
                        if (v > 0) add(v)
                    }
                }.distinct()
            } else {
                emptyList()
            }
            val selCls = o.optInt("selectedClass", fallback.selectedClass).coerceAtLeast(1)
            TestimonialConfig(
                selectedClass = selCls,
                selectedClasses = parsedClasses.ifEmpty { listOf(selCls) },
                rollOrder = runCatching { SeatPlanRollOrder.valueOf(o.optString("rollOrder")) }
                    .getOrDefault(fallback.rollOrder),
                rollFrom = if (o.isNull("rollFrom")) null else o.optInt("rollFrom", -1).takeIf { it >= 1 },
                rollTo = if (o.isNull("rollTo")) null else o.optInt("rollTo", -1).takeIf { it >= 1 },
                extraBlankCount = o.optInt("extraBlankCount", fallback.extraBlankCount).coerceIn(0, 100),
                testimonialType = runCatching { TestimonialType.valueOf(o.optString("testimonialType")) }
                    .getOrDefault(fallback.testimonialType),
                customBannerTitle = o.optString("customBannerTitle", fallback.customBannerTitle),
                bodyPreset = runCatching { TestimonialBodyPreset.valueOf(o.optString("bodyPreset")) }
                    .getOrDefault(fallback.bodyPreset),
                customBodyTemplate = o.optString("customBodyTemplate", fallback.customBodyTemplate),
                customClosingLine = o.optString("customClosingLine", fallback.customClosingLine),
                issueDate = o.optString("issueDate", fallback.issueDate),
                sessionYear = o.optString("sessionYear", fallback.sessionYear),
                establishedYear = o.optString("establishedYear", fallback.establishedYear),
                customSchoolName = o.optString("customSchoolName", fallback.customSchoolName),
                customSchoolAddress = o.optString("customSchoolAddress", fallback.customSchoolAddress),
                customSignPlace = o.optString("customSignPlace", fallback.customSignPlace),
                showGovtHeader = o.optBoolean("showGovtHeader", fallback.showGovtHeader),
                govtLine1 = o.optString("govtLine1", fallback.govtLine1),
                govtLine2 = o.optString("govtLine2", fallback.govtLine2),
                govtLine3 = o.optString("govtLine3", fallback.govtLine3),
                showGovtSeal = o.optBoolean("showGovtSeal", fallback.showGovtSeal),
                showDpeLogo = o.optBoolean("showDpeLogo", fallback.showDpeLogo),
                showWatermark = o.optBoolean("showWatermark", fallback.showWatermark),
                showCounterfoil = o.optBoolean("showCounterfoil", fallback.showCounterfoil),
                borderStyle = runCatching { TestimonialBorderStyle.valueOf(o.optString("borderStyle")) }
                    .getOrDefault(fallback.borderStyle),
                serialMode = runCatching { TestimonialSerialMode.valueOf(o.optString("serialMode")) }
                    .getOrDefault(fallback.serialMode),
                serialStartOrPrefix = o.optString("serialStartOrPrefix", fallback.serialStartOrPrefix),
                padRollTwoDigits = o.optBoolean("padRollTwoDigits", fallback.padRollTwoDigits),
                numeralMode = runCatching { SeatPlanNumeralMode.valueOf(o.optString("numeralMode")) }
                    .getOrDefault(fallback.numeralMode),
                bnFontId = o.optString("bnFontId", fallback.bnFontId).ifBlank { fallback.bnFontId },
                enFontId = o.optString("enFontId", fallback.enFontId).ifBlank { fallback.enFontId },
                marginInches = o.optDouble("marginInches", fallback.marginInches.toDouble()).toFloat().coerceIn(0.14f, 0.75f),
                headTeacherTitle = o.optString("headTeacherTitle", fallback.headTeacherTitle),
                showSignatureImage = o.optBoolean("showSignatureImage", fallback.showSignatureImage),
                signatureScale = o.optDouble("signatureScale", fallback.signatureScale.toDouble()).toFloat().coerceIn(0.35f, 2.5f),
                motherFieldKeyHint = o.optString("motherFieldKeyHint", fallback.motherFieldKeyHint),
                dobFieldKeyHint = o.optString("dobFieldKeyHint", fallback.dobFieldKeyHint)
            )
        } catch (_: Exception) {
            fallback
        }
    }

    fun saveOverrides(ctx: Context, overrides: Map<String, TestimonialStudentOverride>) {
        val root = JSONObject()
        overrides.forEach { (id, ov) ->
            if (ov.fatherName.isNotBlank() || ov.motherName.isNotBlank() || ov.dob.isNotBlank()) {
                root.put(
                    id,
                    JSONObject().apply {
                        put("fatherName", ov.fatherName)
                        put("motherName", ov.motherName)
                        put("dob", ov.dob)
                    }
                )
            }
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(OVERRIDES_KEY, root.toString())
            .apply()
    }

    fun loadOverrides(ctx: Context): Map<String, TestimonialStudentOverride> {
        val raw = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(OVERRIDES_KEY, null)
            ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            buildMap {
                val keys = root.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    val obj = root.optJSONObject(id) ?: continue
                    put(
                        id,
                        TestimonialStudentOverride(
                            fatherName = obj.optString("fatherName", ""),
                            motherName = obj.optString("motherName", ""),
                            dob = obj.optString("dob", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun clearConfig(ctx: Context) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(CONFIG_KEY)
            .apply()
    }
}
