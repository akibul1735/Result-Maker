package com.kgps.result.ai

import com.kgps.result.RELIGIONS
import com.kgps.result.bn
import com.kgps.result.fmtEn
import com.kgps.result.fromBn
import com.kgps.result.subjectsFor
import kotlin.math.abs

enum class FieldConfidence {
    SURE,
    SUSPECT,
    NEUTRAL
}

data class ArithFixSuggestion(
    val labelBn: String,
    val targetField: String, // "TB", "ACT", "LANG", "CT", "WR", "TOTAL", "CONT", "GRAND"
    val newValue: String
)

object ScanConstraintSolver {

    private fun parseValidMark(raw: String?, maxVal: Int): Double? {
        val clean = raw?.trim().orEmpty()
        if (clean.isEmpty()) return null
        val num = fromBn(clean).toDoubleOrNull() ?: return null
        if (num < -0.001 || num > maxVal.toDouble() + 0.001) return null
        return num.coerceIn(0.0, maxVal.toDouble())
    }

    private fun normalizeIssueKey(rawKey: String): String = when (rawKey.trim().lowercase()) {
        "roll" -> "roll"
        "name" -> "name"
        "cont", "continuous", "sheetcontinuoustotal" -> "CONT"
        "grand", "grandtotal", "sheetgrandtotal" -> "GRAND"
        else -> rawKey.trim().uppercase()
    }

