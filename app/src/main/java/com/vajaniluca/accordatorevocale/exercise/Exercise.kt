package com.vajaniluca.accordatorevocale.exercise

/** Una nota dell'esercizio: midi null indica una pausa; la durata è in movimenti (beat). */
data class ExerciseNote(val midi: Int?, val beats: Double)

data class TimedNote(val index: Int, val midi: Int?, val startSec: Double, val endSec: Double)

data class Exercise(
    val id: String,
    val name: String,
    val bpm: Int,
    val notes: List<ExerciseNote>,
) {
    val secondsPerBeat: Double get() = 60.0 / bpm

    val durationSec: Double get() = notes.sumOf { it.beats } * secondsPerBeat

    fun timeline(): List<TimedNote> {
        var t = 0.0
        return notes.mapIndexed { i, note ->
            val start = t
            t += note.beats * secondsPerBeat
            TimedNote(i, note.midi, start, t)
        }
    }

    fun transposed(semitones: Int): Exercise =
        if (semitones == 0) this else copy(notes = notes.map { it.copy(midi = it.midi?.plus(semitones)) })

    fun midiRange(): IntRange? {
        val sung = notes.mapNotNull { it.midi }
        return if (sung.isEmpty()) null else sung.min()..sung.max()
    }
}
