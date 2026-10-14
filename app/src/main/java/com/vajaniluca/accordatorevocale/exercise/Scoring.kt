package com.vajaniluca.accordatorevocale.exercise

import com.vajaniluca.accordatorevocale.audio.PitchPoint
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

data class NoteResult(
    val note: TimedNote,
    /** Scostamento tipico (mediana) in cent: positivo = più alto, negativo = più basso. Null se non hai cantato. */
    val medianCents: Double?,
    /** Deviazione standard in cent: quanto oscilla la voce sulla nota. */
    val spreadCents: Double?,
    /** Frazione della durata della nota passata entro la tolleranza. */
    val inTuneFraction: Double,
    /** Frazione della durata della nota in cui è stata rilevata la voce. */
    val voicedFraction: Double,
)

data class ExerciseResult(
    val exerciseId: String,
    val exerciseName: String,
    val toleranceCents: Int,
    val notes: List<NoteResult>,
) {
    val inTuneFraction: Double get() = if (notes.isEmpty()) 0.0 else notes.sumOf { it.inTuneFraction } / notes.size

    val meanCents: Double? get() = notes.mapNotNull { it.medianCents }.takeIf { it.isNotEmpty() }?.average()

    val meanAbsCents: Double? get() = notes.mapNotNull { it.medianCents?.let(::abs) }.takeIf { it.isNotEmpty() }?.average()
}

object Scoring {
    /** Tempo iniziale di ogni nota escluso dal punteggio: serve alla voce per assestarsi. */
    const val SETTLE_SEC = 0.15

    fun score(exercise: Exercise, samples: List<PitchPoint>, toleranceCents: Int): ExerciseResult {
        val results = exercise.timeline().filter { it.midi != null }.map { note ->
            val target = note.midi!!
            val from = note.startSec + min(SETTLE_SEC, (note.endSec - note.startSec) * 0.3)
            val window = samples.filter { it.timeSec >= from && it.timeSec < note.endSec }
            val cents = window.mapNotNull { p -> p.midi?.let { (it - target) * 100.0 } }
            val median = cents.sorted().let { if (it.isEmpty()) null else it[it.size / 2] }
            val spread = if (cents.size < 2) null else {
                val mean = cents.average()
                sqrt(cents.sumOf { (it - mean) * (it - mean) } / cents.size)
            }
            NoteResult(
                note = note,
                medianCents = median,
                spreadCents = spread,
                inTuneFraction = if (window.isEmpty()) 0.0 else cents.count { abs(it) <= toleranceCents }.toDouble() / window.size,
                voicedFraction = if (window.isEmpty()) 0.0 else cents.size.toDouble() / window.size,
            )
        }
        return ExerciseResult(exercise.id, exercise.name, toleranceCents, results)
    }
}
