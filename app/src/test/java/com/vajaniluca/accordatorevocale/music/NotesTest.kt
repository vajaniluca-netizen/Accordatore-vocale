package com.vajaniluca.accordatorevocale.music

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesTest {
    @Test
    fun conversioniFrequenza() {
        assertEquals(69.0, Notes.freqToMidi(440.0), 1e-9)
        assertEquals(60.0, Notes.freqToMidi(261.6256), 1e-3)
        assertEquals(69.0, Notes.freqToMidi(442.0, a4 = 442.0), 1e-9)
        assertEquals(220.0, Notes.midiToFreq(57.0), 1e-9)
    }

    @Test
    fun nomiNote() {
        assertEquals("Do4", Notes.name(60, Notation.ITALIAN))
        assertEquals("La4", Notes.name(69, Notation.ITALIAN))
        assertEquals("Fa♯3", Notes.name(54, Notation.ITALIAN))
        assertEquals("A4", Notes.name(69, Notation.ENGLISH))
        assertEquals("Si-1", Notes.name(11, Notation.ITALIAN))
    }

    @Test
    fun cent() {
        assertEquals(25.0, Notes.cents(60.25, 60), 1e-9)
        assertEquals(-110.0, Notes.cents(58.9, 60), 1e-9)
    }
}
