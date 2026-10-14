package com.vajaniluca.accordatorevocale.exercise

import com.vajaniluca.accordatorevocale.audio.PitchPoint

/** Esecuzione in corso: startSec è il tempo del microfono in cui inizia la prima nota. */
data class ExerciseSession(
    val exercise: Exercise,
    val startSec: Double,
    val samples: MutableList<PitchPoint> = mutableListOf(),
    val result: ExerciseResult? = null,
) {
    val timeline: List<TimedNote> = exercise.timeline()
}
