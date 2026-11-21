package com.kgps.result.ai.offline

import android.graphics.Bitmap
import com.kgps.result.RELIGIONS
import com.kgps.result.Student
import com.kgps.result.ai.ClassSheetImportScope
import com.kgps.result.ai.ExtractedClassSheet
import com.kgps.result.ai.ScanConstraintSolver
import com.kgps.result.ai.ScannedStudentRow
import com.kgps.result.bn
import com.kgps.result.subjectsFor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class OfflineScanOutput(
    val sheet: ExtractedClassSheet,
    val diagnostics: OfflineScanDiagnostics
)

object OfflineTableScanner {

    private const val CONFIDENCE_SURE_THRESHOLD = 0.78f

    fun scanBitmap(
        bitmap: Bitmap,
        recognizer: DualScriptDigitRecognizer,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        importScope: ClassSheetImportScope,
        digitScript: OfflineDigitScript,
        layoutPreset: OfflineTableLayoutPreset,
        existingStudents: List<Student> = emptyList()
    ): OfflineScanOutput {
        val gray = GrayImage.fromBitmap(bitmap, maxLongestSide = 1600)
        return scanGrayImage(
            grayImage = gray,
            recognizer = recognizer,
            clsHint = clsHint,
            termHint = termHint,
            subKeyHint = subKeyHint,
            importScope = importScope,
            digitScript = digitScript,
            layoutPreset = layoutPreset,
            existingStudents = existingStudents
        )
    }

