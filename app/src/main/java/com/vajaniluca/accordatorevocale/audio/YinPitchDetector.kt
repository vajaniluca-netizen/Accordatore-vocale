package com.vajaniluca.accordatorevocale.audio

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Stima della frequenza fondamentale con l'algoritmo YIN (de Cheveigné e Kawahara, 2002),
 * con interpolazione parabolica per avere precisione sotto il campione.
 */
class YinPitchDetector(
    private val sampleRate: Int,
    private val bufferSize: Int,
    private val threshold: Double = 0.15,
    private val maxAperiodicity: Double = 0.3,
    minFrequency: Double = 60.0,
    maxFrequency: Double = 1500.0,
) {
    data class Result(val frequency: Double, val confidence: Double)

    private val window = bufferSize / 2
    private val tauMin = max(2, floor(sampleRate / maxFrequency).toInt())
    private val tauMax = min(window - 1, ceil(sampleRate / minFrequency).toInt())
    private val difference = DoubleArray(tauMax + 2)
    private val cmnd = DoubleArray(tauMax + 2)

    fun detect(buffer: FloatArray): Result? {
        require(buffer.size >= bufferSize) { "Buffer troppo corto: ${buffer.size} < $bufferSize" }

        for (tau in 1..tauMax + 1) {
            var sum = 0.0
            for (j in 0 until window) {
                val d = (buffer[j] - buffer[j + tau]).toDouble()
                sum += d * d
            }
            difference[tau] = sum
        }

        cmnd[0] = 1.0
        var running = 0.0
        for (tau in 1..tauMax + 1) {
            running += difference[tau]
            cmnd[tau] = if (running == 0.0) 1.0 else difference[tau] * tau / running
        }

        var tau = -1
        var t = tauMin
        while (t <= tauMax) {
            if (cmnd[t] < threshold) {
                while (t + 1 <= tauMax && cmnd[t + 1] < cmnd[t]) t++
                tau = t
                break
            }
            t++
        }
        if (tau == -1) {
            var best = tauMin
            for (i in tauMin + 1..tauMax) if (cmnd[i] < cmnd[best]) best = i
            if (cmnd[best] > maxAperiodicity) return null
            tau = best
        }

        // L'interpolazione sulla funzione differenza grezza è meno distorta di quella sulla versione normalizzata.
        val d0 = difference[tau - 1]
        val d1 = difference[tau]
        val d2 = difference[tau + 1]
        val denominator = d0 - 2 * d1 + d2
        val shift = if (denominator > 0) ((d0 - d2) / (2 * denominator)).coerceIn(-1.0, 1.0) else 0.0

        return Result(sampleRate / (tau + shift), (1.0 - cmnd[tau]).coerceIn(0.0, 1.0))
    }
}
