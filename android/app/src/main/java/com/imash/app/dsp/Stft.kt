package com.imash.app.dsp

import kotlin.math.PI
import kotlin.math.cos

/**
 * Spettrogramma di potenza |STFT|², come `np.abs(librosa.stft(y, n_fft, hop)) ** 2`
 * con i default di librosa: finestra di Hann periodica, center=True con zeri ai bordi.
 * I frame vengono consegnati uno alla volta, per non tenere in memoria tutto lo spettrogramma.
 * Usato da [BeatTracker] (n_fft 2048) e dal riconoscimento del ritornello (chroma).
 */
class Stft(val nFft: Int, val hop: Int = nFft / 4) {

    private val fft = Fft(nFft)
    private val finestra = DoubleArray(nFft) { 0.5 - 0.5 * cos(2 * PI * it / nFft) }
    private val re = DoubleArray(nFft)
    private val im = DoubleArray(nFft)
    private val potenza = FloatArray(nFft / 2 + 1)

    /** Numero di frame prodotti per un segnale di [campioni] campioni (come librosa). */
    fun numeroFrame(campioni: Int) = 1 + campioni / hop

    /** Chiama [frame] con (indice, potenza) per ogni frame; l'array viene riusato. */
    fun perFrame(y: FloatArray, frame: (Int, FloatArray) -> Unit) {
        val meta = nFft / 2
        for (t in 0 until numeroFrame(y.size)) {
            val inizio = t * hop - meta                     // center=True
            for (i in 0 until nFft) {
                val k = inizio + i
                re[i] = if (k in y.indices) y[k] * finestra[i] else 0.0
                im[i] = 0.0
            }
            fft.trasforma(re, im)
            for (f in potenza.indices) potenza[f] = (re[f] * re[f] + im[f] * im[f]).toFloat()
            frame(t, potenza)
        }
    }
}