    fun scanGrayImage(
        grayImage: GrayImage,
        recognizer: DualScriptDigitRecognizer,
        clsHint: Int?,
        termHint: Int?,
        subKeyHint: String?,
        importScope: ClassSheetImportScope,
        digitScript: OfflineDigitScript,
        layoutPreset: OfflineTableLayoutPreset,
        existingStudents: List<Student> = emptyList()
    ): OfflineScanOutput {
        val startMs = System.currentTimeMillis()
        val cls = (clsHint ?: 3).coerceIn(1, 5)
        val term = (termHint ?: 1).coerceIn(1, 3)
        val subs = subjectsFor(cls, RELIGIONS.first())
        val activeSub = subs.find { it.key == subKeyHint } ?: subs.first()
        val subKey = activeSub.key

        val grid = TableGridSlicer.sliceTable(grayImage)
        if (grid.dataRowBounds.isEmpty() || grid.colBounds.size < 2) {
            throw IllegalStateException(
                "ছবিতে টেবিলের সারি বা কলাম স্পষ্টভাবে শনাক্ত করা যায়নি (কলাম: ${grid.colBounds.size}, সারি: ${grid.dataRowBounds.size})। টেবিল অংশ ক্রপ করে সোজা অবস্থায় আবার চেষ্টা করুন।"
            )
        }

        val (roles, skippedNameColIdx) = assignColumnRoles(
            grid = grid,
            cls = cls,
            subKey = subKey,
            importScope = importScope,
            layoutPreset = layoutPreset
        )

        val effectiveScript = if (digitScript == OfflineDigitScript.AUTO) {
            detectDominantScript(grid, recognizer, roles)
        } else {
            digitScript
        }

        val rollColIdx = roles.indexOf(ColumnRole.ROLL)
        if (rollColIdx < 0) {
            throw IllegalStateException("টেবিলে রোল কলাম শনাক্ত করা যায়নি। অনুগ্রহ করে রোল, নাম ও নম্বরসহ পুরো টেবিলটি ক্রপ করুন।")
        }

        val markColumns = roles.mapIndexedNotNull { idx, role ->
            role.toMarkFieldKey()?.let { idx to it }
        }

        // Read all cells across all data rows
        val rawRows = ArrayList<RawScannedRow>(grid.dataRowBounds.size)
        for ((rIdx, yr) in grid.dataRowBounds.withIndex()) {
            val rollXr = grid.colBounds[rollColIdx]
            val rollRead = CellDigitSegmenter.readNumericCell(
                grid.image, rollXr.first, yr.first, rollXr.last, yr.last, recognizer, effectiveScript
            )
            val markReads = LinkedHashMap<String, CellReadResult>()
            for ((colIdx, fieldKey) in markColumns) {
                val xr = grid.colBounds[colIdx]
                markReads[fieldKey] = CellDigitSegmenter.readNumericCell(
                    grid.image, xr.first, yr.first, xr.last, yr.last, recognizer, effectiveScript
                )
            }
            if (!rollRead.isBlank || markReads.values.any { !it.isBlank }) {
                rawRows.add(RawScannedRow(rIdx + 1, rollRead, markReads))
            }
        }

        if (rawRows.isEmpty()) {
            throw IllegalStateException(
                "টেবিলের কোনো সারিতে পাঠযোগ্য সংখ্যা পাওয়া যায়নি। আলো ও ফোকাস ঠিক রেখে বা 'ডকুমেন্ট স্পষ্ট' চালু করে আবার চেষ্টা করুন।"
            )
        }

        // Refine ambiguous 2-column mark role assignment in AUTO mode (CONT vs WR vs GRAND)
        val refinedMarkColumns = refineAutoMarkKeysIfNeeded(
            rawRows = rawRows,
            roles = roles,
            cls = cls,
            subKey = subKey,
            importScope = importScope,
            layoutPreset = layoutPreset
        )

        val remappedRows = if (refinedMarkColumns != null) {
            rawRows.map { row ->
                val updated = LinkedHashMap<String, CellReadResult>()
                val oldEntries = row.markReads.values.toList()
                refinedMarkColumns.forEachIndexed { i, newKey ->
                    if (i < oldEntries.size) updated[newKey] = oldEntries[i]
                }
                row.copy(markReads = updated)
            }
        } else {
            rawRows
        }

        val resolvedScope = resolveOutputScope(
            requestedScope = importScope,
            mappedFieldKeys = (refinedMarkColumns ?: markColumns.map { it.second }).toSet(),
            hasWritten = activeSub.written > 0
        )

        val finalRows = ArrayList<ScannedStudentRow>(remappedRows.size)
        var prevRollInt: Int? = null
        val rosterByRoll = existingStudents.filter { it.cls == cls }.associateBy { it.roll }

        for (raw in remappedRows) {
            val rowResult = buildDisambiguatedRow(
                raw = raw,
                prevRoll = prevRollInt,
                cls = cls,
                subKey = subKey,
                scope = resolvedScope,
                rosterByRoll = rosterByRoll
            )
            rowResult.rollText.toIntOrNull()?.let { prevRollInt = it }
            finalRows.add(rowResult)
        }

        val elapsedMs = System.currentTimeMillis() - startMs
        val warnings = ArrayList<String>()
        if (abs(grid.deskewDegrees) >= 0.75f) {
            warnings.add("ছবিটি সামান্য বাঁকা ছিল (${String.format("%.1f", abs(grid.deskewDegrees))}° সোজা করা হয়েছে)।")
        }
        if (skippedNameColIdx != null) {
            warnings.add("কলাম ${bn((skippedNameColIdx + 1).toString())} (শিক্ষার্থীর নাম) বাদ দিয়ে রোল অনুযায়ী অ্যাপের তালিকা থেকে নাম মেলানো হয়েছে।")
        } else {
            warnings.add("নামের কলাম ছাড়া টেবিল — রোল ও নম্বরগুলো পড়ে অ্যাপের তালিকা থেকে নাম মেলানো হয়েছে।")
        }
        val lowConfRows = finalRows.count { it.aiIssues.isNotEmpty() }
        if (lowConfRows > 0) {
            warnings.add("${bn(lowConfRows.toString())}টি সারিতে কিছু সংখ্যা কম নিশ্চিত — লাল চিহ্নিত ঘরগুলো দেখে নিন।")
        }

        val scriptLabel = when (effectiveScript) {
            OfflineDigitScript.BENGALI -> "বাংলা ০–৯"
            OfflineDigitScript.ENGLISH -> "English 0-9"
            OfflineDigitScript.AUTO -> "স্বয়ংক্রিয়"
        }
        val summary = "অফলাইন টেবিল স্ক্যান ($scriptLabel · ${bn(finalRows.size.toString())} সারি · ${bn(elapsedMs.toString())} ms)"

        val finalRoles = if (refinedMarkColumns != null) {
            var mIdx = 0
            roles.map { role ->
                if (role.toMarkFieldKey() != null && mIdx < refinedMarkColumns.size) {
                    val fk = refinedMarkColumns[mIdx++]
                    when (fk) {
                        "TB" -> ColumnRole.FIELD_TB
                        "ACT" -> ColumnRole.FIELD_ACT
                        "LANG" -> ColumnRole.FIELD_LANG
                        "CT" -> ColumnRole.FIELD_CT
                        "CONT" -> ColumnRole.FIELD_CONT
                        "WR" -> ColumnRole.FIELD_WR
                        "TOTAL" -> ColumnRole.FIELD_TOTAL
                        "GRAND" -> ColumnRole.FIELD_GRAND
                        else -> role
                    }
                } else role
            }
        } else roles

        return OfflineScanOutput(
            sheet = ExtractedClassSheet(
                cls = cls,
                term = term,
                subKey = subKey,
                summary = summary,
                warnings = warnings,
                rows = finalRows,
                isSample = false,
                scope = resolvedScope
            ),
            diagnostics = OfflineScanDiagnostics(
                detectedScript = effectiveScript,
                rowCount = finalRows.size,
                headerRowsSkipped = grid.headerRowsSkipped,
                columnCount = grid.colBounds.size,
                skippedNameColumnIndex = skippedNameColIdx,
                columnRoles = finalRoles,
                processingMs = elapsedMs,
                usedDeskewDegrees = grid.deskewDegrees
            )
        )
    }

