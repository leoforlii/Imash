package com.imash.app.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** FFT complessa radix-2 in loco, per lunghezze potenza di 2. Tabelle precalcolate per [n]. */
class Fft(val n: Int) {
    init {
        require(n >= 2 && n and (n - 1) == 0) { "n deve essere una potenza di 2" }
    }

    private val coseni = DoubleArray(n / 2) { cos(-2 * PI * it / n) }
    private val seni = DoubleArray(n / 2) { sin(-2 * PI * it / n) }
    private val inverti = IntArray(n).also { tab ->
        var bit = 0
        while (1 shl bit < n) bit++
        for (i in 0 until n) tab[i] = Integer.reverse(i) ushr (32 - bit)
    }

    /** Trasformata diretta: [re] e [im] (lunghezza n) vengono sovrascritti. */
    fun trasforma(re: DoubleArray, im: DoubleArray) {
        for (i in 0 until n) {
            val j = inverti[i]
            if (j > i) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var lunghezza = 2
        while (lunghezza <= n) {
            val meta = lunghezza / 2
            val passoTab = n / lunghezza
            var inizio = 0
            while (inizio < n) {
                for (k in 0 until meta) {
                    val c = coseni[k * passoTab]
                    val s = seni[k * passoTab]
                    val a = inizio + k
                    val b = a + meta
                    val tr = re[b] * c - im[b] * s
                    val ti = re[b] * s + im[b] * c
                    re[b] = re[a] - tr; im[b] = im[a] - ti
                    re[a] += tr; im[a] += ti
                }
                inizio += lunghezza
            }
            lunghezza *= 2
        }
    }
}
