package com.vajaniluca.accordatorevocale.audio

import com.vajaniluca.accordatorevocale.music.Notes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class YinPitchDetectorTest {
    private val sampleRate = 44100
    private val size = 4096
    private val detector = YinPitchDetector(sampleRate, size)

    private fun signal(frequency: Double, harmonics: Int = 1, noise: Double = 0.0, phase: Double = 0.3): FloatArray {
        val rnd = Random(42)
        return FloatArray(size) { i ->
            var v = 0.0
            for (k in 1..harmonics) v += sin(2 * PI * frequency * k * i / sampleRate + phase * k) / k
            (0.4 * v + noise * (rnd.nextDouble() * 2 - 1)).toFloat()
        }
    }

    private fun centsError(expected: Double, buffer: FloatArray): Double {
        val result = assertNotNullResult(buffer)
        return abs(Notes.freqToMidi(result.frequency) - Notes.freqToMidi(expected)) * 100
    }

    private fun assertNotNullResult(buffer: FloatArray): YinPitchDetector.Result {
        val r = detector.detect(buffer)
        assertNotNull(r)
        return r!!
    }

    private val voiceRange = listOf(82.41, 98.0, 110.0, 146.83, 220.0, 261.63, 329.63, 440.0, 659.25, 880.0, 1046.5)

    @Test
    fun sinusoidePura() {
        for (f in voiceRange) {
            val err = centsError(f, signal(f))
            assert(err < 2.0) { "$f Hz: errore $err cent" }
        }
    }

    @Test
    fun suonoRicoDiArmonici() {
        for (f in voiceRange) {
            val err = centsError(f, signal(f, harmonics = 8))
            assert(err < 3.0) { "$f Hz: errore $err cent" }
        }
    }

    @Test
    fun conRumore() {
        for (f in voiceRange) {
            val err = centsError(f, signal(f, harmonics = 6, noise = 0.08))
            assert(err < 5.0) { "$f Hz: errore $err cent" }
        }
    }

    @Test
    fun scostamentoDiPochiCentRilevato() {
        val target = 220.0
        val sharp = Notes.midiToFreq(Notes.freqToMidi(target) + 0.1) // +10 cent
        val r = assertNotNullResult(signal(sharp, harmonics = 6))
        assertEquals(10.0, (Notes.freqToMidi(r.frequency) - Notes.freqToMidi(target)) * 100, 1.5)
    }

    @Test
    fun silenzioERumoreNonDannoNota() {
        assertNull(detector.detect(FloatArray(size)))
        val rnd = Random(1)
        assertNull(detector.detect(FloatArray(size) { (rnd.nextDouble() * 2 - 1).toFloat() * 0.3f }))
    }
}
