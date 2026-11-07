package com.imash.app.dsp

import com.imash.app.mashup.AnalisiBrano
import com.imash.app.mashup.tratto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.abs

class RitornelloTest {

    private val fps = 22_050.0 / (RilevatoreRitornello.N_FFT / 4)     // ~5,38 frame al secondo

    /** Croma di un accordo casuale: 12 valori in [0, 1] con massimo 1, come dopo la normalizzazione. */
    private fun accordo(rnd: Random): FloatArray {
        val c = FloatArray(12) { rnd.nextFloat() * 0.4f }
        repeat(3) { c[rnd.nextInt(12)] = 1f }
        return c
    }

    /** [secondi] di musica: un accordo ogni 2 s preso da [accordi] (ripetuti ciclicamente). */
    private fun sezione(accordi: List<FloatArray>, secondi: Int, rnd: Random): List<FloatArray> =
        List((secondi * fps).toInt()) { f ->
            val base = accordi[(f / (2 * fps)).toInt() % accordi.size]
            FloatArray(12) { (base[it] + rnd.nextFloat() * 0.05f).coerceAtMost(1f) }
        }

    @Test
    fun trovaIlRitornelloRipetuto() {
        val rnd = Random(3)
        val ritornello = List(20) { accordo(rnd) }                 // 40 s, sempre uguale
        fun strofa() = List(15) { accordo(rnd) }                   // 30 s, ogni volta diversa
        val parti = listOf(30 to strofa(), 40 to ritornello, 30 to strofa(), 40 to ritornello,
            30 to strofa(), 40 to ritornello, 30 to strofa())
        val croma = parti.flatMap { (sec, accordi) -> sezione(accordi, sec, rnd) }.toTypedArray()
        val durata = croma.size / fps

        val r = RilevatoreRitornello.trova(croma, durata, 30.0)
        assertNotNull("nessun ritornello trovato", r)
        val inizi = listOf(30.0, 100.0, 170.0)
        assertTrue("inizio ${r!!.inizio} lontano dai ritornelli", inizi.any { abs(it - r.inizio) <= 4.0 })
        assertTrue("troppo corto: ${r.fine - r.inizio}", r.fine - r.inizio >= 30.0)
        assertTrue(r.fine - r.inizio <= RilevatoreRitornello.MAX_CLIP_SEC + 1e-9)
    }

    @Test
    fun ripiegoSulTrattoPiuForte() {
        val sr = 100
        val y = FloatArray(60 * sr) { i -> if (i in 31 * sr until 51 * sr) 0.9f else 0.1f }
        val t = RilevatoreRitornello.segmentoPiuForte(y, sr)
        assertEquals(31.0, t.inizio, 1e-9)
        assertEquals(51.0, t.fine, 1e-9)
        // Brano più corto della finestra: tutto il brano.
        assertEquals(5.0, RilevatoreRitornello.segmentoPiuForte(FloatArray(5 * sr), sr).fine, 1e-9)
    }

    @Test
    fun bordiRiflessiComeScipy() {
        // mode="reflect": d c b a | a b c d | d c b a
        val n = 4
        assertArrayEquals(
            intArrayOf(2, 1, 0, 0, 1, 2, 3, 3, 2, 1, 0, 0),
            IntArray(12) { RilevatoreRitornello.rifletti(it - 4 + 1, n) },
        )
    }

    @Test
    fun trattoParteDallaMisuraPiuVicina() {
        val misure = DoubleArray(40) { 1.0 + it * 2.0 }            // una misura ogni 2 s da 1 s
        val a = AnalisiBrano(
            durata = 90.0, inizioSuono = 0.0, fineSuono = 90.0, misure = misure, spb = 0.5, bpm = 120.0,
            tonalita = null, energia = FloatArray(1), secondiPerBlocco = 1.0,
        )
        val b = tratto(a, 20.3, 80.3)
        assertEquals(21.0, b.inizio, 1e-9)                        // 21 è più vicino di 19
        assertEquals(21.0, b.misure!!.first(), 1e-9)
        assertTrue(b.misure.last() <= 80.3)
        assertEquals(80.3, b.fineMassima, 1e-9)
        // Senza battiti: si taglia a secondi dal punto trovato.
        val senza = AnalisiBrano(90.0, 0.0, 90.0, null, null, 0.0, null, FloatArray(1), 1.0)
        assertEquals(20.3, tratto(senza, 20.3, 80.3).inizio, 1e-9)
    }
}
