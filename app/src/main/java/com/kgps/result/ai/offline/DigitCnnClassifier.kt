package com.kgps.result.ai.offline

import android.content.Context
import com.kgps.result.R
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.InputStream
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure-Kotlin 2-layer CNN digit classifier (`KGDC` float16 weight format)
 * trained on real handwritten English (MNIST) & Bengali (BHaND) digits
 * mixed with printed fonts. Zero native JNI bloat, identical on Android & JVM tests.
 */
class DigitCnnClassifier private constructor(
    private val inSize: Int,
    private val c1Out: Int,
    private val c2Out: Int,
    private val d1Out: Int,
    private val numClasses: Int,
    private val w1: FloatArray,
    private val b1: FloatArray,
    private val w2: FloatArray,
    private val b2: FloatArray,
    private val wd1: FloatArray,
    private val bd1: FloatArray,
    private val wd2: FloatArray,
    private val bd2: FloatArray
) {
    private val d1In: Int = 6 * 6 * c2Out

    fun predict(canonical28x28: FloatArray): DigitPrediction {
        require(canonical28x28.size == inSize * inSize) {
            "Expected ${inSize * inSize} floats, got ${canonical28x28.size}"
        }

        val c1 = FloatArray(c1Out * 28 * 28)
        for (co in 0 until c1Out) {
            val bias = b1[co]
            val wBase = co * 9
            val outBase = co * 784
            for (y in 0 until 28) {
                for (x0 in 0 until 28) {
                    var sum = bias
                    for (ky in -1..1) {
                        val iy = y + ky
                        if (iy !in 0 until 28) continue
                        val rowOff = iy * 28
                        val kRow = (ky + 1) * 3
                        for (kx in -1..1) {
                            val ix = x0 + kx
                            if (ix !in 0 until 28) continue
                            sum += canonical28x28[rowOff + ix] * w1[wBase + kRow + (kx + 1)]
                        }
                    }
                    c1[outBase + y * 28 + x0] = if (sum > 0f) sum else 0f
                }
            }
        }

        val p1 = FloatArray(c1Out * 14 * 14)
        for (c in 0 until c1Out) {
            val inB = c * 784
            val outB = c * 196
            for (y in 0 until 14) {
                val y2 = y * 2
                for (x0 in 0 until 14) {
                    val x2 = x0 * 2
                    val i0 = inB + y2 * 28 + x2
                    var best = c1[i0]
                    val v1 = c1[i0 + 1]
                    if (v1 > best) best = v1
                    val v2 = c1[i0 + 28]
                    if (v2 > best) best = v2
                    val v3 = c1[i0 + 29]
                    if (v3 > best) best = v3
                    p1[outB + y * 14 + x0] = best
                }
            }
        }

        val c2 = FloatArray(c2Out * 12 * 12)
        for (co in 0 until c2Out) {
            val bias = b2[co]
            val outB = co * 144
            val wCoBase = co * c1Out * 9
            for (y in 0 until 12) {
                for (x0 in 0 until 12) {
                    var sum = bias
                    for (ci in 0 until c1Out) {
                        val inB = ci * 196
                        val wB = wCoBase + ci * 9
                        val r0 = inB + y * 14 + x0
                        val r1 = r0 + 14
                        val r2 = r1 + 14
                        sum += p1[r0] * w2[wB] +
                            p1[r0 + 1] * w2[wB + 1] +
                            p1[r0 + 2] * w2[wB + 2] +
                            p1[r1] * w2[wB + 3] +
                            p1[r1 + 1] * w2[wB + 4] +
                            p1[r1 + 2] * w2[wB + 5] +
                            p1[r2] * w2[wB + 6] +
                            p1[r2 + 1] * w2[wB + 7] +
                            p1[r2 + 2] * w2[wB + 8]
                    }
                    c2[outB + y * 12 + x0] = if (sum > 0f) sum else 0f
                }
            }
        }

        val p2 = FloatArray(d1In)
        for (c in 0 until c2Out) {
            val inB = c * 144
            val outB = c * 36
            for (y in 0 until 6) {
                val y2 = y * 2
                for (x0 in 0 until 6) {
                    val x2 = x0 * 2
                    val i0 = inB + y2 * 12 + x2
                    var best = c2[i0]
                    val v1 = c2[i0 + 1]
                    if (v1 > best) best = v1
                    val v2 = c2[i0 + 12]
                    if (v2 > best) best = v2
                    val v3 = c2[i0 + 13]
                    if (v3 > best) best = v3
                    p2[outB + y * 6 + x0] = best
                }
            }
        }

        val d1 = FloatArray(d1Out)
        for (o in 0 until d1Out) {
            var sum = bd1[o]
            val wB = o * d1In
            for (i in 0 until d1In) {
                sum += p2[i] * wd1[wB + i]
            }
            d1[o] = if (sum > 0f) sum else 0f
        }

        val logits = FloatArray(numClasses)
        var maxL = Float.NEGATIVE_INFINITY
        for (o in 0 until numClasses) {
            var sum = bd2[o]
            val wB = o * d1Out
            for (i in 0 until d1Out) {
                sum += d1[i] * wd2[wB + i]
            }
            logits[o] = sum
            if (sum > maxL) maxL = sum
        }

        val probs = FloatArray(numClasses)
        var sumExp = 0.0
        for (o in 0 until numClasses) {
            val e = exp((logits[o] - maxL).toDouble())
            probs[o] = e.toFloat()
            sumExp += e
        }
        val invSum = if (sumExp > 0.0) (1.0 / sumExp).toFloat() else 0.1f
        for (o in 0 until numClasses) {
            probs[o] *= invSum
        }

        var top1 = 0
        var top1P = probs[0]
        var top2 = 1
        var top2P = -1f
        for (o in 1 until numClasses) {
            val p = probs[o]
            if (p > top1P) {
                top2 = top1
                top2P = top1P
                top1 = o
                top1P = p
            } else if (p > top2P) {
                top2 = o
                top2P = p
            }
        }

        return DigitPrediction(
            digit = top1,
            confidence = top1P,
            secondDigit = top2,
            secondConfidence = max(0f, top2P),
            probabilities = probs
        )
    }

    companion object {
        const val INPUT_DIM = 28

        fun fromStream(input: InputStream): DigitCnnClassifier {
            DataInputStream(BufferedInputStream(input)).use { dis ->
                val magic = ByteArray(4)
                dis.readFully(magic)
                require(String(magic, Charsets.US_ASCII) == "KGDC") {
                    "Invalid digit CNN model header"
                }
                val version = dis.readInt()
                require(version == 1) { "Unsupported digit CNN model version: $version" }
                val inSize = dis.readInt()
                val c1Out = dis.readInt()
                val c2Out = dis.readInt()
                val d1Out = dis.readInt()
                val numClasses = dis.readInt()
                val d1In = 6 * 6 * c2Out

                val w1 = readF16Array(dis, c1Out * 9)
                val b1 = readF32Array(dis, c1Out)
                val w2 = readF16Array(dis, c2Out * c1Out * 9)
                val b2 = readF32Array(dis, c2Out)
                val wd1 = readF16Array(dis, d1Out * d1In)
                val bd1 = readF32Array(dis, d1Out)
                val wd2 = readF16Array(dis, numClasses * d1Out)
                val bd2 = readF32Array(dis, numClasses)

                return DigitCnnClassifier(
                    inSize, c1Out, c2Out, d1Out, numClasses,
                    w1, b1, w2, b2, wd1, bd1, wd2, bd2
                )
            }
        }

        private fun readF16Array(dis: DataInputStream, count: Int): FloatArray {
            val out = FloatArray(count)
            for (i in 0 until count) {
                out[i] = float16ToFloat(dis.readShort())
            }
            return out
        }

        private fun readF32Array(dis: DataInputStream, count: Int): FloatArray {
            val out = FloatArray(count)
            for (i in 0 until count) {
                out[i] = dis.readFloat()
            }
            return out
        }

        /**
         * IEEE-754 binary16 -> IEEE-754 binary32 exact conversion.
         */
        private fun float16ToFloat(half: Short): Float {
            val bits = half.toInt() and 0xFFFF
            val sign = (bits and 0x8000) shl 16
            val exp = (bits ushr 10) and 0x1F
            val mant = bits and 0x03FF
            val outBits = when (exp) {
                0 -> {
                    if (mant == 0) {
                        sign
                    } else {
                        var m = mant
                        var e = 127 - 15 + 1
                        while ((m and 0x0400) == 0) {
                            m = m shl 1
                            e--
                        }
                        m = m and 0x03FF
                        sign or (e shl 23) or (m shl 13)
                    }
                }
                31 -> sign or 0x7F800000 or (mant shl 13)
                else -> sign or ((exp + (127 - 15)) shl 23) or (mant shl 13)
            }
            return java.lang.Float.intBitsToFloat(outBits)
        }

        /**
         * Fits foreground ink into a 20x20 aspect-preserving box and centers by center-of-mass
         * inside a 28x28 canvas (ink = 1.0f, background = 0.0f). Matches training preprocessing.
         */
        fun canonicalNormalize(src: FloatArray, w: Int, h: Int): FloatArray {
            var maxV = 0f
            for (v in src) if (v > maxV) maxV = v
            if (maxV < 1e-4f) return FloatArray(INPUT_DIM * INPUT_DIM)

            var minX = w
            var minY = h
            var maxX = -1
            var maxY = -1
            val thresh = maxV * 0.12f
            for (y in 0 until h) {
                val rowOff = y * w
                for (x in 0 until w) {
                    if (src[rowOff + x] >= thresh) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }
            if (maxX < minX || maxY < minY) return FloatArray(INPUT_DIM * INPUT_DIM)

            val bw = maxX - minX + 1
            val bh = maxY - minY + 1
            val scale = 20.0f / max(bw, bh)
            val nw = max(1, (bw * scale).roundToInt())
            val nh = max(1, (bh * scale).roundToInt())

            val resized = FloatArray(nw * nh)
            for (dy in 0 until nh) {
                val sy = minY + (dy + 0.5f) * (bh / nh.toFloat()) - 0.5f
                val y0 = floor(sy.toDouble()).toInt().coerceIn(0, h - 1)
                val y1 = (y0 + 1).coerceIn(0, h - 1)
                val fy = (sy - y0).coerceAtLeast(0f)
                for (dx in 0 until nw) {
                    val sx = minX + (dx + 0.5f) * (bw / nw.toFloat()) - 0.5f
                    val x0 = floor(sx.toDouble()).toInt().coerceIn(0, w - 1)
                    val x1 = (x0 + 1).coerceIn(0, w - 1)
                    val fx = (sx - x0).coerceAtLeast(0f)
                    val v00 = src[y0 * w + x0]
                    val v10 = src[y0 * w + x1]
                    val v01 = src[y1 * w + x0]
                    val v11 = src[y1 * w + x1]
                    val v = (v00 * (1f - fx) + v10 * fx) * (1f - fy) +
                        (v01 * (1f - fx) + v11 * fx) * fy
                    resized[dy * nw + dx] = (v / maxV).coerceIn(0f, 1f)
                }
            }

            var sumM = 0f
            var sumX = 0f
            var sumY = 0f
            for (y in 0 until nh) {
                val rOff = y * nw
                for (x in 0 until nw) {
                    val m = resized[rOff + x]
                    sumM += m
                    sumX += m * x
                    sumY += m * y
                }
            }
            val cx = if (sumM > 1e-4f) sumX / sumM else (nw - 1) * 0.5f
            val cy = if (sumM > 1e-4f) sumY / sumM else (nh - 1) * 0.5f
            val offX = (13.5f - cx).roundToInt().coerceIn(1, max(1, INPUT_DIM - nw - 1))
            val offY = (13.5f - cy).roundToInt().coerceIn(1, max(1, INPUT_DIM - nh - 1))

            val out = FloatArray(INPUT_DIM * INPUT_DIM)
            for (y in 0 until nh) {
                val ty = y + offY
                if (ty !in 0 until INPUT_DIM) continue
                val srcOff = y * nw
                val dstOff = ty * INPUT_DIM
                for (x in 0 until nw) {
                    val tx = x + offX
                    if (tx !in 0 until INPUT_DIM) continue
                    out[dstOff + tx] = resized[srcOff + x]
                }
            }
            return out
        }
    }
}

class DualScriptDigitRecognizer(
    private val openEnglishStream: () -> InputStream,
    private val openBengaliStream: () -> InputStream
) {
    private val englishModel by lazy { openEnglishStream().use { DigitCnnClassifier.fromStream(it) } }
    private val bengaliModel by lazy { openBengaliStream().use { DigitCnnClassifier.fromStream(it) } }

    fun predict(canonical28x28: FloatArray, script: OfflineDigitScript): DigitPrediction = when (script) {
        OfflineDigitScript.ENGLISH -> englishModel.predict(canonical28x28)
        OfflineDigitScript.BENGALI -> bengaliModel.predict(canonical28x28)
        OfflineDigitScript.AUTO -> {
            val en = englishModel.predict(canonical28x28)
            val bn = bengaliModel.predict(canonical28x28)
            if (bn.confidence >= en.confidence) bn else en
        }
    }

    fun predictBoth(canonical28x28: FloatArray): Pair<DigitPrediction, DigitPrediction> =
        englishModel.predict(canonical28x28) to bengaliModel.predict(canonical28x28)

    companion object {
        fun fromAssets(context: Context): DualScriptDigitRecognizer = DualScriptDigitRecognizer(
            openEnglishStream = {
                openResilientModelStream(
                    context = context,
                    assetPath = "ml/digits_en.bin",
                    rawResId = R.raw.digits_en,
                    embeddedFallback = { EmbeddedDigitModels.openEnglishStream() }
                )
            },
            openBengaliStream = {
                openResilientModelStream(
                    context = context,
                    assetPath = "ml/digits_bn.bin",
                    rawResId = R.raw.digits_bn,
                    embeddedFallback = { EmbeddedDigitModels.openBengaliStream() }
                )
            }
        )

        private fun openResilientModelStream(
            context: Context,
            assetPath: String,
            rawResId: Int,
            embeddedFallback: () -> InputStream
        ): InputStream {
            val fromAssets = runCatching { context.assets.open(assetPath) }.getOrNull()
            if (fromAssets != null) return fromAssets
            val fromRaw = runCatching { context.resources.openRawResource(rawResId) }.getOrNull()
            if (fromRaw != null) return fromRaw
            return embeddedFallback()
        }
    }
}