    private data class RawScannedRow(
        val rowNum: Int,
        val rollRead: CellReadResult,
        val markReads: Map<String, CellReadResult>
    )

    private fun detectDominantScript(
        grid: TableGridResult,
        recognizer: DualScriptDigitRecognizer,
        roles: List<ColumnRole>
    ): OfflineDigitScript {
        var enScoreSum = 0.0
        var bnScoreSum = 0.0
        var count = 0

        val sampleRows = grid.dataRowBounds.take(6)
        for (yr in sampleRows) {
            for (cIdx in grid.colBounds.indices) {
                val role = roles.getOrNull(cIdx)
                if (role == ColumnRole.NAME_SKIP || role == ColumnRole.SERIAL_SKIP || role == ColumnRole.EXTRA_SKIP) {
                    continue
                }
                val xr = grid.colBounds[cIdx]
                val enRes = CellDigitSegmenter.readNumericCell(
                    grid.image, xr.first, yr.first, xr.last, yr.last, recognizer, OfflineDigitScript.ENGLISH
                )
                val bnRes = CellDigitSegmenter.readNumericCell(
                    grid.image, xr.first, yr.first, xr.last, yr.last, recognizer, OfflineDigitScript.BENGALI
                )
                if (!enRes.isBlank && !bnRes.isBlank) {
                    enScoreSum += enRes.confidence.toDouble()
                    bnScoreSum += bnRes.confidence.toDouble()
                    count++
                }
                if (count >= 18) break
            }
            if (count >= 18) break
        }
        return if (bnScoreSum >= enScoreSum) OfflineDigitScript.BENGALI else OfflineDigitScript.ENGLISH
    }

