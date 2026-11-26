package com.kgps.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

object AdmitCardLocalStore {
    private const val PREFS_NAME = "admit_card_prefs"
    private const val SIGNATURE_FILE = "admit_head_signature.png"
    private const val BASE_ROUTINES_KEY = "base_routines_v1"
    private const val CONFIG_KEY = "admit_card_config_v1"

    private fun parseRoutineArray(arr: JSONArray): List<ExamRoutineEntry> = buildList {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val subjectsArr = o.optJSONArray("subjects")
            val subjectsList = if (subjectsArr != null) {
                buildList {
                    for (j in 0 until subjectsArr.length()) {
                        add(subjectsArr.optString(j))
                    }
                }.ifEmpty { listOf("") }
            } else {
                ExamRoutineEntry.parseSubjects(o.optString("subject"))
            }
            add(
                ExamRoutineEntry(
                    id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                    date = o.optString("date"),
                    day = o.optString("day"),
                    subjects = subjectsList
                )
            )
        }
    }

    private fun serializeRoutineArray(rows: List<ExamRoutineEntry>): JSONArray = JSONArray().apply {
        rows.forEach { r ->
            put(
                JSONObject().apply {
                    put("id", r.id)
                    put("date", r.date)
                    put("day", r.day)
                    put("subject", r.subject)
                    put(
                        "subjects",
                        JSONArray().apply {
                            r.subjects.forEach { put(it) }
                        }
                    )
                }
            )
        }
    }

    fun loadRoutine(ctx: Context, cls: Int, settings: Settings): List<ExamRoutineEntry> {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("routine_cls_$cls", null)
        if (!raw.isNullOrBlank()) {
            try {
                val parsed = parseRoutineArray(JSONArray(raw))
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Exception) {
                // Fall back to default class subjects
            }
        }
        return AdmitCardPlanner.defaultRoutineForClass(cls, settings)
    }

    fun saveRoutine(ctx: Context, cls: Int, rows: List<ExamRoutineEntry>) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("routine_cls_$cls", serializeRoutineArray(rows).toString())
            .apply()
    }

    fun loadExamTime(ctx: Context, cls: Int, settings: Settings): String {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val perClass = prefs.getString("exam_time_cls_$cls", null)
        if (!perClass.isNullOrBlank()) return perClass
        return prefs.getString("exam_time", null) ?: AdmitCardPlanner.defaultExamTimeForClass(cls, settings)
    }

    fun saveExamTime(ctx: Context, cls: Int, examTime: String) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("exam_time_cls_$cls", examTime)
            .apply()
    }

    fun saveConfig(ctx: Context, cfg: AdmitCardConfig) {
        val obj = JSONObject().apply {
            put("rollOrder", cfg.rollOrder.name)
            put("term", cfg.term)
            put("cardsPerPage", cfg.cardsPerPage)
            put("marginInches", cfg.marginInches.toDouble())
            put("verticalGapPt", cfg.verticalGapPt.toDouble())
            put("bnFontId", cfg.bnFontId)
            put("enFontId", cfg.enFontId)
            put("borderStyle", cfg.borderStyle.name)
            put("dashStyle", cfg.dashStyle.name)
            put("borderWeight", cfg.borderWeight.name)
            put("cornerRadiusPt", cfg.cornerRadiusPt.toDouble())
            put("showCutLines", cfg.showCutLines)
            put("showCenterDivider", cfg.showCenterDivider)
            put("padRollTwoDigits", cfg.padRollTwoDigits)
            put("numeralMode", cfg.numeralMode.name)
            put("customSchoolName", cfg.customSchoolName)
            put("customSchoolAddress", cfg.customSchoolAddress)
            put("showSchoolAddress", cfg.showSchoolAddress)
            put("customExamTitle", cfg.customExamTitle)
            put("badgeLabel", cfg.badgeLabel)
            put("customRoutineTitle", cfg.customRoutineTitle)
            put("headTeacherTitle", cfg.headTeacherTitle)
            put("showSignatureLine", cfg.showSignatureLine)
            put("showRoutineTable", cfg.showRoutineTable)
            put("includeBlankRoutineRow", cfg.includeBlankRoutineRow)
            put("routineNote", cfg.routineNote)
            put("showSignatureImage", cfg.showSignatureImage)
            put("signatureScale", cfg.signatureScale.toDouble())
            put("headerToInfoGapPt", cfg.headerToInfoGapPt.toDouble())
            put("studentInfoOffsetXPt", cfg.studentInfoOffsetXPt.toDouble())
            put("schoolFontPt", cfg.schoolFontPt.toDouble())
            put("headerMetaFontPt", cfg.headerMetaFontPt.toDouble())
            put("headerBold", cfg.headerBold)
            put("badgeFontPt", cfg.badgeFontPt.toDouble())
            put("badgeBold", cfg.badgeBold)
            put("badgeUnderline", cfg.badgeUnderline)
            put("studentInfoFontPt", cfg.studentInfoFontPt.toDouble())
            put("studentLabelBold", cfg.studentLabelBold)
            put("studentValueBold", cfg.studentValueBold)
            put("routineHeaderFontPt", cfg.routineHeaderFontPt.toDouble())
            put("routineHeaderBold", cfg.routineHeaderBold)
            put("routineBodyFontPt", cfg.routineBodyFontPt.toDouble())
            put("routineBodyBold", cfg.routineBodyBold)
            put("signatureTitleFontPt", cfg.signatureTitleFontPt.toDouble())
            put("signatureTitleBold", cfg.signatureTitleBold)
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(CONFIG_KEY, obj.toString())
            .apply()
    }

    fun loadConfig(ctx: Context, fallback: AdmitCardConfig): AdmitCardConfig {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(CONFIG_KEY, null) ?: return fallback
        return try {
            val o = JSONObject(raw)
            fallback.copy(
                rollOrder = runCatching { SeatPlanRollOrder.valueOf(o.optString("rollOrder")) }.getOrDefault(fallback.rollOrder),
                term = o.optInt("term", fallback.term).coerceIn(1, 3),
                cardsPerPage = o.optInt("cardsPerPage", fallback.cardsPerPage).coerceIn(1, 6),
                marginInches = o.optDouble("marginInches", fallback.marginInches.toDouble()).toFloat(),
                verticalGapPt = o.optDouble("verticalGapPt", fallback.verticalGapPt.toDouble()).toFloat(),
                bnFontId = o.optString("bnFontId", fallback.bnFontId).ifBlank { fallback.bnFontId },
                enFontId = o.optString("enFontId", fallback.enFontId).ifBlank { fallback.enFontId },
                borderStyle = runCatching { AdmitBorderStyle.valueOf(o.optString("borderStyle")) }.getOrDefault(fallback.borderStyle),
                dashStyle = runCatching { AdmitDashStyle.valueOf(o.optString("dashStyle")) }.getOrDefault(fallback.dashStyle),
                borderWeight = runCatching { SeatPlanBorderWeight.valueOf(o.optString("borderWeight")) }.getOrDefault(fallback.borderWeight),
                cornerRadiusPt = o.optDouble("cornerRadiusPt", fallback.cornerRadiusPt.toDouble()).toFloat(),
                showCutLines = o.optBoolean("showCutLines", fallback.showCutLines),
                showCenterDivider = o.optBoolean("showCenterDivider", fallback.showCenterDivider),
                padRollTwoDigits = o.optBoolean("padRollTwoDigits", fallback.padRollTwoDigits),
                numeralMode = runCatching { SeatPlanNumeralMode.valueOf(o.optString("numeralMode")) }.getOrDefault(fallback.numeralMode),
                customSchoolName = o.optString("customSchoolName", fallback.customSchoolName),
                customSchoolAddress = o.optString("customSchoolAddress", fallback.customSchoolAddress),
                showSchoolAddress = o.optBoolean("showSchoolAddress", fallback.showSchoolAddress),
                customExamTitle = o.optString("customExamTitle", fallback.customExamTitle),
                badgeLabel = o.optString("badgeLabel", fallback.badgeLabel),
                customRoutineTitle = o.optString("customRoutineTitle", fallback.customRoutineTitle),
                headTeacherTitle = o.optString("headTeacherTitle", fallback.headTeacherTitle),
                showSignatureLine = o.optBoolean("showSignatureLine", fallback.showSignatureLine),
                showRoutineTable = o.optBoolean("showRoutineTable", fallback.showRoutineTable),
                includeBlankRoutineRow = o.optBoolean("includeBlankRoutineRow", fallback.includeBlankRoutineRow),
                routineNote = o.optString("routineNote", fallback.routineNote),
                showSignatureImage = o.optBoolean("showSignatureImage", fallback.showSignatureImage),
                signatureScale = o.optDouble("signatureScale", fallback.signatureScale.toDouble()).toFloat(),
                headerToInfoGapPt = o.optDouble("headerToInfoGapPt", fallback.headerToInfoGapPt.toDouble()).toFloat(),
                studentInfoOffsetXPt = o.optDouble("studentInfoOffsetXPt", fallback.studentInfoOffsetXPt.toDouble()).toFloat(),
                schoolFontPt = o.optDouble("schoolFontPt", fallback.schoolFontPt.toDouble()).toFloat(),
                headerMetaFontPt = o.optDouble("headerMetaFontPt", fallback.headerMetaFontPt.toDouble()).toFloat(),
                headerBold = o.optBoolean("headerBold", fallback.headerBold),
                badgeFontPt = o.optDouble("badgeFontPt", fallback.badgeFontPt.toDouble()).toFloat(),
                badgeBold = o.optBoolean("badgeBold", fallback.badgeBold),
                badgeUnderline = o.optBoolean("badgeUnderline", fallback.badgeUnderline),
                studentInfoFontPt = o.optDouble("studentInfoFontPt", fallback.studentInfoFontPt.toDouble()).toFloat(),
                studentLabelBold = o.optBoolean("studentLabelBold", fallback.studentLabelBold),
                studentValueBold = o.optBoolean("studentValueBold", fallback.studentValueBold),
                routineHeaderFontPt = o.optDouble("routineHeaderFontPt", fallback.routineHeaderFontPt.toDouble()).toFloat(),
                routineHeaderBold = o.optBoolean("routineHeaderBold", fallback.routineHeaderBold),
                routineBodyFontPt = o.optDouble("routineBodyFontPt", fallback.routineBodyFontPt.toDouble()).toFloat(),
                routineBodyBold = o.optBoolean("routineBodyBold", fallback.routineBodyBold),
                signatureTitleFontPt = o.optDouble("signatureTitleFontPt", fallback.signatureTitleFontPt.toDouble()).toFloat(),
                signatureTitleBold = o.optBoolean("signatureTitleBold", fallback.signatureTitleBold)
            )
        } catch (_: Exception) {
            fallback
        }
    }

    fun loadBaseRoutines(ctx: Context, settings: Settings): List<BaseRoutineTemplate> {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(BASE_ROUTINES_KEY, null)
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                val parsed = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val id = o.optString("id").ifBlank { "base_${i + 1}" }
                        val name = o.optString("name").ifBlank { "বেস রুটিন ${bn((i + 1).toString())}" }
                        val time = o.optString("examTimeLabel").ifBlank { "দুপুর ০১:০০-০৩:৩০ টা" }
                        val rowsArr = o.optJSONArray("rows") ?: JSONArray()
                        add(
                            BaseRoutineTemplate(
                                id = id,
                                name = name,
                                examTimeLabel = time,
                                rows = parseRoutineArray(rowsArr)
                            )
                        )
                    }
                }
                if (parsed.isNotEmpty()) return parsed
            } catch (_: Exception) {
                // Fall back to default base routines
            }
        }
        return AdmitCardPlanner.defaultBaseRoutines(settings)
    }

    fun saveBaseRoutines(ctx: Context, bases: List<BaseRoutineTemplate>) {
        val arr = JSONArray()
        bases.forEach { b ->
            arr.put(
                JSONObject().apply {
                    put("id", b.id)
                    put("name", b.name)
                    put("examTimeLabel", b.examTimeLabel)
                    put("rows", serializeRoutineArray(b.rows))
                }
            )
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(BASE_ROUTINES_KEY, arr.toString())
            .apply()
    }

    fun signatureFile(ctx: Context): File = File(ctx.filesDir, SIGNATURE_FILE)

    fun loadSignatureBitmap(ctx: Context): Bitmap? {
        val f = signatureFile(ctx)
        if (!f.exists() || f.length() <= 0L) return null
        return try {
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    fun clearSignature(ctx: Context) {
        runCatching {
            val f = signatureFile(ctx)
            if (f.exists()) f.delete()
        }
    }

    /**
     * Reads picked image via ContentResolver, scales to compact stamp dimensions,
     * removes bright white paper background cleanly for crisp PDF stamping, and persists PNG.
     */
    fun saveSignatureFromUri(ctx: Context, uri: Uri): Bitmap? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { inp ->
                BitmapFactory.decodeStream(inp, null, boundsOpts)
            }
            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return null

            var sample = 1
            while ((srcW / sample) > 960 || (srcH / sample) > 480) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            val rawBmp = ctx.contentResolver.openInputStream(uri)?.use { inp ->
                BitmapFactory.decodeStream(inp, null, decodeOpts)
            } ?: return null

            val maxW = 480
            val maxH = 240
            val scale = min(maxW.toFloat() / rawBmp.width, maxH.toFloat() / rawBmp.height).coerceAtMost(1f)
            val targetW = (rawBmp.width * scale).toInt().coerceAtLeast(1)
            val targetH = (rawBmp.height * scale).toInt().coerceAtLeast(1)
            val scaled = if (targetW != rawBmp.width || targetH != rawBmp.height) {
                Bitmap.createScaledBitmap(rawBmp, targetW, targetH, true).also {
                    if (it !== rawBmp) rawBmp.recycle()
                }
            } else {
                rawBmp
            }

            val cleaned = stripWhitePaperBackground(scaled)
            if (cleaned !== scaled) scaled.recycle()

            FileOutputStream(signatureFile(ctx)).use { out ->
                cleaned.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            cleaned
        } catch (_: Exception) {
            null
        }
    }

    private fun stripWhitePaperBackground(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val px = pixels[i]
            val a = Color.alpha(px)
            if (a == 0) continue
            val r = Color.red(px)
            val g = Color.green(px)
            val b = Color.blue(px)
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            val spread = maxC - minC
            val lum = (r + g + b) / 3
            if (lum >= 225 && spread <= 28) {
                pixels[i] = Color.TRANSPARENT
            } else if (lum >= 195 && spread <= 32) {
                val nextA = ((225 - lum).coerceIn(0, 30) * a / 30).coerceIn(0, a)
                pixels[i] = Color.argb(nextA, r, g, b)
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }
}
