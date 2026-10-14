package com.vajaniluca.accordatorevocale.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Media esponenziale nel tempo dell'altezza, per un indicatore stabile.
 * Un salto ampio (cambio di nota) viene seguito subito, senza la scia della media.
 */
class PitchSmoother(
    var timeConstantSec: Double,
    private val jumpSemitones: Double = 1.5,
    private val maxGapSec: Double = 0.15,
) {
    private var value: Double? = null
    private var lastTime = 0.0

    fun reset() {
        value = null
    }

    fun process(timeSec: Double, midi: Double?): Double? {
        if (midi == null) {
            if (timeSec - lastTime > maxGapSec) value = null
            return null
        }
        val previous = value
        val next = if (previous == null || abs(midi - previous) > jumpSemitones || timeConstantSec <= 0.0) {
            midi
        } else {
            val alpha = 1.0 - exp(-(timeSec - lastTime).coerceAtLeast(0.0) / timeConstantSec)
            previous + alpha * (midi - previous)
        }
        value = next
        lastTime = timeSec
        return next
    }
}

/**
 * Sceglie la nota più vicina con un margine: per passare alla nota accanto devi superare
 * la metà del semitono di [marginSemitones], così il nome non sfarfalla quando sei sul confine.
 */
class NoteHysteresis(private val marginSemitones: Double = 0.15) {
    private var current: Int? = null

    fun reset() {
        current = null
    }

    fun process(midi: Double?): Int? {
        if (midi == null) return current
        val c = current
        if (c == null || abs(midi - c) > 0.5 + marginSemitones) current = midi.roundToInt()
        return current
    }
}