    private fun assignColumnRoles(
        grid: TableGridResult,
        cls: Int,
        subKey: String,
        importScope: ClassSheetImportScope,
        layoutPreset: OfflineTableLayoutPreset
    ): Pair<List<ColumnRole>, Int?> {
        val nCols = grid.colBounds.size
        val widths = grid.colBounds.map { it.last - it.first + 1 }
        val roles = MutableList(nCols) { ColumnRole.EXTRA_SKIP }

        val medianCompPerCol = IntArray(nCols)
        val avgSpanPerCol = FloatArray(nCols)
        val medianAspectPerCol = FloatArray(nCols)
        val sampleRows = grid.dataRowBounds.take(8)
        for (c in 0 until nCols) {
            val xr = grid.colBounds[c]
            val comps = ArrayList<Int>(sampleRows.size)
            val aspects = ArrayList<Float>(sampleRows.size)
            var spanSum = 0f
            var spanCnt = 0
            for (yr in sampleRows) {
                val st = CellDigitSegmenter.inspectCellStats(grid.image, xr.first, yr.first, xr.last, yr.last)
                if (!st.isEmpty) {
                    comps.add(st.componentCount)
                    aspects.add(st.maxComponentAspect)
                    spanSum += st.inkSpanFraction * widths[c]
                    spanCnt++
                }
            }
            medianCompPerCol[c] = if (comps.isEmpty()) 0 else comps.sorted()[comps.size / 2]
            medianAspectPerCol[c] = if (aspects.isEmpty()) 0f else aspects.sorted()[aspects.size / 2]
            avgSpanPerCol[c] = if (spanCnt == 0) 0f else spanSum / spanCnt
        }

        // Detect Name column (usually col 1, or col 2 if SL + Roll precede Name),
        // while avoiding false positives on Roll|Cont|Wr or Roll|Cont|Wr|Total tables without Name.
        var nameColIdx: Int? = null
        if (nCols >= 3) {
            val candidateRange = 1..min(2, nCols - 2)
            var bestScore = -1f
            var bestC = -1
            for (c in candidateRange) {
                val rightMarkWidths = widths.drop(c + 1).sorted()
                val rightMarkMedian = rightMarkWidths[rightMarkWidths.size / 2].coerceAtLeast(1)
                val widthVsMarks = widths[c].toFloat() / rightMarkMedian.toFloat()
                val compBonus = if (medianCompPerCol[c] >= 4 || medianAspectPerCol[c] >= 1.55f) 0.85f else 0f
                val spanBonus = if (avgSpanPerCol[c] > widths[0] * 1.4f && widthVsMarks >= 1.32f) 0.35f else 0f
                val totalScore = widthVsMarks + compBonus + spanBonus
                if (totalScore > bestScore) {
                    bestScore = totalScore
                    bestC = c
                }
            }
            if (bestC >= 0) {
                val rightMarkWidths = widths.drop(bestC + 1).sorted()
                val rightMarkMedian = rightMarkWidths[rightMarkWidths.size / 2].coerceAtLeast(1)
                val widthVsMarks = widths[bestC].toFloat() / rightMarkMedian.toFloat()
                val isWordy = medianCompPerCol[bestC] >= 4 || medianAspectPerCol[bestC] >= 1.55f
                val isSignificantlyWiderThanMarks = widthVsMarks >= 1.42f &&
                    (medianCompPerCol[bestC] >= 3 || medianAspectPerCol[bestC] >= 1.25f || widthVsMarks >= 1.72f)
                if (isWordy || isSignificantlyWiderThanMarks) {
                    nameColIdx = bestC
                }
            }
        }

        val markStartIdx: Int
        if (nameColIdx == 1) {
            roles[0] = ColumnRole.ROLL
            roles[1] = ColumnRole.NAME_SKIP
            markStartIdx = 2
        } else if (nameColIdx == 2) {
            roles[0] = ColumnRole.SERIAL_SKIP
            roles[1] = ColumnRole.ROLL
            roles[2] = ColumnRole.NAME_SKIP
            markStartIdx = 3
        } else {
            roles[0] = ColumnRole.ROLL
            markStartIdx = 1
        }

        val markColCount = (nCols - markStartIdx).coerceAtLeast(0)
        val activeSub = subjectsFor(cls, RELIGIONS.first()).find { it.key == subKey }
        val hasWritten = (activeSub?.written ?: 0) > 0
        val isArt = activeSub?.comps?.map { it.key } == listOf("TOTAL") && !hasWritten

        val markRoles: List<ColumnRole> = when (layoutPreset) {
            OfflineTableLayoutPreset.ROLL_NAME_SINGLE -> when {
                isArt -> listOf(ColumnRole.FIELD_TOTAL)
                importScope == ClassSheetImportScope.WR_ONLY && hasWritten -> listOf(ColumnRole.FIELD_WR)
                else -> listOf(ColumnRole.FIELD_CONT)
            }
            OfflineTableLayoutPreset.ROLL_NAME_CONT_WR_TOTAL -> when {
                isArt -> listOf(ColumnRole.FIELD_TOTAL)
                !hasWritten || importScope == ClassSheetImportScope.CONT_ONLY ->
                    listOf(ColumnRole.FIELD_CONT, ColumnRole.FIELD_GRAND)
                importScope == ClassSheetImportScope.WR_ONLY ->
                    listOf(ColumnRole.FIELD_WR)
                markColCount <= 1 -> listOf(ColumnRole.FIELD_CONT)
                markColCount == 2 -> listOf(ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR)
                else -> listOf(ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR, ColumnRole.FIELD_GRAND)
            }
            OfflineTableLayoutPreset.ROLL_NAME_FULL_BREAKDOWN -> when {
                isArt -> listOf(ColumnRole.FIELD_TOTAL)
                !hasWritten || importScope == ClassSheetImportScope.CONT_ONLY -> listOf(
                    ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT,
                    ColumnRole.FIELD_CONT
                )
                markColCount == 5 -> listOf(
                    ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT,
                    ColumnRole.FIELD_WR
                )
                markColCount == 6 -> listOf(
                    ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT,
                    ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR
                )
                else -> listOf(
                    ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT,
                    ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR, ColumnRole.FIELD_GRAND
                )
            }
            OfflineTableLayoutPreset.AUTO -> when (markColCount) {
                0 -> emptyList()
                1 -> when {
                    isArt -> listOf(ColumnRole.FIELD_TOTAL)
                    importScope == ClassSheetImportScope.WR_ONLY && hasWritten -> listOf(ColumnRole.FIELD_WR)
                    importScope == ClassSheetImportScope.CONT_ONLY || !hasWritten -> listOf(ColumnRole.FIELD_CONT)
                    else -> listOf(ColumnRole.FIELD_WR)
                }
                2 -> when {
                    isArt -> listOf(ColumnRole.FIELD_TOTAL, ColumnRole.EXTRA_SKIP)
                    importScope == ClassSheetImportScope.WR_ONLY && hasWritten -> listOf(ColumnRole.FIELD_WR, ColumnRole.EXTRA_SKIP)
                    importScope == ClassSheetImportScope.CONT_ONLY || !hasWritten -> listOf(ColumnRole.FIELD_CONT, ColumnRole.FIELD_GRAND)
                    else -> listOf(ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR)
                }
                3 -> when {
                    !hasWritten || importScope == ClassSheetImportScope.CONT_ONLY ->
                        listOf(ColumnRole.FIELD_CONT, ColumnRole.FIELD_GRAND, ColumnRole.EXTRA_SKIP)
                    else -> listOf(ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR, ColumnRole.FIELD_GRAND)
                }
                4 -> listOf(ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT)
                5 -> if (!hasWritten || importScope == ClassSheetImportScope.CONT_ONLY) {
                    listOf(ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT, ColumnRole.FIELD_CONT)
                } else {
                    listOf(ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT, ColumnRole.FIELD_WR)
                }
                6 -> listOf(
                    ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT,
                    ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR
                )
                else -> listOf(
                    ColumnRole.FIELD_TB, ColumnRole.FIELD_ACT, ColumnRole.FIELD_LANG, ColumnRole.FIELD_CT,
                    ColumnRole.FIELD_CONT, ColumnRole.FIELD_WR, ColumnRole.FIELD_GRAND
                )
            }
        }

        for (i in 0 until markColCount) {
            roles[markStartIdx + i] = markRoles.getOrElse(i) { ColumnRole.EXTRA_SKIP }
        }
        return roles to nameColIdx
    }

