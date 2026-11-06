package com.imash.app.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Risultato del rilevamento: tempo stimato e istanti dei battiti in secondi. */
class Battiti(val bpm: Double, val tempi: DoubleArray)

/**
 * Porting di `librosa.beat.beat_track` (default di librosa 0.10):
 * onset strength su spettrogramma Mel → stima del tempo con autocorrelazione e prior
 * log-normale attorno a 120 BPM → programmazione dinamica di Ellis (2007) → taglio dei
 * battiti deboli ai bordi. Lavora su audio mono a [sr] Hz (nell'app 22050, come librosa).
 */
class BeatTracker(private val sr: Int) {

    private val hop = HOP
    private val frameRate = sr.toDouble() / hop

    fun analizza(y: FloatArray): Battiti {
        val onset = forzaAttacchi(y)
        if (onset.none { it > 0 }) return Battiti(0.0, DoubleArray(0))
        val bpm = stimaTempo(onset)
        val frame = traccia(onset, bpm)
        return Battiti(bpm, DoubleArray(frame.size) { frame[it] * hop.toDouble() / sr })
    }

    /** `librosa.onset.onset_strength`: flusso spettrale positivo medio sulle bande Mel in dB. */
    internal fun forzaAttacchi(y: FloatArray): DoubleArray {
        val stft = Stft(N_FFT, hop)
        val mel = Mel(sr, N_FFT, N_MEL)
        val nFrame = stft.numeroFrame(y.size)
        val db = Array(nFrame) { FloatArray(N_MEL) }
        val bande = FloatArray(N_MEL)
        var massimo = Float.NEGATIVE_INFINITY
        stft.perFrame(y) { t, potenza ->
            mel.applica(potenza, bande)
            for (m in 0 until N_MEL) {
                val v = (10 * log10(max(1e-10, bande[m].toDouble()))).toFloat()
                db[t][m] = v
                if (v > massimo) massimo = v
            }
        }
        val soglia = massimo - TOP_DB                       // power_to_db(top_db=80)
        val onset = DoubleArray(nFrame)
        val ritardo = 1 + N_FFT / (2 * hop)                 // lag 1 + compensazione di center=True
        for (t in 1 until nFrame) {
            var s = 0.0
            for (m in 0 until N_MEL) {
                val d = max(db[t][m], soglia) - max(db[t - 1][m], soglia)
                if (d > 0) s += d
            }
            val dest = t - 1 + ritardo
            if (dest < nFrame) onset[dest] = s / N_MEL
        }
        return onset
    }

    /** `librosa.feature.tempo`: tempogramma di autocorrelazione medio × prior log-normale. */
    internal fun stimaTempo(onset: DoubleArray): Double {
        val win = (AC_SIZE * sr / hop).toInt()             // time_to_frames(8 s)
        val meta = win / 2
        val n = onset.size
        // Padding "linear_ramp" verso 0 ai due lati (center=True).
        val pad = DoubleArray(n + 2 * meta) { i ->
            when {
                i < meta -> onset[0] * i / meta
                i >= meta + n -> onset[n - 1] * (n + 2 * meta - 1 - i) / meta
                else -> onset[i - meta]
            }
        }
        val finestra = DoubleArray(win) { 0.5 - 0.5 * cos(2 * PI * it / win) }
        var nFft = 1
        while (nFft < 2 * win) nFft *= 2
        val fft = Fft(nFft)
        val re = DoubleArray(nFft)
        val im = DoubleArray(nFft)
        val media = DoubleArray(win)
        for (t in 0 until n) {
            for (i in 0 until nFft) {
                re[i] = if (i < win) pad[t + i] * finestra[i] else 0.0
                im[i] = 0.0
            }
            fft.trasforma(re, im)
            // Autocorrelazione = antitrasformata di |X|²; per un segnale reale basta
            // trasformare di nuovo lo spettro di potenza (pari) e riscalare.
            for (i in 0 until nFft) {
                re[i] = re[i] * re[i] + im[i] * im[i]
                im[i] = 0.0
            }
            fft.trasforma(re, im)
            var picco = 0.0
            for (lag in 0 until win) picco = max(picco, abs(re[lag]))
            if (picco > 1e-12) for (lag in 0 until win) media[lag] += re[lag] / picco
        }
        var migliore = 1
        var punteggio = Double.NEGATIVE_INFINITY
        for (lag in 1 until win) {
            val bpm = 60.0 * sr / (hop * lag)
            if (bpm >= MAX_TEMPO) continue
            val prior = -0.5 * ((log2(bpm) - log2(START_BPM)) / STD_BPM).let { it * it }
            val s = ln(1 + 1e6 * media[lag] / n) + prior
            if (s > punteggio) {
                punteggio = s
                migliore = lag
            }
        }
        return 60.0 * sr / (hop * migliore)
    }

