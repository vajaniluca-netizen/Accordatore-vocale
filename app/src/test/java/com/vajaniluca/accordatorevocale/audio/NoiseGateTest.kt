package com.vajaniluca.accordatorevocale.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseGateTest {
    private val dt = 1024.0 / 44100

    @Test
    fun tvLontanaNonApreLaSoglia() {
        val g = NoiseGate()
        // Silenzio, poi TV con livello che oscilla tra -48 e -40 dB per 10 secondi
        repeat(40) { g.process(-65.0, dt) }
        var openFrames = 0
        for (i in 0 until 430) {
            val level = if ((i / 10) % 2 == 0) -48.0 else -40.0
            if (g.process(level, dt) && i > 200) openFrames++
        }
        assertTrue("aperta per $openFrames frame", openFrames == 0)
    }

    @Test
    fun voceVicinaSopraLaTvApre() {
        val g = NoiseGate()
        repeat(300) { g.process(if (it % 20 < 10) -48.0 else -42.0, dt) }
        assertTrue(g.process(-20.0, dt))
    }

    @Test
    fun notaLungaNonDiventaRumore() {
        val g = NoiseGate()
        repeat(40) { g.process(-65.0, dt) }
        var allOpen = true
        repeat(430) { allOpen = g.process(-25.0, dt, pitched = true) && allOpen } // 10 s di nota tenuta
        assertTrue(allOpen)
    }

    @Test
    fun voceSopraLaTvDopoCheSiEAdattata() {
        val g = NoiseGate()
        repeat(40) { g.process(-65.0, dt) }
        repeat(430) { g.process(if ((it / 10) % 2 == 0) -48.0 else -40.0, dt) }
        var open = 0
        repeat(100) { if (g.process(-22.0, dt, pitched = true)) open++ }
        assertTrue("aperta per $open frame su 100", open >= 99)
    }

    @Test
    fun silenzioChiude() {
        val g = NoiseGate()
        repeat(40) { g.process(-65.0, dt) }
        g.process(-25.0, dt)
        assertFalse(g.process(-64.0, dt))
    }
}
