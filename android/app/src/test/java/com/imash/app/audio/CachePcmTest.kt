package com.imash.app.audio

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

class CachePcmTest {

    private fun temporaneo() = File.createTempFile("imash", ".pcm").apply { deleteOnExit() }

    private fun leggiTutto(file: File, max: Long = Long.MAX_VALUE, scala: Float = 1f): Array<FloatArray> {
        val sx = ArrayList<Float>()
        val dx = ArrayList<Float>()
        LettorePcm.leggi(file, max, scala) { b, n -> for (i in 0 until n) { sx.add(b[0][i]); dx.add(b[1][i]) } }
        return arrayOf(sx.toFloatArray(), dx.toFloatArray())
    }

    @Test
    fun andataERitornoA16Bit() {
        val n = 10_000                                  // più blocchi di lettura, l'ultimo incompleto
        val sx = FloatArray(n) { sin(2 * PI * 440 * it / 44_100).toFloat() * 0.8f }
        val dx = FloatArray(n) { -sx[it] / 2 }
        val file = temporaneo()
        val scrittore = ScrittorePcm(file)
        scrittore.use {
            it.ricevi(arrayOf(sx.copyOfRange(0, 3000), dx.copyOfRange(0, 3000)), 3000)
            it.ricevi(arrayOf(sx.copyOfRange(3000, n), dx.copyOfRange(3000, n)), n - 3000)
        }
        val letto = leggiTutto(file)
        assertEquals(n, letto[0].size)
        for (i in 0 until n) {
            assertEquals(sx[i], letto[0][i], 1f / 32767)
            assertEquals(dx[i], letto[1][i], 1f / 32767)
        }
        assertEquals(0.8f, scrittore.picco, 1e-3f)
        assertEquals(0L, scrittore.saturati)
    }

    @Test
    fun scalaInScritturaELetturaELimite() {
        val file = temporaneo()
        val scrittore = ScrittorePcm(file, scala = 0.5f)
        scrittore.use { it.ricevi(arrayOf(floatArrayOf(1.5f, 3f, -1f), floatArrayOf(0f, 0f, 0f)), 3) }
        assertEquals(1.5f, scrittore.picco, 1e-6f)     // 3 × 0,5: misurato prima del limite
        assertEquals(1L, scrittore.saturati)
        val letto = leggiTutto(file, scala = 2f)
        assertEquals(1.5f, letto[0][0], 1e-3f)
        assertEquals(2f, letto[0][1], 1e-3f)           // limitato a 1 in scrittura
        assertEquals(-1f, letto[0][2], 1e-3f)
    }

    @Test
    fun siFermaAlMassimoRichiesto() {
        val file = temporaneo()
        ScrittorePcm(file).use { it.ricevi(arrayOf(FloatArray(9000), FloatArray(9000)), 9000) }
        assertEquals(5000, leggiTutto(file, max = 5000)[0].size)
    }
}