    /**
     * Cross-row arithmetic & bound plausibility check in AUTO layout mode:
     * e.g., distinguishes `[CONT, WR]` vs `[CONT, GRAND]` or `[4 comps + CONT]` vs `[4 comps + WR]`.
     */
    private fun refineAutoMarkKeysIfNeeded(
        rawRows: List<RawScannedRow>,
        roles: List<ColumnRole>,
        cls: Int,
        subKey: String,
        importScope: ClassSheetImportScope,
        layoutPreset: OfflineTableLayoutPreset
    ): List<String>? {
        if (layoutPreset != OfflineTableLayoutPreset.AUTO) return null
        val activeSub = subjectsFor(cls, RELIGIONS.first()).find { it.key == subKey } ?: return null
        if (activeSub.written <= 0 || importScope == ClassSheetImportScope.CONT_ONLY || importScope == ClassSheetImportScope.WR_ONLY) {
            return null
        }

        val currentKeys = roles.mapNotNull { it.toMarkFieldKey() }
        val maxCont = activeSub.comps.sumOf { it.max }

        // Case 1: 1 mark column in AUTO mode -> decide CONT vs WR by checking values against maxCont
        if (currentKeys.size == 1) {
            val key = currentKeys[0]
            if (key == "WR" || key == "CONT") {
                val vals = rawRows.mapNotNull { it.markReads[key]?.text?.toIntOrNull() }
                if (vals.isNotEmpty() && vals.all { it <= maxCont } && activeSub.written > maxCont) {
                    val anyLikelyWritten = vals.any { it > maxCont }
                    if (!anyLikelyWritten && vals.count { it <= maxCont } == vals.size && cls >= 3) {
                        // Class 3..5 maxCont is 30, written is 70; if every mark <= 30 it's likely CONT unless very low scores
                        if (vals.average() <= maxCont * 0.92 && vals.max() <= maxCont) {
                            return listOf("CONT")
                        }
                    }
                }
            }
        }

        // Case 2: 5 mark columns (`TB, ACT, LANG, CT, X`) -> check if X == TB+ACT+LANG+CT across rows!
        if (currentKeys == listOf("TB", "ACT", "LANG", "CT", "WR")) {
            var sumMatches = 0
            var comparable = 0
            for (row in rawRows) {
                val tb = row.markReads["TB"]?.text?.toIntOrNull() ?: continue
                val act = row.markReads["ACT"]?.text?.toIntOrNull() ?: continue
                val lang = row.markReads["LANG"]?.text?.toIntOrNull() ?: continue
                val ct = row.markReads["CT"]?.text?.toIntOrNull() ?: continue
                val fifth = row.markReads["WR"]?.text?.toIntOrNull() ?: continue
                comparable++
                if (tb + act + lang + ct == fifth) sumMatches++
            }
            if (comparable >= 2 && sumMatches * 2 >= comparable) {
                return listOf("TB", "ACT", "LANG", "CT", "CONT")
            }
        }
        return null
    }

