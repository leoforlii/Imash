package com.imash.app.dsp

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Banco di filtri Mel come `librosa.filters.mel` con i default (scala Slaney, norm="slaney",
 * fmin = 0, fmax = sr/2). Applicato a un frame di potenza restituisce le [nMel] bande.
 */
class Mel(sr: Int, nFft: Int, val nMel: Int = 128) {

    // Per ogni banda: primo bin e pesi (solo la parte non nulla del triangolo).
    private val primo = IntArray(nMel)
    private val pesi = arrayOfNulls<FloatArray>(nMel)

    init {
        val bins = nFft / 2 + 1
        val freqFft = DoubleArray(bins) { it.toDouble() * sr / nFft }
        val melMax = hzAMel(sr / 2.0)
        val freqMel = DoubleArray(nMel + 2) { melAHz(melMax * it / (nMel + 1)) }
        for (m in 0 until nMel) {
            val basso = freqMel[m]
            val centro = freqMel[m + 1]
            val alto = freqMel[m + 2]
            val norma = 2.0 / (alto - basso)
            val valori = DoubleArray(bins) { k ->
                val f = freqFft[k]
                max(0.0, min((f - basso) / (centro - basso), (alto - f) / (alto - centro))) * norma
            }
            val da = valori.indexOfFirst { it > 0 }.coerceAtLeast(0)
            val a = valori.indexOfLast { it > 0 }.coerceAtLeast(da)
            primo[m] = da
            pesi[m] = FloatArray(a - da + 1) { valori[da + it].toFloat() }
        }
    }

    fun applica(potenza: FloatArray, uscita: FloatArray) {
        for (m in 0 until nMel) {
            val p = pesi[m]!!
            val base = primo[m]
            var s = 0f
            for (i in p.indices) s += p[i] * potenza[base + i]
            uscita[m] = s
        }
    }

    private companion object {
        // Scala Slaney: lineare sotto 1 kHz, logaritmica sopra.
        const val F_SP = 200.0 / 3
        const val MIN_LOG_HZ = 1000.0
        const val MIN_LOG_MEL = MIN_LOG_HZ / F_SP
        val PASSO_LOG = ln(6.4) / 27.0

        fun hzAMel(f: Double) =
            if (f < MIN_LOG_HZ) f / F_SP else MIN_LOG_MEL + ln(f / MIN_LOG_HZ) / PASSO_LOG

        fun melAHz(m: Double) =
            if (m < MIN_LOG_MEL) m * F_SP else MIN_LOG_HZ * exp(PASSO_LOG * (m - MIN_LOG_MEL))
    }
}