    /** `__beat_tracker`: restituisce gli indici di frame dei battiti. */
    internal fun traccia(onset: DoubleArray, bpm: Double): IntArray {
        val periodo = (60.0 * frameRate / bpm).roundToInt().coerceAtLeast(1)
        val locale = punteggioLocale(onset, periodo)
        val n = locale.size
        val cumulato = DoubleArray(n)
        val indietro = IntArray(n) { -1 }
        val sogliaPrimo = 0.01 * locale.max()
        var primoBattito = true
        cumulato[0] = locale[0]
        val logPeriodo = ln(periodo.toDouble())
        for (i in 1 until n) {
            var meglio = Double.NEGATIVE_INFINITY
            var pos = -1
            var loc = i - (periodo / 2.0).roundToInt()
            while (loc >= i - 2 * periodo && loc >= 0) {
                val d = ln((i - loc).toDouble()) - logPeriodo
                val s = cumulato[loc] - TIGHTNESS * d * d
                if (s > meglio) {
                    meglio = s
                    pos = loc
                }
                loc--
            }
            cumulato[i] = if (pos >= 0) locale[i] + meglio else locale[i]
            if (primoBattito && locale[i] < sogliaPrimo) {
                indietro[i] = -1
            } else {
                indietro[i] = pos
                primoBattito = false
            }
        }
        // Ultimo battito: ultimo massimo locale sopra metà della mediana dei massimi.
        val massimi = (0 until n).filter { i ->
            val sx = if (i > 0) cumulato[i - 1] else cumulato[i]
            val dx = if (i < n - 1) cumulato[i + 1] else cumulato[i]
            cumulato[i] > sx && cumulato[i] >= dx
        }
        if (massimi.isEmpty()) return IntArray(0)
        val mediana = massimi.map { cumulato[it] }.sorted().let { v ->
            if (v.size % 2 == 1) v[v.size / 2] else (v[v.size / 2 - 1] + v[v.size / 2]) / 2
        }
        val coda = massimi.last { cumulato[it] >= 0.5 * mediana }
        val battiti = ArrayList<Int>()
        var b = coda
        while (b >= 0) {
            battiti.add(b)
            b = indietro[b]
        }
        battiti.reverse()
        return tagliaDeboli(locale, battiti.toIntArray())
    }

    private fun punteggioLocale(onset: DoubleArray, periodo: Int): DoubleArray {
        val media = onset.average()
        val std = sqrt(onset.sumOf { (it - media) * (it - media) } / (onset.size - 1).coerceAtLeast(1))
        val norm = DoubleArray(onset.size) { onset[it] / (std + 1e-300) }
        val finestra = DoubleArray(2 * periodo + 1) { k ->
            val x = (k - periodo) * 32.0 / periodo
            exp(-0.5 * x * x)
        }
        return convolviStessa(norm, finestra)
    }

    /** `__trim_beats`: scarta i battiti iniziali e finali più deboli. */
    private fun tagliaDeboli(locale: DoubleArray, battiti: IntArray): IntArray {
        if (battiti.isEmpty()) return battiti
        val forza = DoubleArray(battiti.size) { locale[battiti[it]] }
        val liscia = convolviStessa(forza, doubleArrayOf(0.0, 0.5, 1.0, 0.5, 0.0)) // hann(5)
        val soglia = 0.5 * sqrt(liscia.sumOf { it * it } / liscia.size)
        val da = liscia.indexOfFirst { it > soglia }
        val a = liscia.indexOfLast { it > soglia }
        return if (da < 0) IntArray(0) else battiti.copyOfRange(da, a + 1)
    }

    companion object {
        const val HOP = 512
        private const val N_FFT = 2048
        private const val N_MEL = 128
        private const val TOP_DB = 80f
        private const val AC_SIZE = 8.0
        private const val START_BPM = 120.0
        private const val STD_BPM = 1.0
        private const val MAX_TEMPO = 320.0
        private const val TIGHTNESS = 100.0

        /** Convoluzione con uscita della stessa lunghezza di [x], come `convolve(mode="same")`. */
        internal fun convolviStessa(x: DoubleArray, h: DoubleArray): DoubleArray {
            val centro = (h.size - 1) / 2
            return DoubleArray(x.size) { i ->
                var s = 0.0
                for (k in h.indices) {
                    val j = i + centro - k
                    if (j in x.indices) s += x[j] * h[k]
                }
                s
            }
        }
    }
}
