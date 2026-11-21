package com.kgps.result.ai.offline

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class CellStats(
    val isEmpty: Boolean,
    val componentCount: Int,
    val inkSpanFraction: Float,
    val pixelCount: Int,
    val maxComponentAspect: Float = 0f
)

data class DigitBlob(
    val x0: Int,
    val y0: Int,
    val x1: Int,
    val y1: Int,
    val pixelCount: Int,
    val mask: BooleanArray, // size = (x1 - x0 + 1) * (y1 - y0 + 1)
    val blobWidth: Int = x1 - x0 + 1,
    val blobHeight: Int = y1 - y0 + 1
)

object CellDigitSegmenter {

    fun inspectCellStats(
        img: GrayImage,
        rawX0: Int,
        rawY0: Int,
        rawX1: Int,
        rawY1: Int
    ): CellStats {
        val (cx0, cy0, cx1, cy1) = trimCellBorders(img, rawX0, rawY0, rawX1, rawY1)
        val cw = cx1 - cx0
        val ch = cy1 - cy0
        if (cw < 6 || ch < 6) return CellStats(true, 0, 0f, 0)

        val blobs = extractRawComponents(img, cx0, cy0, cx1, cy1, minArea = max(5, (cw * ch) / 300))
        if (blobs.isEmpty()) return CellStats(true, 0, 0f, 0)

        var minBx = Int.MAX_VALUE
        var maxBx = Int.MIN_VALUE
        var pxSum = 0
        var maxAsp = 0f
        for (b in blobs) {
            if (b.x0 < minBx) minBx = b.x0
            if (b.x1 > maxBx) maxBx = b.x1
            pxSum += b.pixelCount
            val asp = b.blobWidth.toFloat() / max(1, b.blobHeight)
            if (asp > maxAsp) maxAsp = asp
        }
        val spanFrac = (maxBx - minBx + 1).toFloat() / cw.toFloat()
        return CellStats(
            isEmpty = false,
            componentCount = blobs.size,
            inkSpanFraction = spanFrac.coerceIn(0f, 1f),
            pixelCount = pxSum,
            maxComponentAspect = maxAsp
        )
    }

    fun readNumericCell(
        img: GrayImage,
        rawX0: Int,
        rawY0: Int,
        rawX1: Int,
        rawY1: Int,
        recognizer: DualScriptDigitRecognizer,
        script: OfflineDigitScript
    ): CellReadResult {
        val (cx0, cy0, cx1, cy1) = trimCellBorders(img, rawX0, rawY0, rawX1, rawY1)
        val cw = cx1 - cx0
        val ch = cy1 - cy0
        if (cw < 6 || ch < 6) {
            return CellReadResult("", 1f, 0, emptyList(), isBlank = true)
        }

        val minArea = max(6, (cw * ch) / 260)
        val rawBlobs = extractRawComponents(img, cx0, cy0, cx1, cy1, minArea = minArea)
        if (rawBlobs.isEmpty()) {
            return CellReadResult("", 1f, 0, emptyList(), isBlank = true)
        }

        val merged = mergeVerticalFragments(rawBlobs, cellHeight = ch)
        val digitBlobs = splitTouchingDigits(merged, cellHeight = ch)
            .sortedBy { (it.x0 + it.x1) * 0.5f }

        if (digitBlobs.isEmpty()) {
            return CellReadResult("", 1f, 0, emptyList(), isBlank = true)
        }

        // If > 3 blobs appear (noise or slash/punctuation), keep the tallest/heaviest up to 3
        val selectedBlobs = if (digitBlobs.size > 3) {
            val medianH = digitBlobs.map { it.blobHeight }.sorted()[digitBlobs.size / 2]
            digitBlobs
                .filter { it.blobHeight >= (medianH * 0.55f).toInt() && it.pixelCount >= minArea }
                .take(3)
                .ifEmpty { digitBlobs.sortedByDescending { it.pixelCount }.take(3).sortedBy { it.x0 } }
        } else {
            digitBlobs
        }

        val preds = selectedBlobs.map { blob ->
            val patch = extractSoftInkPatch(img, blob)
            val canonical = DigitCnnClassifier.canonicalNormalize(patch, blob.blobWidth, blob.blobHeight)
            recognizer.predict(canonical, script)
        }

        val topText = preds.joinToString("") { it.digit.toString() }
        var minConf = 1f
        var topScore = 1f
        for (p in preds) {
            if (p.confidence < minConf) minConf = p.confidence
            topScore *= p.confidence
        }

        val candidates = buildCandidates(preds, topText, topScore)

        return CellReadResult(
            text = topText,
            confidence = minConf,
            digitCount = preds.size,
            candidates = candidates,
            isBlank = false,
            hadAmbiguousSplit = digitBlobs.size != merged.size
        )
    }

