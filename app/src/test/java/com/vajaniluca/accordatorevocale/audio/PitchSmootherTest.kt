package com.vajaniluca.accordatorevocale.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PitchSmootherTest {
    private val hop = 1024.0 / 44100

    @Test
    fun attenuaIlTremolio() {
        val s = PitchSmoother(0.25)
        // Vibrato di ±40 cent a 6 Hz attorno a 60
        val out = (0 until 200).map { i -> val t = i * hop; s.process(t, 60 + 0.4 * sin(2 * PI * 6 * t))!! }
        val swing = out.drop(50).let { it.max() - it.min() }
        assertTrue("oscillazione residua $swing", swing < 0.3)
    }

    @Test
    fun seguitSubitoUnCambioDiNota() {
        val s = PitchSmoother(0.3)
        repeat(20) { s.process(it * hop, 60.0) }
        assertEquals(67.0, s.process(20 * hop, 67.0)!!, 1e-9)
    }

    @Test
    fun convergeSullaNotaTenuta() {
        val s = PitchSmoother(0.2)
        s.process(0.0, 60.0)
        var v = 0.0
        for (i in 1..60) v = s.process(i * hop, 60.3)!!
        assertEquals(60.3, v, 0.01)
    }

    @Test
    fun ripartedaZeroDopoIlSilenzio() {
        val s = PitchSmoother(0.3)
        s.process(0.0, 60.0)
        assertNull(s.process(0.5, null))
        assertEquals(61.0, s.process(0.6, 61.0)!!, 1e-9)
    }

    @Test
    fun isteresiDellaNota() {
        val h = NoteHysteresis()
        assertEquals(60, h.process(60.1))
        assertEquals(60, h.process(60.55))
        assertEquals(60, h.process(60.6))
        assertEquals(61, h.process(60.7))
        assertEquals(61, h.process(60.4))
        assertTrue(abs(60.4 - 61) < 0.65)
    }
}