    private fun resolveOutputScope(
        requestedScope: ClassSheetImportScope,
        mappedFieldKeys: Set<String>,
        hasWritten: Boolean
    ): ClassSheetImportScope {
        if (requestedScope == ClassSheetImportScope.CONT_ONLY) return ClassSheetImportScope.CONT_ONLY
        if (requestedScope == ClassSheetImportScope.WR_ONLY && hasWritten) return ClassSheetImportScope.WR_ONLY
        val hasContCols = mappedFieldKeys.any { it in setOf("TB", "ACT", "LANG", "CT", "CONT", "TOTAL") }
        val hasWrCol = "WR" in mappedFieldKeys
        return when {
            hasWrCol && !hasContCols && hasWritten -> ClassSheetImportScope.WR_ONLY
            hasContCols && !hasWrCol -> ClassSheetImportScope.CONT_ONLY
            else -> ClassSheetImportScope.BOTH
        }
    }

    private fun buildDisambiguatedRow(
        raw: RawScannedRow,
        prevRoll: Int?,
        cls: Int,
        subKey: String,
        scope: ClassSheetImportScope,
        rosterByRoll: Map<Int, Student>
    ): ScannedStudentRow {
        val issues = LinkedHashMap<String, String>()
        val deduced = LinkedHashMap<String, String>()

        // 1. Roll resolution with sequence plausibility
        var rollText = raw.rollRead.text
        if (raw.rollRead.isBlank) {
            if (prevRoll != null) {
                rollText = (prevRoll + 1).toString()
                deduced["roll"] = "ক্রম অনুযায়ী অনুমিত"
                issues["roll"] = "রোল ঘর খালি — ক্রম অনুযায়ী ${bn(rollText)} ধরা হয়েছে"
            } else {
                issues["roll"] = "রোল পড়া যায়নি"
            }
        } else if (prevRoll != null) {
            val expected = (prevRoll + 1).toString()
            val curInt = rollText.toIntOrNull()
            if ((curInt == null || curInt != prevRoll + 1) && raw.rollRead.confidence < 0.90f) {
                val seqCand = raw.rollRead.candidates.find { it.text == expected && it.score >= 0.12f }
                if (seqCand != null) {
                    rollText = seqCand.text
                    deduced["roll"] = "ক্রম (${bn(expected)}) মিল"
                    if (raw.rollRead.confidence < CONFIDENCE_SURE_THRESHOLD) {
                        issues["roll"] = "অফলাইন রিডিং নিশ্চিত নয় — যাচাই করুন"
                    }
                }
            }
            if (raw.rollRead.confidence < CONFIDENCE_SURE_THRESHOLD && "roll" !in issues) {
                issues["roll"] = "অফলাইন রিডিং নিশ্চিত নয় (${(raw.rollRead.confidence * 100).roundToInt()}%) — যাচাই করুন"
            }
        } else if (raw.rollRead.confidence < CONFIDENCE_SURE_THRESHOLD) {
            issues["roll"] = "অফলাইন রিডিং নিশ্চিত নয় (${(raw.rollRead.confidence * 100).roundToInt()}%) — যাচাই করুন"
        }

        // 2. Numeric mark fields + candidate arithmetic disambiguation
        val chosenText = LinkedHashMap<String, String>()
        raw.markReads.forEach { (k, res) ->
            if (!res.isBlank) chosenText[k] = res.text
        }

        val activeSub = subjectsFor(cls, RELIGIONS.first()).find { it.key == subKey }
        val maxCont = activeSub?.comps?.sumOf { it.max } ?: 30
        val maxWr = activeSub?.written ?: 0
        val maxGrand = activeSub?.max ?: 100
        val compMaxMap = activeSub?.comps?.associate { it.key to it.max } ?: emptyMap()

        // Enforce domain upper-bound candidate preference (e.g., if top-1 is 92 for max-30 CONT, check 32/22 top-2 candidate!)
        for ((k, res) in raw.markReads) {
            if (res.isBlank) continue
            val fieldMax = when (k) {
                "CONT" -> maxCont
                "WR" -> maxWr
                "GRAND", "TOTAL" -> maxGrand
                else -> compMaxMap[k] ?: maxGrand
            }
            val topInt = res.text.toIntOrNull()
            if (topInt != null && topInt > fieldMax) {
                val validCand = res.candidates.firstOrNull { c ->
                    val v = c.text.toIntOrNull()
                    v != null && v in 0..fieldMax && c.score >= 0.06f
                }
                if (validCand != null) {
                    chosenText[k] = validCand.text
                    issues[k] = "সীমার (${bn(fieldMax.toString())}) সাথে মিলিয়ে ${bn(validCand.text)} ধরা হয়েছে — যাচাই করুন"
                }
            }
        }

        // Arithmetic disambiguation: if CONT + WR != GRAND (or 4-part sum != CONT), test single top-2 candidate swap
        tryArithmeticCandidateDisambiguation(
            rawReads = raw.markReads,
            chosenText = chosenText,
            issues = issues,
            compKeys = activeSub?.comps?.map { it.key } ?: emptyList(),
            maxCont = maxCont,
            maxWr = maxWr,
            maxGrand = maxGrand
        )

        // Flag any remaining low-confidence cells
        for ((k, res) in raw.markReads) {
            if (!res.isBlank && res.confidence < CONFIDENCE_SURE_THRESHOLD && k !in issues) {
                issues[k] = "অফলাইন রিডিং নিশ্চিত নয় (${(res.confidence * 100).roundToInt()}%) — যাচাই করুন"
            }
        }

        val compMarks = LinkedHashMap<String, String>()
        for (k in listOf("TB", "ACT", "LANG", "CT", "WR", "TOTAL")) {
            chosenText[k]?.let { compMarks[k] = it }
        }

        val rollInt = rollText.toIntOrNull()
        val matchedStudentName = rollInt?.let { rosterByRoll[it]?.name.orEmpty() }.orEmpty()

        val candidateRow = ScannedStudentRow(
            rollText = rollText,
            name = matchedStudentName,
            marksText = compMarks,
            sheetContinuousTotal = chosenText["CONT"],
            sheetGrandTotal = chosenText["GRAND"],
            rawNotes = "অফলাইন সারি ${bn(raw.rowNum.toString())}",
            aiIssues = issues,
            deducedFields = deduced
        )
        return ScanConstraintSolver.reconcileInterdependencies(candidateRow, cls, subKey, scope)
    }

