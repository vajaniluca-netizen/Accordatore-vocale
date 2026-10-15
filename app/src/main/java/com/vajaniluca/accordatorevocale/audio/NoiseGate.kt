package com.vajaniluca.accordatorevocale.audio

import kotlin.math.max
import kotlin.math.min

/**
 * Soglia di volume che si adatta al rumore di fondo (TV, ventilatori, voci lontane).
 * Il livello di fondo scende subito quando l'ambiente si fa silenzioso e sale piano, ma resta
 * fermo mentre c'è una nota cantata stabile, così una nota lunga non viene scambiata per rumore.
 * Un rumore forte che arriva all'improvviso apre la soglia per pochi secondi, poi viene assorbito.
 * È considerata voce solo ciò che supera il fondo di [marginDb].
 */
class NoiseGate(
    @Volatile var marginDb: Double = 20.0,
    private val absoluteMinDb: Double = -50.0,
    private val riseDbPerSec: Double = 5.0,
    private val hysteresisDb: Double = 4.0,
) {
    var floorDb: Double = Double.NaN
        private set
    var open = false
        private set

    val thresholdDb: Double get() = max(absoluteMinDb, (if (floorDb.isNaN()) -120.0 else floorDb) + marginDb)

    fun reset() {
        floorDb = Double.NaN
        open = false
    }

    /** [pitched]: nel frame precedente è stata riconosciuta una nota stabile. */
    fun process(levelDb: Double, dtSec: Double, pitched: Boolean = false): Boolean {
        val floor = floorDb
        floorDb = when {
            floor.isNaN() -> levelDb
            levelDb < floor -> floor + (levelDb - floor) * 0.5
            !open || !pitched -> floor + min(levelDb - floor, riseDbPerSec * dtSec)
            else -> floor
        }
        val threshold = thresholdDb
        open = if (open) levelDb > threshold - hysteresisDb else levelDb > threshold
        return open
    }
}