    private fun buildCandidates(
        preds: List<DigitPrediction>,
        topText: String,
        topScore: Float
    ): List<CellNumberCandidate> {
        val list = ArrayList<CellNumberCandidate>(4)
        list.add(CellNumberCandidate(topText, topScore))

        // Single-digit swap candidates sorted by plausibility
        val alts = ArrayList<CellNumberCandidate>()
        for (idx in preds.indices) {
            val p = preds[idx]
            if (p.secondConfidence >= 0.08f && p.secondDigit != p.digit) {
                val chars = preds.mapIndexed { j, dp ->
                    if (j == idx) ('0' + dp.secondDigit) else ('0' + dp.digit)
                }.joinToString("")
                var score = 1f
                for (j in preds.indices) {
                    score *= if (j == idx) preds[j].secondConfidence else preds[j].confidence
                }
                alts.add(CellNumberCandidate(chars, score))
            }
        }
        alts.sortedByDescending { it.score }
            .distinctBy { it.text }
            .take(3)
            .forEach { list.add(it) }
        return list
    }

    private data class RectI(val x0: Int, val y0: Int, val x1: Int, val y1: Int)

    /**
     * Insets inside cell borders and erases any horizontal/vertical grid-line remnants touching edges.
     */
    private fun trimCellBorders(
        img: GrayImage,
        rawX0: Int,
        rawY0: Int,
        rawX1: Int,
        rawY1: Int
    ): RectI {
        val w = (rawX1 - rawX0).coerceAtLeast(1)
        val h = (rawY1 - rawY0).coerceAtLeast(1)
        var x0 = (rawX0 + max(2, w / 28)).coerceIn(0, img.width - 1)
        var x1 = (rawX1 - max(2, w / 28)).coerceIn(x0 + 1, img.width)
        var y0 = (rawY0 + max(2, h / 24)).coerceIn(0, img.height - 1)
        var y1 = (rawY1 - max(2, h / 24)).coerceIn(y0 + 1, img.height)

        // Strip edge rows/cols that look like table rule fragments (>68% solid run across edge)
        val edgeLimitY = min(6, (y1 - y0) / 6)
        repeat(edgeLimitY) {
            if (y0 < y1 - 4 && rowFillFraction(img, y0, x0, x1) > 0.68f) y0++
            if (y1 - 1 > y0 + 4 && rowFillFraction(img, y1 - 1, x0, x1) > 0.68f) y1--
        }
        val edgeLimitX = min(6, (x1 - x0) / 6)
        repeat(edgeLimitX) {
            if (x0 < x1 - 4 && colFillFraction(img, x0, y0, y1) > 0.68f) x0++
            if (x1 - 1 > x0 + 4 && colFillFraction(img, x1 - 1, y0, y1) > 0.68f) x1--
        }
        return RectI(x0, y0, x1, y1)
    }

    private fun rowFillFraction(img: GrayImage, y: Int, x0: Int, x1: Int): Float {
        var c = 0
        val off = y * img.width
        for (x in x0 until x1) if (img.binary[off + x]) c++
        return c.toFloat() / max(1, x1 - x0)
    }

    private fun colFillFraction(img: GrayImage, x: Int, y0: Int, y1: Int): Float {
        var c = 0
        for (y in y0 until y1) if (img.binary[y * img.width + x]) c++
        return c.toFloat() / max(1, y1 - y0)
    }

