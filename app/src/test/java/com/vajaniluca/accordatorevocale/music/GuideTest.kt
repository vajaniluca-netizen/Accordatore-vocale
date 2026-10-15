package com.vajaniluca.accordatorevocale.music

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideTest {
    @Test
    fun indicazioni() {
        assertEquals(Guidance(Direction.HOLD, "✓ Intonato, resta così"), Guide.forCents(-12.0, 20))
        assertEquals(Guidance(Direction.UP, "▲ Sali un poco"), Guide.forCents(-35.0, 20))
        assertEquals(Guidance(Direction.DOWN, "▼ Scendi un poco"), Guide.forCents(30.0, 20))
        assertEquals(Guidance(Direction.UP, "▲ Sali di un semitono"), Guide.forCents(-90.0, 20))
        assertEquals(Guidance(Direction.UP, "▲ Sali di 5 semitoni"), Guide.forCents(-510.0, 20))
        assertEquals(Guidance(Direction.DOWN, "▼ Scendi di un'ottava"), Guide.forCents(1190.0, 20))
    }
}
