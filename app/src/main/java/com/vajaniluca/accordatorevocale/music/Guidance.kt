package com.vajaniluca.accordatorevocale.music

import kotlin.math.abs
import kotlin.math.roundToInt

/** Cosa fare con la voce per arrivare alla nota: salire, scendere o restare così. */
enum class Direction { UP, DOWN, HOLD }

data class Guidance(val direction: Direction, val text: String)

object Guide {
    /**
     * Trasforma uno scostamento in cent (positivo = sei più alto) in un'indicazione pratica.
     * Entro mezzo semitono dice solo "un poco"; oltre indica i semitoni, e riconosce l'ottava.
     */
    fun forCents(cents: Double, toleranceCents: Int): Guidance {
        if (abs(cents) <= toleranceCents) return Guidance(Direction.HOLD, "✓ Intonato, resta così")
        val direction = if (cents < 0) Direction.UP else Direction.DOWN
        val verb = if (direction == Direction.UP) "▲ Sali" else "▼ Scendi"
        val semitones = (abs(cents) / 100).roundToInt()
        val amount = when {
            abs(cents) < 50 -> "un poco"
            semitones in 11..13 -> "di un'ottava"
            semitones == 1 -> "di un semitono"
            semitones == 0 -> "un poco"
            else -> "di $semitones semitoni"
        }
        return Guidance(direction, "$verb $amount")
    }
}