    private fun extractRawComponents(
        img: GrayImage,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        minArea: Int
    ): List<DigitBlob> {
        val cw = x1 - x0
        val ch = y1 - y0
        val visited = BooleanArray(cw * ch)
        val queue = IntArray(cw * ch)
        val out = ArrayList<DigitBlob>()

        for (ly in 0 until ch) {
            val gy = y0 + ly
            val imgOff = gy * img.width
            for (lx in 0 until cw) {
                val lIdx = ly * cw + lx
                if (visited[lIdx]) continue
                if (!img.binary[imgOff + x0 + lx]) continue

                var head = 0
                var tail = 0
                queue[tail++] = lIdx
                visited[lIdx] = true

                var bx0 = lx
                var bx1 = lx
                var by0 = ly
                var by1 = ly
                val coords = ArrayList<Int>(64)

                while (head < tail) {
                    val cur = queue[head++]
                    val cy = cur / cw
                    val cx = cur - cy * cw
                    coords.add(cur)
                    if (cx < bx0) bx0 = cx
                    if (cx > bx1) bx1 = cx
                    if (cy < by0) by0 = cy
                    if (cy > by1) by1 = cy

                    for (dy in -1..1) {
                        val ny = cy + dy
                        if (ny !in 0 until ch) continue
                        val nImgOff = (y0 + ny) * img.width
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = cx + dx
                            if (nx !in 0 until cw) continue
                            val nIdx = ny * cw + nx
                            if (!visited[nIdx] && img.binary[nImgOff + x0 + nx]) {
                                visited[nIdx] = true
                                queue[tail++] = nIdx
                            }
                        }
                    }
                }

                val bw = bx1 - bx0 + 1
                val bh = by1 - by0 + 1
                val area = coords.size
                if (area < minArea) continue

                // Reject leftover horizontal/vertical table line segments
                val isThinHBar = bw >= (cw * 0.78f).toInt() && bh <= max(3, ch / 12)
                val isThinVBar = bh >= (ch * 0.85f).toInt() && bw <= max(3, cw / 18) && area > bh * 2 && bh > bw * 5
                if (isThinHBar || isThinVBar) continue

                val mask = BooleanArray(bw * bh)
                for (c in coords) {
                    val cy = c / cw
                    val cx = c - cy * cw
                    mask[(cy - by0) * bw + (cx - bx0)] = true
                }
                out.add(
                    DigitBlob(
                        x0 = x0 + bx0,
                        y0 = y0 + by0,
                        x1 = x0 + bx1,
                        y1 = y0 + by1,
                        pixelCount = area,
                        mask = mask
                    )
                )
            }
        }
        return out
    }

    private fun mergeVerticalFragments(blobs: List<DigitBlob>, cellHeight: Int): List<DigitBlob> {
        if (blobs.size <= 1) return blobs
        val sorted = blobs.sortedByDescending { it.pixelCount }.toMutableList()
        val maxGapY = max(4, cellHeight / 5)
        var changed = true
        while (changed) {
            changed = false
            outer@ for (i in sorted.indices) {
                for (j in i + 1 until sorted.size) {
                    val a = sorted[i]
                    val b = sorted[j]
                    val overlapX = min(a.x1, b.x1) - max(a.x0, b.x0) + 1
                    val minW = min(a.blobWidth, b.blobWidth).coerceAtLeast(1)
                    val gapY = max(0, max(a.y0, b.y0) - min(a.y1, b.y1))
                    if (overlapX.toFloat() / minW >= 0.45f && gapY <= maxGapY) {
                        sorted[i] = unionBlobs(a, b)
                        sorted.removeAt(j)
                        changed = true
                        break@outer
                    }
                }
            }
        }
        return sorted
    }

    private fun unionBlobs(a: DigitBlob, b: DigitBlob): DigitBlob {
        val nx0 = min(a.x0, b.x0)
        val ny0 = min(a.y0, b.y0)
        val nx1 = max(a.x1, b.x1)
        val ny1 = max(a.y1, b.y1)
        val nw = nx1 - nx0 + 1
        val nh = ny1 - ny0 + 1
        val mask = BooleanArray(nw * nh)
        for (y in 0 until a.blobHeight) {
            for (x in 0 until a.blobWidth) {
                if (a.mask[y * a.blobWidth + x]) {
                    mask[(a.y0 - ny0 + y) * nw + (a.x0 - nx0 + x)] = true
                }
            }
        }
        for (y in 0 until b.blobHeight) {
            for (x in 0 until b.blobWidth) {
                if (b.mask[y * b.blobWidth + x]) {
                    mask[(b.y0 - ny0 + y) * nw + (b.x0 - nx0 + x)] = true
                }
            }
        }
        return DigitBlob(nx0, ny0, nx1, ny1, a.pixelCount + b.pixelCount, mask)
    }

    /**
     * Splits wide touching multi-digit blobs via minimum vertical ink projection valley.
     * Iterates up to 2 split rounds so 3-digit touching numbers (e.g. "100") split cleanly.
     */
    private fun splitTouchingDigits(blobs: List<DigitBlob>, cellHeight: Int): List<DigitBlob> {
        val out = ArrayList<DigitBlob>(blobs.size)
        for (initial in blobs) {
            val queue = ArrayDeque<Pair<DigitBlob, Int>>()
            queue.add(initial to 0)
            while (queue.isNotEmpty()) {
                val (blob, depth) = queue.removeFirst()
                val aspect = blob.blobWidth.toFloat() / max(1, blob.blobHeight)
                if (depth < 2 && aspect >= 1.18f && blob.blobWidth >= max(16, (cellHeight * 0.45f).toInt())) {
                    val colInk = IntArray(blob.blobWidth)
                    for (x in 0 until blob.blobWidth) {
                        var s = 0
                        for (y in 0 until blob.blobHeight) {
                            if (blob.mask[y * blob.blobWidth + x]) s++
                        }
                        colInk[x] = s
                    }
                    val startX = (blob.blobWidth * 0.24f).roundToInt()
                    val endX = (blob.blobWidth * 0.76f).roundToInt()
                    var bestX = -1
                    var minInk = Int.MAX_VALUE
                    for (x in startX..endX) {
                        if (colInk[x] < minInk) {
                            minInk = colInk[x]
                            bestX = x
                        }
                    }
                    val avgInk = colInk.average()
                    if (bestX > 2 && bestX < blob.blobWidth - 2 && minInk <= max(2.0, avgInk * 0.38)) {
                        val left = sliceBlob(blob, 0, bestX)
                        val right = sliceBlob(blob, bestX + 1, blob.blobWidth - 1)
                        if (left != null && right != null) {
                            queue.add(left to (depth + 1))
                            queue.add(right to (depth + 1))
                            continue
                        }
                    }
                }
                out.add(blob)
            }
        }
        return out
    }

    private fun sliceBlob(blob: DigitBlob, lx0: Int, lx1: Int): DigitBlob? {
        if (lx1 <= lx0) return null
        var minX = lx1
        var maxX = lx0
        var minY = blob.blobHeight - 1
        var maxY = 0
        var count = 0
        for (y in 0 until blob.blobHeight) {
            for (x in lx0..lx1) {
                if (blob.mask[y * blob.blobWidth + x]) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                    count++
                }
            }
        }
        if (count < 5 || maxX < minX || maxY < minY) return null
        val nw = maxX - minX + 1
        val nh = maxY - minY + 1
        val mask = BooleanArray(nw * nh)
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                mask[(y - minY) * nw + (x - minX)] = blob.mask[y * blob.blobWidth + x]
            }
        }
        return DigitBlob(
            x0 = blob.x0 + minX,
            y0 = blob.y0 + minY,
            x1 = blob.x0 + maxX,
            y1 = blob.y0 + maxY,
            pixelCount = count,
            mask = mask
        )
    }

    /**
     * Extracts soft normalized ink [0..1] inside the blob's tight bounding box,
     * preserving anti-aliased stroke edges while zeroing stray foreign pixels outside dilated mask.
     */
    private fun extractSoftInkPatch(img: GrayImage, blob: DigitBlob): FloatArray {
        val bw = blob.blobWidth
        val bh = blob.blobHeight
        val patch = FloatArray(bw * bh)
        for (ly in 0 until bh) {
            val gy = blob.y0 + ly
            val imgRow = gy * img.width
            val bRow = ly * bw
            for (lx in 0 until bw) {
                val onOrNeighbor = blob.mask[bRow + lx] ||
                    (lx > 0 && blob.mask[bRow + lx - 1]) ||
                    (lx + 1 < bw && blob.mask[bRow + lx + 1]) ||
                    (ly > 0 && blob.mask[(ly - 1) * bw + lx]) ||
                    (ly + 1 < bh && blob.mask[(ly + 1) * bw + lx])
                if (onOrNeighbor) {
                    val v = img.ink[imgRow + blob.x0 + lx]
                    patch[bRow + lx] = if (blob.mask[bRow + lx]) max(v, 0.45f) else v * 0.6f
                }
            }
        }
        return patch
    }
}
