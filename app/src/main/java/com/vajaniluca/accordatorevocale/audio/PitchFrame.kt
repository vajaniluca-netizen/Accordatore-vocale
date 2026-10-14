package com.vajaniluca.accordatorevocale.audio

/** Un'analisi del microfono: tempo in secondi dall'avvio, altezza come MIDI frazionario (null se non c'è voce). */
data class PitchFrame(
    val generation: Int,
    val timeSec: Double,
    val frequency: Double?,
    val midi: Double?,
    val levelDb: Double,
)

data class PitchPoint(val timeSec: Double, val midi: Double?)
