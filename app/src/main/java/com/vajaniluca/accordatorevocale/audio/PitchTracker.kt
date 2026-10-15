package com.vajaniluca.accordatorevocale.audio

import kotlin.math.abs

/**
 * Pulisce la sequenza di altezze stimate.
 * All'attacco una nota viene accettata solo se resta stabile per [minStableFrames] frame (circa 0,14 s):
 * il canto si ferma su una nota, il parlato (per esempio la TV) cambia altezza di continuo.
 * Poi scarta i salti isolati (tipici errori di ottava) confrontando ogni valore con la mediana
 * degli ultimi frame, ma lascia passare il vibrato e accetta un vero cambio di nota dopo un paio di frame.
 */
class PitchTracker(
    private val windowSize: Int = 5,
    private val maxGapFrames: Int = 3,
    private val outlierSemitones: Double = 1.0,
    private val minStableFrames: Int = 6,
    private val stableSpanSemitones: Double = 0.8,
) {
    private val recent = ArrayDeque<Double>()
    private val onset = ArrayDeque<Double>()
    private var gap = 0
    private var confirmed = false

    fun reset() {
        recent.clear()
        onset.clear()
        gap = 0
        confirmed = false
    }

    fun process(midi: Double?): Double? {
        if (midi == null) {
            gap++
            if (gap >= maxGapFrames) {
                recent.clear()
                onset.clear()
                confirmed = false
            }
            return null
        }
        gap = 0
        recent.addLast(midi)
        if (recent.size > windowSize) recent.removeFirst()

        if (!confirmed) {
            onset.addLast(midi)
            if (onset.size > minStableFrames) onset.removeFirst()
            if (onset.size < minStableFrames) return null
            if (onset.max() - onset.min() > stableSpanSemitones) return null
            confirmed = true
        }
        if (recent.size < 3) return midi
        val median = recent.sorted()[recent.size / 2]
        return if (abs(midi - median) <= outlierSemitones) midi else median
    }
}
