package com.imash.app.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class ResamplerTest {

    private fun seno(freq: Double, sr: Int, campioni: Int) =
        FloatArray(campioni) { i -> sin(2 * PI * freq * i / sr).toFloat() }

    /** Converte [ingresso] (mono) a blocchi di [blocco] campioni e restituisce l'uscita intera. */
    private fun converti(ingresso: FloatArray, daSr: Int, aSr: Int, blocco: Int = 1000): FloatArray {
        val out = ArrayList<Float>()
        val r = Resampler(daSr, aSr, 1) { b, n -> for (i in 0 until n) out.add(b[0][i]) }
        var i = 0
        while (i < ingresso.size) {
            val n = minOf(blocco, ingresso.size - i)
            r.processa(arrayOf(ingresso.copyOfRange(i, i + n)), n)
            i += n
        }
        r.fine()
        return out.toFloatArray()
    }

    private fun rms(a: FloatArray, da: Int, a2: Int): Double =
        sqrt((da until a2).sumOf { a[it].toDouble() * a[it] } / (a2 - da))

    private fun passaggiPerZero(a: FloatArray, da: Int, a2: Int) =
        (da + 1 until a2).count { (a[it - 1] < 0) != (a[it] < 0) }

    @Test
    fun durataConservata() {
        val out = converti(FloatArray(48_000), 48_000, 44_100)
        assertEquals(44_100, out.size)
    }

    @Test
    fun senoDa48kA44kMantieneFrequenzaEAmpiezza() {
        val out = converti(seno(1000.0, 48_000, 48_000), 48_000, 44_100)
        // Un secondo di 1 kHz = 2000 passaggi per lo zero; si misura nella parte centrale.
        val zeri = passaggiPerZero(out, 4410, 44_100 - 4410)
        assertEquals(2000 * 0.8, zeri.toDouble(), 2.0)
        assertEquals(1 / sqrt(2.0), rms(out, 4410, 44_100 - 4410), 0.01)
    }

    @Test
    fun da44kA22kTagliaLeFrequenzeSopraNyquist() {
        // 15 kHz non è rappresentabile a 22050 Hz: deve essere quasi eliminato (no aliasing).
        val out = converti(seno(15_000.0, 44_100, 44_100), 44_100, 22_050)
        assertTrue("residuo troppo alto", rms(out, 2205, 22_050 - 2205) < 0.01)
    }

    @Test
    fun aBlocchiComeTuttoInsieme() {
        val ingresso = seno(440.0, 48_000, 20_000)
        val aBlocchi = converti(ingresso, 48_000, 44_100, blocco = 333)
        val tutto = converti(ingresso, 48_000, 44_100, blocco = ingresso.size)
        assertArrayEquals(tutto, aBlocchi, 1e-6f)
    }

    @Test
    fun stessaFrequenzaPassaInvariato() {
        val ingresso = seno(440.0, 44_100, 5_000)
        assertArrayEquals(ingresso, converti(ingresso, 44_100, 44_100), 0f)
    }

    @Test
    fun monoEMediaDeiCanali() {
        val a = arrayOf(floatArrayOf(1f, 0f), floatArrayOf(0f, -1f))
        val m = arrayOf(FloatArray(2))
        LettoreAudio.adattaCanali(a, 2, m)
        assertArrayEquals(floatArrayOf(0.5f, -0.5f), m[0], 0f)
        assertTrue(abs(m[0][0] - 0.5f) < 1e-9)
    }
}
