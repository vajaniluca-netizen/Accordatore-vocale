package com.vajaniluca.accordatorevocale.exercise

/** Esercizi di esempio. Le note sono numeri MIDI: 60 = Do4 (Do centrale). */
object Presets {
    private fun seq(beats: Double, vararg midi: Int?) = midi.map { ExerciseNote(it, beats) }

    private val majorScale = listOf(0, 2, 4, 5, 7, 9, 11, 12)

    fun all(): List<Exercise> = listOf(
        Exercise(
            id = "preset-note-lunghe",
            name = "Note lunghe",
            bpm = 60,
            notes = listOf(
                ExerciseNote(60, 4.0), ExerciseNote(null, 2.0),
                ExerciseNote(64, 4.0), ExerciseNote(null, 2.0),
                ExerciseNote(67, 4.0), ExerciseNote(null, 2.0),
                ExerciseNote(64, 4.0), ExerciseNote(null, 2.0),
                ExerciseNote(60, 4.0),
            ),
        ),
        Exercise(
            id = "preset-scala-maggiore",
            name = "Scala maggiore di Do",
            bpm = 72,
            notes = seq(1.0, *(majorScale + majorScale.reversed().drop(1)).map { 60 + it }.toTypedArray()) +
                ExerciseNote(60, 1.0),
        ),
        Exercise(
            id = "preset-cinque-note",
            name = "Vocalizzo a cinque note (sale di semitono)",
            bpm = 96,
            notes = (0..4).flatMap { shift ->
                val base = 60 + shift
                listOf(0, 2, 4, 5, 7, 5, 4, 2).map { ExerciseNote(base + it, 1.0) } +
                    ExerciseNote(base, 2.0) + ExerciseNote(null, 2.0)
            },
        ),
        Exercise(
            id = "preset-arpeggio",
            name = "Arpeggio maggiore",
            bpm = 72,
            notes = seq(1.0, 60, 64, 67, 72, 67, 64) + ExerciseNote(60, 2.0),
        ),
        Exercise(
            id = "preset-terze",
            name = "Terze per grado congiunto",
            bpm = 80,
            notes = seq(1.0, 60, 64, 62, 65, 64, 67, 65, 69, 67, 71, 69, 72) + ExerciseNote(72, 2.0),
        ),
    )
}