    /**
     * একে অপরের উপর নির্ভরশীল গাণিতিক সম্পর্ক (TB+ACT+LANG+CT = CONT এবং CONT+WR = GRAND) ব্যবহার করে
     * নিশ্চিত মানগুলোর সাহায্যে অস্পষ্ট বা ফাঁকা ঘরের মান স্বয়ংক্রিয়ভাবে নির্ণয় করে।
     */
    fun reconcileInterdependencies(
        row: ScannedStudentRow,
        cls: Int,
        subKey: String,
        scope: ClassSheetImportScope = ClassSheetImportScope.BOTH
    ): ScannedStudentRow {
        if (scope == ClassSheetImportScope.WR_ONLY) return row
        val subDef = subjectsFor(cls.coerceIn(1, 20), RELIGIONS.first()).find { it.key == subKey }
            ?: return row

        val compSpecs = subDef.comps // [TB, ACT, LANG, CT] বা ART-এর জন্য [TOTAL]
        val compKeys = compSpecs.map { it.key }
        val compMax = compSpecs.associate { it.key to it.max }
        val maxCont = compSpecs.sumOf { it.max }
        val hasWritten = subDef.written > 0
        val maxWr = subDef.written
        val maxGrand = subDef.max
        val isArt = compKeys == listOf("TOTAL") && !hasWritten

        val marks = row.marksText.toMutableMap()
        var contText = row.sheetContinuousTotal?.trim().orEmpty()
        var grandText = row.sheetGrandTotal?.trim().orEmpty()
        val resolved = row.resolvedFields.toMutableSet()
        val deduced = row.deducedFields.toMutableMap()
        val aiIssuesNorm = row.aiIssues.entries.associate { (k, v) -> normalizeIssueKey(k) to v }

        fun getMax(k: String): Int = when (k) {
            "WR" -> maxWr
            "CONT" -> maxCont
            "GRAND" -> maxGrand
            else -> compMax[k] ?: 100
        }

        fun getRaw(k: String): String = when (k) {
            "CONT" -> contText
            "GRAND" -> grandText
            else -> marks[k]?.trim().orEmpty()
        }

        fun setDeduced(k: String, value: Double, reason: String) {
            val formatted = fmtEn(value)
            when (k) {
                "CONT" -> contText = formatted
                "GRAND" -> grandText = formatted
                else -> marks[k] = formatted
            }
            resolved.add(k)
            deduced[k] = reason
        }

        fun isUncertain(k: String): Boolean = aiIssuesNorm.containsKey(k) && k !in resolved

        fun sureVal(k: String): Double? {
            if (isUncertain(k)) return null
            return parseValidMark(getRaw(k), getMax(k))
        }

        fun canDeduce(k: String): Boolean {
            if (k in resolved) return false
            val valid = parseValidMark(getRaw(k), getMax(k))
            return valid == null || isUncertain(k)
        }

        fun fitsRange(v: Double, maxVal: Int): Boolean = v >= -0.001 && v <= maxVal.toDouble() + 0.001

        // সর্বোচ্চ ৪ বার ইটারেশন (ধারাবাহিক <-> সর্বমোট ক্যাসকেড সমাধানের জন্য)
        repeat(4) {
            var changed = false

            if (isArt) {
                val totalVal = sureVal("TOTAL")
                val grandVal = sureVal("GRAND")
                if (totalVal != null && canDeduce("GRAND")) {
                    setDeduced("GRAND", totalVal, "মোট (${bn(fmtEn(totalVal))}) থেকে নিশ্চিত")
                    changed = true
                } else if (grandVal != null && canDeduce("TOTAL")) {
                    setDeduced("TOTAL", grandVal, "সর্বমোট (${bn(fmtEn(grandVal))}) থেকে নিশ্চিত")
                    changed = true
                }
            } else {
                // ১. ধারাবাহিক সমীকরণ: TB + ACT + LANG + CT = CONT
                val compSure = compKeys.associateWith { sureVal(it) }
                val contSure = sureVal("CONT")

                val knownCount = compSure.values.count { it != null }
                if (knownCount == compKeys.size) {
                    val sum = compSure.values.filterNotNull().sum()
                    if (canDeduce("CONT") && fitsRange(sum, maxCont)) {
                        setDeduced(
                            "CONT",
                            sum.coerceIn(0.0, maxCont.toDouble()),
                            "ধারাবাহিক অংশগুলোর যোগফল = ${bn(fmtEn(sum))}"
                        )
                        changed = true
                    }
                } else if (contSure != null && knownCount == compKeys.size - 1) {
                    val missingKey = compKeys.first { compSure[it] == null }
                    if (canDeduce(missingKey)) {
                        val restSum = compSure.values.filterNotNull().sum()
                        val derived = contSure - restSum
                        val mMax = getMax(missingKey)
                        if (fitsRange(derived, mMax)) {
                            val clamped = derived.coerceIn(0.0, mMax.toDouble())
                            setDeduced(
                                missingKey,
                                clamped,
                                "ধারাবাহিক মোট (${bn(fmtEn(contSure))}) − বাকি (${bn(fmtEn(restSum))}) = ${bn(fmtEn(clamped))}"
                            )
                            changed = true
                        }
                    }
                }

                // ২. সর্বমোট সমীকরণ: কেবল BOTH মোডে (CONT_ONLY মোডে WR বা GRAND অনুমান করবে না)
                if (scope == ClassSheetImportScope.BOTH) {
                    val contNow = sureVal("CONT")
                    val grandNow = sureVal("GRAND")

                    if (hasWritten) {
                        val wrNow = sureVal("WR")
                        if (contNow != null && wrNow != null && canDeduce("GRAND")) {
                            val g = contNow + wrNow
                            if (fitsRange(g, maxGrand)) {
                                val clamped = g.coerceIn(0.0, maxGrand.toDouble())
                                setDeduced(
                                    "GRAND",
                                    clamped,
                                    "ধারাবাহিক (${bn(fmtEn(contNow))}) + সামষ্টিক (${bn(fmtEn(wrNow))}) = ${bn(fmtEn(clamped))}"
                                )
                                changed = true
                            }
                        } else if (grandNow != null && contNow != null && canDeduce("WR")) {
                            val w = grandNow - contNow
                            if (fitsRange(w, maxWr)) {
                                val clamped = w.coerceIn(0.0, maxWr.toDouble())
                                setDeduced(
                                    "WR",
                                    clamped,
                                    "মোট (${bn(fmtEn(grandNow))}) − ধারাবাহিক (${bn(fmtEn(contNow))}) = ${bn(fmtEn(clamped))}"
                                )
                                changed = true
                            }
                        } else if (grandNow != null && wrNow != null && canDeduce("CONT")) {
                            val c = grandNow - wrNow
                            if (fitsRange(c, maxCont)) {
                                val clamped = c.coerceIn(0.0, maxCont.toDouble())
                                setDeduced(
                                    "CONT",
                                    clamped,
                                    "মোট (${bn(fmtEn(grandNow))}) − সামষ্টিক (${bn(fmtEn(wrNow))}) = ${bn(fmtEn(clamped))}"
                                )
                                changed = true
                            }
                        }
                    } else {
                        if (contNow != null && canDeduce("GRAND")) {
                            setDeduced("GRAND", contNow, "ধারাবাহিক মোট (${bn(fmtEn(contNow))}) সমান")
                            changed = true
                        } else if (grandNow != null && canDeduce("CONT")) {
                            setDeduced("CONT", grandNow, "মোট (${bn(fmtEn(grandNow))}) সমান")
                            changed = true
                        }
                    }
                }
            }

            if (!changed) return@repeat
        }

        return row.copy(
            marksText = marks,
            sheetContinuousTotal = contText.ifBlank { null },
            sheetGrandTotal = grandText.ifBlank { null },
            resolvedFields = resolved,
            deducedFields = deduced
        )
    }

