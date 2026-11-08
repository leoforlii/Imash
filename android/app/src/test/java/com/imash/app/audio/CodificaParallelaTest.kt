package com.imash.app.audio

import com.imash.app.audio.CodificaParallela.CAMPIONI_FRAME
import com.imash.app.audio.CodificaParallela.MARGINE
import com.imash.app.audio.CodificaParallela.RISCALDAMENTO
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CodificaParallelaTest {

    @Test
    fun pezziContiguiSenzaBuchi() {
        val totale = 600L * 44_100                        // 10 minuti
        val pezzi = CodificaParallela.pezzi(totale, 4)
        assertEquals(4, pezzi.size)
        assertEquals(0L, pezzi[0].primoFrame)
        assertEquals(0, pezzi[0].daSaltare)
        assertEquals(0L, pezzi[0].inizioCampioni)
        assertNull(pezzi.last().fineFrame)
        assertEquals(totale, pezzi.last().fineCampioni)
        for (k in 1 until pezzi.size) {
            val p = pezzi[k]
            // Ogni pezzo riparte dove finisce il precedente (nessun frame perso o doppio)...
            assertEquals(pezzi[k - 1].fineFrame, p.primoFrame)
            // ...leggendo qualche frame prima per scaldare LAME e qualche frame dopo per guardare avanti.
            assertEquals(RISCALDAMENTO, p.daSaltare)
            assertEquals((p.primoFrame - RISCALDAMENTO) * CAMPIONI_FRAME, p.inizioCampioni)
            assertEquals((p.primoFrame + MARGINE) * CAMPIONI_FRAME, pezzi[k - 1].fineCampioni)
        }
    }

    @Test
    fun brevissimoUnPezzoSolo() {
        val pezzi = CodificaParallela.pezzi(10L * CAMPIONI_FRAME, 4)
        assertEquals(1, pezzi.size)
        assertEquals(0, pezzi[0].daSaltare)
        assertNull(pezzi[0].fineFrame)
    }

    private fun frame(kbpsIndice: Int, padding: Int): ByteArray {
        // MPEG-1 Layer III, 44,1 kHz, stereo.
        val b2 = (kbpsIndice shl 4) or (padding shl 1)
        val lunghezza = 144 * intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)[kbpsIndice] * 1000 / 44_100 + padding
        return ByteArray(lunghezza).also {
            it[0] = 0xFF.toByte(); it[1] = 0xFB.toByte(); it[2] = b2.toByte(); it[3] = 0x04
        }
    }

    @Test
    fun frameDa320kbps() {
        val a = frame(14, 0)
        val b = frame(14, 1)
        assertEquals(1044, a.size)
        assertEquals(1045, b.size)
        assertArrayEquals(intArrayOf(0, 1044, 2089), FrameMp3.inizi(a + b + a))
    }

    @Test
    fun byteNonValidiRifiutati() {
        assertThrows(IllegalStateException::class.java) { FrameMp3.inizi(frame(14, 0) + ByteArray(10)) }
    }
}
