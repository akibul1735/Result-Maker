package com.kgps.result

import android.content.Context
import org.json.JSONObject

object SeatPlanLocalStore {
    private const val PREFS_NAME = "seat_plan_prefs"
    private const val CONFIG_KEY = "seat_plan_config_v1"

    fun saveConfig(ctx: Context, cfg: SeatPlanConfig) {
        val obj = JSONObject().apply {
            put("pairMode", cfg.pairMode.name)
            put("rollOrder", cfg.rollOrder.name)
            put("term", cfg.term)
            put("customExamTitle", cfg.customExamTitle)
            put("subtitle", cfg.subtitle)
            put("roomLabel", cfg.roomLabel)
            put("customSchoolName", cfg.customSchoolName)
            put("customSchoolAddress", cfg.customSchoolAddress)
            put("showSchoolAddress", cfg.showSchoolAddress)
            put("columns", cfg.columns)
            put("rows", cfg.rows)
            put("orientation", cfg.orientation.name)
            put("marginInches", cfg.marginInches.toDouble())
            put("horizontalGapPt", cfg.horizontalGapPt.toDouble())
            put("verticalGapPt", cfg.verticalGapPt.toDouble())
            put("bnFontId", cfg.bnFontId)
            put("enFontId", cfg.enFontId)
            put("schoolFontPt", cfg.schoolFontPt.toDouble())
            put("headerMetaFontPt", cfg.headerMetaFontPt.toDouble())
            put("studentNameFontPt", cfg.studentNameFontPt.toDouble())
            put("classRollFontPt", cfg.classRollFontPt.toDouble())
            put("boldSchoolName", cfg.boldSchoolName)
            put("boldAddress", cfg.boldAddress)
            put("boldExamTitle", cfg.boldExamTitle)
            put("boldStudentName", cfg.boldStudentName)
            put("boldClassRoll", cfg.boldClassRoll)
            put("borderStyle", cfg.borderStyle.name)
            put("borderWeight", cfg.borderWeight.name)
            put("cornerRadiusPt", cfg.cornerRadiusPt.toDouble())
            put("showCutLines", cfg.showCutLines)
            put("padRollTwoDigits", cfg.padRollTwoDigits)
            put("numeralMode", cfg.numeralMode.name)
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(CONFIG_KEY, obj.toString())
            .apply()
    }

    fun loadConfig(ctx: Context, fallback: SeatPlanConfig): SeatPlanConfig {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(CONFIG_KEY, null) ?: return fallback
        return try {
            val o = JSONObject(raw)
            fallback.copy(
                pairMode = runCatching { SeatPlanPairMode.valueOf(o.optString("pairMode")) }.getOrDefault(fallback.pairMode),
                rollOrder = runCatching { SeatPlanRollOrder.valueOf(o.optString("rollOrder")) }.getOrDefault(fallback.rollOrder),
                term = o.optInt("term", fallback.term).coerceIn(1, 3),
                customExamTitle = o.optString("customExamTitle", fallback.customExamTitle),
                subtitle = o.optString("subtitle", fallback.subtitle),
                roomLabel = o.optString("roomLabel", fallback.roomLabel),
                customSchoolName = o.optString("customSchoolName", fallback.customSchoolName),
                customSchoolAddress = o.optString("customSchoolAddress", fallback.customSchoolAddress),
                showSchoolAddress = o.optBoolean("showSchoolAddress", fallback.showSchoolAddress),
                columns = o.optInt("columns", fallback.columns).coerceIn(1, 3),
                rows = o.optInt("rows", fallback.rows).coerceIn(3, 10),
                orientation = runCatching { SeatPlanOrientation.valueOf(o.optString("orientation")) }.getOrDefault(fallback.orientation),
                marginInches = o.optDouble("marginInches", fallback.marginInches.toDouble()).toFloat(),
                horizontalGapPt = o.optDouble("horizontalGapPt", fallback.horizontalGapPt.toDouble()).toFloat(),
                verticalGapPt = o.optDouble("verticalGapPt", fallback.verticalGapPt.toDouble()).toFloat(),
                bnFontId = o.optString("bnFontId", fallback.bnFontId).ifBlank { fallback.bnFontId },
                enFontId = o.optString("enFontId", fallback.enFontId).ifBlank { fallback.enFontId },
                schoolFontPt = o.optDouble("schoolFontPt", fallback.schoolFontPt.toDouble()).toFloat(),
                headerMetaFontPt = o.optDouble("headerMetaFontPt", fallback.headerMetaFontPt.toDouble()).toFloat(),
                studentNameFontPt = o.optDouble("studentNameFontPt", fallback.studentNameFontPt.toDouble()).toFloat(),
                classRollFontPt = o.optDouble("classRollFontPt", fallback.classRollFontPt.toDouble()).toFloat(),
                boldSchoolName = o.optBoolean("boldSchoolName", fallback.boldSchoolName),
                boldAddress = o.optBoolean("boldAddress", fallback.boldAddress),
                boldExamTitle = o.optBoolean("boldExamTitle", fallback.boldExamTitle),
                boldStudentName = o.optBoolean("boldStudentName", fallback.boldStudentName),
                boldClassRoll = o.optBoolean("boldClassRoll", fallback.boldClassRoll),
                borderStyle = runCatching { SeatPlanBorderStyle.valueOf(o.optString("borderStyle")) }.getOrDefault(fallback.borderStyle),
                borderWeight = runCatching { SeatPlanBorderWeight.valueOf(o.optString("borderWeight")) }.getOrDefault(fallback.borderWeight),
                cornerRadiusPt = o.optDouble("cornerRadiusPt", fallback.cornerRadiusPt.toDouble()).toFloat(),
                showCutLines = o.optBoolean("showCutLines", fallback.showCutLines),
                padRollTwoDigits = o.optBoolean("padRollTwoDigits", fallback.padRollTwoDigits),
                numeralMode = runCatching { SeatPlanNumeralMode.valueOf(o.optString("numeralMode")) }.getOrDefault(fallback.numeralMode)
            )
        } catch (_: Exception) {
            fallback
        }
    }
}
