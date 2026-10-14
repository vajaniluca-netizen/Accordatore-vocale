package com.vajaniluca.accordatorevocale.audio

import kotlin.math.abs

/**
 * Pulisce la sequenza di altezze stimate.
 * All'attacco una nota viene accettata solo se resta stabile per [minStableFrames] frame:
 * il canto si ferma su una nota, il parlato (per esempio la TV) cambia altezza di continuo.
 * Poi scarta i salti isolati (tipici errori di ottava) confrontando ogni valore con la mediana
 * degli ultimi frame, ma lascia passare il vibrato e accetta un vero cambio di nota dopo un paio di frame.
 */
class PitchTracker(
    private val windowSize: Int = 5,
    private val maxGapFrames: Int = 3,
    private val outlierSemitones: Double = 1.0,
    private val minStableFrames: Int = 4,
    private val stableSpanSemitones: Double = 0.8,
) {
    private val recent = ArrayDeque<Double>()
    private var gap = 0
    private var confirmed = false

    fun reset() {
        recent.clear()
        gap = 0
        confirmed = false
    }

    fun process(midi: Double?): Double? {
        if (midi == null) {
            gap++
            if (gap >= maxGapFrames) {
                recent.clear()
                confirmed = false
            }
            return null
        }
        gap = 0
        recent.addLast(midi)
        if (recent.size > windowSize) recent.removeFirst()

        if (!confirmed) {
            if (recent.size < minStableFrames) return null
            val last = recent.toList().takeLast(minStableFrames)
            if (last.max() - last.min() > stableSpanSemitones) return null
            confirmed = true
        }
        if (recent.size < 3) return midi
        val median = recent.sorted()[recent.size / 2]
        return if (abs(midi - median) <= outlierSemitones) midi else median
    }
}
