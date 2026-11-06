package com.imash.app.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class DspTest {

    @Test
    fun fftUgualeADftDiretta() {
        val n = 64
        val r = Random(1)
        val x = DoubleArray(n) { r.nextDouble(-1.0, 1.0) }
        val re = x.copyOf()
        val im = DoubleArray(n)
        Fft(n).trasforma(re, im)
        for (k in 0 until n) {
            var sr = 0.0
            var si = 0.0
            for (t in 0 until n) {
                sr += x[t] * cos(-2 * PI * k * t / n)
                si += x[t] * sin(-2 * PI * k * t / n)
            }
            assertEquals(sr, re[k], 1e-9)
            assertEquals(si, im[k], 1e-9)
        }
    }

    @Test
    fun stftNumeroFrameComeLibrosa() {
        // librosa: 1 + len(y) // hop con center=True
        assertEquals(1 + 22050 / 512, Stft(2048, 512).numeroFrame(22050))
    }

    /** Clic brevi (rumore smorzato) a [bpm] battiti al minuto. */
    private fun clic(bpm: Double, secondi: Double, sr: Int = 22050): FloatArray {
        val r = Random(7)
        val y = FloatArray((secondi * sr).toInt())
        val periodo = 60.0 / bpm * sr
        var t = 0.5 * sr
        while (t < y.size) {
            val i0 = t.toInt()
            for (k in 0 until 800) if (i0 + k < y.size) y[i0 + k] += (r.nextFloat() * 2 - 1) * exp(-k / 120.0).toFloat()
            t += periodo
        }
        return y
    }

    private fun verificaTempo(bpm: Double) {
        val b = BeatTracker(22050).analizza(clic(bpm, 30.0))
        assertEquals("tempo stimato", bpm, b.bpm, bpm * 0.04)
        assertTrue("troppi pochi battiti: ${b.tempi.size}", b.tempi.size > 30 * bpm / 60 * 0.8)
        val intervalli = DoubleArray(b.tempi.size - 1) { b.tempi[it + 1] - b.tempi[it] }
        val mediana = intervalli.sorted()[intervalli.size / 2]
        assertEquals(60 / bpm, mediana, 0.03)
    }

    @Test fun battitiA120Bpm() = verificaTempo(120.0)

    @Test fun battitiA100Bpm() = verificaTempo(100.0)

    @Test fun battitiA140Bpm() = verificaTempo(140.0)

    @Test
    fun silenzioNessunBattito() {
        assertEquals(0, BeatTracker(22050).analizza(FloatArray(22050 * 5)).tempi.size)
    }

    // ---------- Croma, tonalità, filtro ----------

    private fun seno(f: Double, sr: Int = 22050) = FloatArray(sr * 2) { (0.5 * sin(2 * PI * f * it / sr)).toFloat() }

    @Test
    fun cromaDiUnaNota() {
        val croma = Croma(22050, 4096)
        for ((f, nota) in listOf(440.0 to 9, 261.63 to 0, 196.0 to 7)) {
            val frame = croma.calcola(seno(f))[10]
            assertEquals("nota a $f Hz", nota, frame.indices.maxBy { frame[it] })
            assertEquals(1f, frame[nota], 1e-6f)
        }
    }

    @Test
    fun camelotECompatibilita() {
        assertEquals(8, Tonalita(0, false).camelot)      // Do magg = 8B
        assertEquals(11, Tonalita(9, false).camelot)     // La magg = 11B
        assertEquals(10, Tonalita(11, true).camelot)     // Si min = 10A
        assertEquals(11, Tonalita(6, true).camelot)      // Fa# min = 11A
        assertTrue(Tonalita(0, false).compatibileCon(Tonalita(7, false)))   // Do – Sol
        assertTrue(Tonalita(9, false).compatibileCon(Tonalita(6, true)))    // La – Fa# min (relativa)
        assertTrue(!Tonalita(0, false).compatibileCon(Tonalita(2, false)))  // Do – Re
        assertTrue(!Tonalita(11, true).compatibileCon(Tonalita(9, false)))  // Si min – La magg
    }

    @Test
    fun stimaDellaTonalita() {
        // Croma = profilo di La maggiore e di Mi minore (ruotati sulla tonica).
        val magg = doubleArrayOf(6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88)
        val min = doubleArrayOf(6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17)
        assertEquals(Tonalita(9, false), Tonalita.stima(DoubleArray(12) { magg[(it - 9).mod(12)] }))
        assertEquals(Tonalita(4, true), Tonalita.stima(DoubleArray(12) { min[(it - 4).mod(12)] }))
        assertEquals(null, Tonalita.stima(DoubleArray(12)))
    }

    @Test
    fun passaBassoTieneIBassiETaglieGliAlti() {
        fun ampiezza(f: Double): Double {
            val filtro = PassaBasso(44100, 180.0)
            val y = FloatArray(44100) { filtro.filtra(sin(2 * PI * f * it / 44100).toFloat()) }
            return y.drop(22050).maxOf { abs(it).toDouble() }
        }
        assertEquals(1.0, ampiezza(40.0), 0.02)
        assertEquals(1 / kotlin.math.sqrt(2.0), ampiezza(180.0), 0.02)   // -3 dB alla frequenza di taglio
        assertTrue(ampiezza(2000.0) < 0.01)
    }

    @Test
    fun convoluzioneStessaLunghezza() {
        val y = BeatTracker.convolviStessa(doubleArrayOf(0.0, 1.0, 0.0), doubleArrayOf(1.0, 2.0, 3.0))
        assertEquals(listOf(1.0, 2.0, 3.0), y.toList())
        assertTrue(abs(y.sum() - 6.0) < 1e-12)
    }
}