    fun validateRow(
        row: ScannedStudentRow,
        cls: Int,
        subKey: String,
        duplicateRolls: Set<Int>,
        hasExistingDbName: Boolean = false,
        scope: ClassSheetImportScope = ClassSheetImportScope.BOTH
    ): RowValidationReport {
        val hard = mutableMapOf<String, String>()
        val warn = mutableMapOf<String, String>()
        val aiNorm = row.aiIssues.entries.associate { (k, v) -> normalizeIssueKey(k) to v }
        val dbNameActive = hasExistingDbName && !row.useScannedNameForExisting

        val subDef = subjectsFor(cls.coerceIn(1, 20), RELIGIONS.first()).find { it.key == subKey }
        val compSpecs = subDef?.comps ?: emptyList()
        val compKeys = compSpecs.map { it.key }
        val hasWritten = (subDef?.written ?: 0) > 0
        val maxCont = compSpecs.sumOf { it.max }
        val maxWr = subDef?.written ?: 0
        val maxGrand = subDef?.max ?: 100
        val isArt = compKeys == listOf("TOTAL") && !hasWritten

        val expectCont = scope != ClassSheetImportScope.WR_ONLY
        val expectWr = hasWritten && scope != ClassSheetImportScope.CONT_ONLY
        val expectGrand = scope == ClassSheetImportScope.BOTH

        val rawCont = row.sheetContinuousTotal?.trim().orEmpty()
        val rawGrand = row.sheetGrandTotal?.trim().orEmpty()
        val rawWr = row.marksText["WR"]?.trim().orEmpty()
        val compKeysSet = compKeys.toSet()

        // সহজ / সারসংক্ষেপ মোড: পাঠ্যবই–ক্লাস টেস্ট নেই, কেবল ধারাবাহিক মোট এবং/অথবা সামষ্টিক আছে
        val isSummaryOnlyMode = !isArt &&
            compKeys.all { row.marksText[it]?.trim().isNullOrEmpty() } &&
            (rawCont.isNotEmpty() || rawGrand.isNotEmpty() || rawWr.isNotEmpty() || scope != ClassSheetImportScope.BOTH)

        aiNorm.forEach { (field, reason) ->
            if (field == "name" && (dbNameActive || row.name.isBlank())) return@forEach
            if (!expectCont && (field in compKeysSet || field == "CONT")) return@forEach
            if (!expectWr && field == "WR") return@forEach
            if (!expectGrand && field == "GRAND") return@forEach
            if (isSummaryOnlyMode && field in compKeysSet) return@forEach
            if (field !in row.resolvedFields) {
                warn[field] = reason.ifBlank { "লেখা অস্পষ্ট — যাচাই করুন" }
            }
        }

        val rollClean = fromBn(row.rollText.trim())
        val rollNum = rollClean.toIntOrNull()
        if (rollClean.isBlank() || rollNum == null || rollNum <= 0) {
            hard["roll"] = "সঠিক রোল লিখুন (১, ২, ৩...)"
        } else if (rollNum in duplicateRolls) {
            hard["roll"] = "রোল ${bn(rollNum.toString())} একাধিক সারিতে আছে"
        }

        val expectedMarkKeys = buildList {
            if (expectCont) compSpecs.forEach { add(it.key to it.max) }
            if (expectWr) add("WR" to maxWr)
        }

        val parsedMarks = mutableMapOf<String, Double>()
        var contSum = 0.0
        var hasAllComps = expectCont && compKeys.isNotEmpty()
        var hasAnyComp = false

        expectedMarkKeys.forEach { (cKey, maxVal) ->
            val raw = row.marksText[cKey]?.trim().orEmpty()
            if (raw.isEmpty()) {
                if (cKey != "WR") hasAllComps = false
                val isOptionalSummaryComp = isSummaryOnlyMode && cKey in compKeysSet
                if (!isOptionalSummaryComp && cKey !in row.resolvedFields && cKey !in warn) {
                    warn[cKey] = "মান ফাঁকা — যাচাই করুন"
                }
                return@forEach
            }
            val num = fromBn(raw).toDoubleOrNull()
            if (num == null) {
                if (cKey != "WR") hasAllComps = false
                hard[cKey] = "\"$raw\" সঠিক সংখ্যা নয়"
            } else if (num < 0.0 || num > maxVal.toDouble()) {
                if (cKey != "WR") hasAllComps = false
                hard[cKey] = "সর্বোচ্চ ${bn(maxVal.toString())}-এর মধ্যে হতে হবে"
            } else {
                parsedMarks[cKey] = num
                if (cKey != "WR") {
                    contSum += num
                    hasAnyComp = true
                }
            }
        }

        // ধারাবাহিক মোট (CONT) পার্সিং
        val sheetCont = if (!expectCont || rawCont.isEmpty()) null else fromBn(rawCont).toDoubleOrNull()?.also {
            if (it < 0.0 || it > maxCont.toDouble()) {
                hard["CONT"] = "সর্বোচ্চ ${bn(maxCont.toString())}-এর মধ্যে হতে হবে"
            }
        } ?: run {
            hard["CONT"] = "\"$rawCont\" সঠিক সংখ্যা নয়"
            null
        }
        if (expectCont && !isArt && rawCont.isEmpty() && "CONT" !in row.resolvedFields && "CONT" !in warn) {
            warn["CONT"] = "ধারাবাহিক মোট ফাঁকা"
        }

        // সর্বমোট (GRAND) পার্সিং (কেবল BOTH মোডে)
        val sheetGrand = if (!expectGrand || rawGrand.isEmpty()) null else fromBn(rawGrand).toDoubleOrNull()?.also {
            if (it < 0.0 || it > maxGrand.toDouble()) {
                hard["GRAND"] = "সর্বোচ্চ ${bn(maxGrand.toString())}-এর মধ্যে হতে হবে"
            }
        } ?: run {
            hard["GRAND"] = "\"$rawGrand\" সঠিক সংখ্যা নয়"
            null
        }
        if (expectGrand && rawGrand.isEmpty() && "GRAND" !in row.resolvedFields && "GRAND" !in warn) {
            warn["GRAND"] = "মোট নম্বর ফাঁকা"
        }

        val effectiveCont = if (!expectCont) null else if (hasAnyComp) contSum else sheetCont
        val wrVal = parsedMarks["WR"]
        val calcGrand = when {
            !expectGrand -> null
            isArt -> parsedMarks["TOTAL"] ?: sheetGrand
            hasWritten && effectiveCont != null && wrVal != null -> effectiveCont + wrVal
            !hasWritten && effectiveCont != null -> effectiveCont
            else -> null
        }

        // আন্তঃনির্ভরশীলতা যাচাই ও ত্রুটি/সাজেশন
        var arithWarn: String? = null
        val mismatchFields = mutableSetOf<String>()
        val fixes = mutableListOf<ArithFixSuggestion>()

        if ("arithmetic" !in row.resolvedFields && expectCont) {
            if (isArt) {
                val totalVal = parsedMarks["TOTAL"]
                if (totalVal != null && sheetGrand != null && abs(totalVal - sheetGrand) > 0.01) {
                    mismatchFields.addAll(listOf("TOTAL", "GRAND"))
                    arithWarn = "শিল্পকলা (${bn(fmtEn(totalVal))}) ও মোট (${bn(fmtEn(sheetGrand))}) মিলছে না"
                    fixes.add(ArithFixSuggestion("মোট = ${bn(fmtEn(totalVal))}", "GRAND", fmtEn(totalVal)))
                }
            } else {
                val contMismatch = hasAllComps && sheetCont != null && abs(contSum - sheetCont) > 0.01
                val contRefForGrand = sheetCont ?: if (hasAllComps) contSum else null
                val grandMismatch = if (!expectGrand) false else if (hasWritten) {
                    contRefForGrand != null && wrVal != null && sheetGrand != null &&
                        abs((contRefForGrand + wrVal) - sheetGrand) > 0.01
                } else {
                    contRefForGrand != null && sheetGrand != null &&
                        abs(contRefForGrand - sheetGrand) > 0.01
                }

                if (contMismatch) {
                    compKeys.forEach { if (it !in row.resolvedFields) mismatchFields.add(it) }
                    if ("CONT" !in row.resolvedFields) mismatchFields.add("CONT")
                    if (mismatchFields.isEmpty()) {
                        mismatchFields.addAll(compKeys)
                        mismatchFields.add("CONT")
                    }
                    fixes.add(
                        ArithFixSuggestion(
                            "ধারাবাহিক = ${bn(fmtEn(contSum))} করুন",
                            "CONT",
                            fmtEn(contSum)
                        )
                    )
                }

                if (grandMismatch && sheetGrand != null && contRefForGrand != null) {
                    if ("CONT" !in row.resolvedFields) mismatchFields.add("CONT")
                    if (hasWritten && "WR" !in row.resolvedFields) mismatchFields.add("WR")
                    if ("GRAND" !in row.resolvedFields) mismatchFields.add("GRAND")
                    if (mismatchFields.none { it in setOf("CONT", "WR", "GRAND") }) {
                        mismatchFields.addAll(if (hasWritten) listOf("CONT", "WR", "GRAND") else listOf("CONT", "GRAND"))
                    }

                    if (hasWritten && wrVal != null) {
                        val impliedWr = sheetGrand - contRefForGrand
                        if (impliedWr >= 0.0 && impliedWr <= maxWr.toDouble()) {
                            fixes.add(
                                ArithFixSuggestion(
                                    "সামষ্টিক = ${bn(fmtEn(impliedWr))} (${bn(fmtEn(sheetGrand))}−${bn(fmtEn(contRefForGrand))})",
                                    "WR",
                                    fmtEn(impliedWr)
                                )
                            )
                        }
                        val impliedCont = sheetGrand - wrVal
                        if (impliedCont >= 0.0 && impliedCont <= maxCont.toDouble() && !contMismatch) {
                            fixes.add(
                                ArithFixSuggestion(
                                    "ধারাবাহিক = ${bn(fmtEn(impliedCont))} (${bn(fmtEn(sheetGrand))}−${bn(fmtEn(wrVal))})",
                                    "CONT",
                                    fmtEn(impliedCont)
                                )
                            )
                        }
                        val impliedGrand = contRefForGrand + wrVal
                        if (impliedGrand >= 0.0 && impliedGrand <= maxGrand.toDouble()) {
                            fixes.add(
                                ArithFixSuggestion(
                                    "মোট = ${bn(fmtEn(impliedGrand))} (${bn(fmtEn(contRefForGrand))}+${bn(fmtEn(wrVal))})",
                                    "GRAND",
                                    fmtEn(impliedGrand)
                                )
                            )
                        }
                    } else if (!hasWritten) {
                        fixes.add(
                            ArithFixSuggestion(
                                "মোট = ${bn(fmtEn(contRefForGrand))} করুন",
                                "GRAND",
                                fmtEn(contRefForGrand)
                            )
                        )
                    }
                }

                arithWarn = when {
                    contMismatch && grandMismatch ->
                        "ধারাবাহিক (${bn(fmtEn(contSum))} ≠ ${bn(fmtEn(sheetCont!!))}) ও সর্বমোট (${bn(fmtEn(sheetGrand!!))}) হিসাবে অমিল আছে"
                    contMismatch ->
                        "অংশগুলোর যোগফল ${bn(fmtEn(contSum))}, কিন্তু ধারাবাহিক মোট ${bn(fmtEn(sheetCont!!))}"
                    grandMismatch && hasWritten && wrVal != null ->
                        "ধারাবাহিক (${bn(fmtEn(contRefForGrand!!))}) + সামষ্টিক (${bn(fmtEn(wrVal))}) = ${bn(fmtEn(contRefForGrand + wrVal))}, কিন্তু মোট ${bn(fmtEn(sheetGrand!!))}"
                    grandMismatch ->
                        "ধারাবাহিক (${bn(fmtEn(contRefForGrand!!))}) ও মোট (${bn(fmtEn(sheetGrand!!))}) মিলছে না"
                    else -> null
                }
            }
        }

        // প্রতিটি ফিল্ডের শিউর (সবুজ) বনাম সন্দেহযুক্ত (লাল) বনাম ঐচ্ছিক (নিউট্রাল) অবস্থা নির্ধারণ
        val allFieldKeys = buildList {
            add("roll")
            add("name")
            if (expectCont) {
                addAll(compKeys)
                if (!isArt) add("CONT")
            }
            if (expectWr) add("WR")
            if (expectGrand) add("GRAND")
        }

        val confidences = mutableMapOf<String, FieldConfidence>()
        allFieldKeys.forEach { key ->
            val isSuspect = key in hard || key in warn || key in mismatchFields
            val isOptionalCell = (isSummaryOnlyMode && key in compKeysSet) ||
                (key == "name" && !dbNameActive && row.name.isBlank())
            confidences[key] = when {
                isSuspect -> FieldConfidence.SUSPECT
                isOptionalCell -> FieldConfidence.NEUTRAL
                else -> FieldConfidence.SURE
            }
        }

        return RowValidationReport(
            hardErrors = hard,
            uncertainWarnings = warn,
            arithmeticWarning = arithWarn,
            calculatedContinuous = effectiveCont,
            calculatedGrandTotal = calcGrand,
            fieldConfidences = confidences,
            mismatchFields = mismatchFields,
            quickFixes = fixes.distinctBy { "${it.targetField}:${it.newValue}" },
            isSummaryOnlyMode = isSummaryOnlyMode
        )
    }
}
