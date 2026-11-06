package com.imash.app.mashup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class BattereTest {

    @Test
    fun battereDoveCambiaLAccordo() {
        // 40 battiti ogni 0,5 s, croma a 10 frame/s; l'accordo cambia sui battiti 2, 6, 10, …
        val tempi = DoubleArray(40) { it * 0.5 }
        val accordi = listOf(intArrayOf(0, 4, 7), intArrayOf(5, 9, 0), intArrayOf(7, 11, 2))
        val croma = Array(200) { f ->
            val battito = (f / 10.0 / 0.5).toInt()
            val accordo = accordi[Math.floorDiv(battito - 2, 4).mod(3)]
            FloatArray(12) { if (it in accordo) 1f else 0.1f }
        }
        assertEquals(2, faseBattere(tempi, croma, 10.0))
    }

    @Test
    fun grigliaEstesaFinoAllInizioDelSuono() {
        val g = grigliaMisure(doubleArrayOf(5.0, 7.0, 9.0), 2.0, 0.8)
        assertArrayEquals(doubleArrayOf(1.0, 3.0, 5.0, 7.0, 9.0), g, 1e-9)
    }
}
