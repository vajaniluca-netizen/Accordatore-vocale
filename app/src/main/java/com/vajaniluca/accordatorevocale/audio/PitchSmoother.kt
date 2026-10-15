package com.vajaniluca.accordatorevocale.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Altezza stabilizzata per l'indicatore, in due passaggi:
 * una mediana sugli ultimi [medianSize] frame elimina i picchi isolati, poi una media esponenziale
 * nel tempo attenua vibrato e tremolii. Un cambio di nota ampio viene seguito senza la scia della media.
 * I brevi buchi del segnale (fino a [maxGapSec]) non azzerano la media; con [holdDuringGaps]
 * durante il buco viene restituito l'ultimo valore, così l'indicatore non lampeggia.
 */
class PitchSmoother(
    var timeConstantSec: Double,
    private val jumpSemitones: Double = 1.5,
    private val maxGapSec: Double = 0.5,
    private val medianSize: Int = 7,
    private val holdDuringGaps: Boolean = false,
) {
    private var value: Double? = null
    private var lastTime = 0.0
    private val window = ArrayDeque<Double>()

    fun reset() {
        value = null
        window.clear()
    }

    fun process(timeSec: Double, raw: Double?): Double? {
        if (raw == null) {
            if (timeSec - lastTime > maxGapSec) reset()
            return if (holdDuringGaps) value else null
        }
        if (value != null && timeSec - lastTime > maxGapSec) reset()
        window.addLast(raw)
        if (window.size > medianSize) window.removeFirst()
        val midi = window.sorted()[window.size / 2]
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
class NoteHysteresis(private val marginSemitones: Double = 0.25) {
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
