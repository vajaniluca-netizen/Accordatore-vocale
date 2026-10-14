package com.vajaniluca.accordatorevocale.audio

import kotlin.math.abs

/**
 * Pulisce la sequenza di altezze stimate: scarta i salti isolati (tipicamente errori di ottava)
 * confrontando ogni valore con la mediana degli ultimi frame, ma lascia passare il vibrato
 * e accetta un vero cambio di nota dopo un paio di frame.
 */
class PitchTracker(
    private val windowSize: Int = 5,
    private val maxGapFrames: Int = 3,
    private val outlierSemitones: Double = 1.0,
) {
    private val recent = ArrayDeque<Double>()
    private var gap = 0

    fun reset() {
        recent.clear()
        gap = 0
    }

    fun process(midi: Double?): Double? {
        if (midi == null) {
            gap++
            if (gap >= maxGapFrames) recent.clear()
            return null
        }
        gap = 0
        recent.addLast(midi)
        if (recent.size > windowSize) recent.removeFirst()
        if (recent.size < 3) return midi
        val median = recent.sorted()[recent.size / 2]
        return if (abs(midi - median) <= outlierSemitones) midi else median
    }
}
