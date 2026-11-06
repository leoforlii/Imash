package com.imash.app.dsp

import kotlin.math.exp
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Cromagramma come `librosa.feature.chroma_stft` con tuning = 0: i filtri di
 * `librosa.filters.chroma` (pesi gaussiani, ctroct = 5, octwidth = 2, norm 2, base Do)
 * applicati al |STFT|², poi ogni frame normalizzato al suo massimo (norm = inf).
 * Usato per tonalità e battere; servirà anche al riconoscimento del ritornello (fase 4).
 */
class Croma(sr: Int, nFft: Int, hop: Int = nFft / 4) {

    private val stft = Stft(nFft, hop)
    private val bins = nFft / 2 + 1
    private val pesi: Array<FloatArray>

    val framePerSecondo = sr.toDouble() / hop

    init {
        // frqbins = 12 · log2(f / (440/16)); il bin 0 (0 Hz) prende frqbins[1] - 1,5 · 12.
        val frq = DoubleArray(bins + 1)
        for (k in 1..bins) frq[k] = N * log2(k.toDouble() * sr / nFft / (440.0 / 16))
        frq[0] = frq[1] - 1.5 * N
        val w = Array(N) { FloatArray(bins) }
        val colonna = DoubleArray(N)
        for (k in 0 until bins) {
            val larghezza = max(frq[k + 1] - frq[k], 1.0)
            var norma = 0.0
            for (c in 0 until N) {
                val d = (frq[k] - c + N / 2 + 10 * N).mod(N.toDouble()) - N / 2
                colonna[c] = exp(-0.5 * (2 * d / larghezza).pow(2))
                norma += colonna[c] * colonna[c]
            }
            val ottava = exp(-0.5 * ((frq[k] / N - CENTRO_OTTAVA) / LARGHEZZA_OTTAVE).pow(2))
            for (c in 0 until N) w[c][k] = (colonna[c] / sqrt(norma) * ottava).toFloat()
        }
        // base_c: np.roll(-3) → la riga 0 diventa il Do (prima era il La).
        pesi = Array(N) { c -> w[(c + 3) % N] }
    }

    /** Cromagramma di [y] (mono): 12 valori per frame (0 = Do), massimo del frame = 1. */
    fun calcola(y: FloatArray): Array<FloatArray> {
        val uscita = Array(stft.numeroFrame(y.size)) { FloatArray(N) }
        stft.perFrame(y) { t, potenza ->
            val c = uscita[t]
            var massimo = 0f
            for (i in 0 until N) {
                val p = pesi[i]
                var s = 0f
                for (k in 0 until bins) s += p[k] * potenza[k]
                c[i] = s
                if (s > massimo) massimo = s
            }
            if (massimo > 0f) for (i in 0 until N) c[i] /= massimo
        }
        return uscita
    }

    private companion object {
        const val N = 12
        const val CENTRO_OTTAVA = 5.0
        const val LARGHEZZA_OTTAVE = 2.0
    }
}
