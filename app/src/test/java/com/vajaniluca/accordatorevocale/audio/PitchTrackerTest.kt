package com.vajaniluca.accordatorevocale.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchTrackerTest {
    @Test
    fun scartaErroreDiOttavaIsolato() {
        val t = PitchTracker()
        repeat(6) { t.process(60.1) }
        assertEquals(60.1, t.process(72.1)!!, 1e-9)
        assertEquals(60.2, t.process(60.2)!!, 1e-9)
    }

    @Test
    fun accettaUnVeroCambioDiNota() {
        val t = PitchTracker()
        repeat(6) { t.process(60.0) }
        val out = (1..4).map { t.process(67.0)!! }
        assertEquals(67.0, out.last(), 1e-9)
        assertEquals(67.0, out[2], 1e-9)
    }

    @Test
    fun mantieneIlVibrato() {
        val t = PitchTracker()
        repeat(6) { t.process(60.0) }
        val input = listOf(60.0, 60.3, 60.5, 60.3, 60.0, 59.7, 59.5, 59.7, 60.0)
        val out = input.map { t.process(it)!! }
        assertEquals(input, out)
    }

    @Test
    fun attaccoRichiedeUnaNotaStabile() {
        val t = PitchTracker()
        repeat(5) { assertNull(t.process(60.0 + 0.1 * (it % 2))) }
        assertEquals(60.1, t.process(60.1)!!, 1e-9)
    }

    @Test
    fun parlatoCheScivolaVieneIgnorato() {
        val t = PitchTracker()
        // Intonazione del parlato: sale e scende di più semitoni in poche decine di millisecondi
        val speech = listOf(55.0, 56.5, 58.0, 57.0, 55.5, 54.0, 56.0, 58.5, 57.0, 55.0)
        assertTrue(speech.all { t.process(it) == null })
    }

    @Test
    fun silenzioRestituisceNull() {
        val t = PitchTracker()
        repeat(6) { t.process(60.0) }
        assertNull(t.process(null))
    }
}
