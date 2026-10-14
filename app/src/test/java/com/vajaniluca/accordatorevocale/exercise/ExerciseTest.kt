package com.vajaniluca.accordatorevocale.exercise

import com.vajaniluca.accordatorevocale.audio.PitchPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseTest {
    private val ex = Exercise("t", "Test", 60, listOf(ExerciseNote(60, 1.0), ExerciseNote(null, 1.0), ExerciseNote(64, 2.0)))

    @Test
    fun timeline() {
        val tl = ex.timeline()
        assertEquals(3, tl.size)
        assertEquals(2.0, tl[2].startSec, 1e-9)
        assertEquals(4.0, tl[2].endSec, 1e-9)
        assertEquals(4.0, ex.durationSec, 1e-9)
    }

    @Test
    fun trasposizione() {
        val t = ex.transposed(3)
        assertEquals(listOf(63, null, 67), t.notes.map { it.midi })
        assertEquals(63..67, t.midiRange())
    }

    private fun samples(cents: (Double) -> Double?) =
        (0 until 400).map { i -> val t = i * 0.01; PitchPoint(t, cents(t)) }

    @Test
    fun punteggioPerfetto() {
        val r = Scoring.score(ex, samples { t -> if (t < 1) 60.0 else if (t >= 2) 64.0 else null }, 20)
        assertEquals(2, r.notes.size)
        assertEquals(1.0, r.inTuneFraction, 1e-9)
        assertEquals(0.0, r.meanCents!!, 1e-9)
    }

    @Test
    fun punteggioCrescente() {
        val r = Scoring.score(ex, samples { t -> if (t < 1) 60.3 else if (t >= 2) 64.3 else null }, 20)
        assertEquals(30.0, r.meanCents!!, 1e-6)
        assertEquals(0.0, r.inTuneFraction, 1e-9)
    }

    @Test
    fun notaNonCantata() {
        val r = Scoring.score(ex, samples { t -> if (t < 1) 60.0 else null }, 20)
        assertNull(r.notes[1].medianCents)
        assertEquals(0.0, r.notes[1].voicedFraction, 1e-9)
        assertEquals(0.5, r.inTuneFraction, 1e-9)
    }
}
