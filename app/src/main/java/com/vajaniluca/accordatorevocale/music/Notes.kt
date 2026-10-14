package com.vajaniluca.accordatorevocale.music

import kotlin.math.ln
import kotlin.math.pow

enum class Notation { ITALIAN, ENGLISH }

/**
 * Conversioni tra frequenza, numero MIDI e nome della nota.
 * Le ottave seguono la numerazione scientifica: Do4 è il Do centrale (MIDI 60), La4 è il La di riferimento (MIDI 69).
 */
object Notes {
    private val ITALIAN = arrayOf("Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")
    private val ENGLISH = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    private val BLACK_KEYS = booleanArrayOf(false, true, false, true, false, false, true, false, true, false, true, false)
    private val LN2 = ln(2.0)

    fun freqToMidi(frequency: Double, a4: Double = 440.0): Double = 69.0 + 12.0 * ln(frequency / a4) / LN2

    fun midiToFreq(midi: Double, a4: Double = 440.0): Double = a4 * 2.0.pow((midi - 69.0) / 12.0)

    fun pitchClassName(midi: Int, notation: Notation): String {
        val names = if (notation == Notation.ITALIAN) ITALIAN else ENGLISH
        return names[midi.mod(12)]
    }

    fun octave(midi: Int): Int = Math.floorDiv(midi, 12) - 1

    fun name(midi: Int, notation: Notation): String = pitchClassName(midi, notation) + octave(midi)

    fun isBlackKey(midi: Int): Boolean = BLACK_KEYS[midi.mod(12)]

    /** Scostamento in cent di una altezza (MIDI frazionario) rispetto a una nota. */
    fun cents(midi: Double, target: Int): Double = (midi - target) * 100.0
}