    private fun tryArithmeticCandidateDisambiguation(
        rawReads: Map<String, CellReadResult>,
        chosenText: MutableMap<String, String>,
        issues: MutableMap<String, String>,
        compKeys: List<String>,
        maxCont: Int,
        maxWr: Int,
        maxGrand: Int
    ) {
        // A) Check 4 components -> CONT
        val hasAllComps = compKeys.size >= 2 && compKeys.all { !chosenText[it].isNullOrBlank() }
        val contVal = chosenText["CONT"]?.toIntOrNull()
        if (hasAllComps && contVal != null) {
            val compSum = compKeys.sumOf { chosenText[it]?.toIntOrNull() ?: 0 }
            if (compSum != contVal) {
                val keysToCheck = compKeys + "CONT"
                var bestFix: Pair<String, String>? = null
                var bestScore = -1f
                for (fk in keysToCheck) {
                    val cands = rawReads[fk]?.candidates ?: continue
                    val orig = chosenText[fk]
                    for (cand in cands) {
                        if (cand.text == orig || cand.score < 0.08f) continue
                        val cv = cand.text.toIntOrNull() ?: continue
                        val testMap = chosenText.toMutableMap().apply { put(fk, cand.text) }
                        val s = compKeys.sumOf { testMap[it]?.toIntOrNull() ?: 0 }
                        val c = testMap["CONT"]?.toIntOrNull() ?: -1
                        if (s == c && c in 0..maxCont && cand.score > bestScore) {
                            bestScore = cand.score
                            bestFix = fk to cand.text
                        }
                    }
                }
                if (bestFix != null) {
                    chosenText[bestFix.first] = bestFix.second
                    issues[bestFix.first] = "যোগফলের সাথে মিলিয়ে ${bn(bestFix.second)} ধরা হয়েছে — যাচাই করুন"
                }
            }
        }

        // B) Check CONT + WR -> GRAND
        val cont = chosenText["CONT"]?.toIntOrNull()
        val wr = chosenText["WR"]?.toIntOrNull()
        val grand = chosenText["GRAND"]?.toIntOrNull()
        if (cont != null && wr != null && grand != null && cont + wr != grand) {
            var bestFix: Pair<String, String>? = null
            var bestScore = -1f
            for (fk in listOf("CONT", "WR", "GRAND")) {
                val cands = rawReads[fk]?.candidates ?: continue
                val orig = chosenText[fk]
                for (cand in cands) {
                    if (cand.text == orig || cand.score < 0.08f) continue
                    val cv = cand.text.toIntOrNull() ?: continue
                    val okBound = when (fk) {
                        "CONT" -> cv in 0..maxCont
                        "WR" -> cv in 0..maxWr
                        else -> cv in 0..maxGrand
                    }
                    if (!okBound) continue
                    val tCont = if (fk == "CONT") cv else cont
                    val tWr = if (fk == "WR") cv else wr
                    val tGrand = if (fk == "GRAND") cv else grand
                    if (tCont + tWr == tGrand && cand.score > bestScore) {
                        bestScore = cand.score
                        bestFix = fk to cand.text
                    }
                }
            }
            if (bestFix != null) {
                chosenText[bestFix.first] = bestFix.second
                issues[bestFix.first] = "মোটের সাথে মিলিয়ে ${bn(bestFix.second)} ধরা হয়েছে — যাচাই করুন"
            }
        }
    }
}
