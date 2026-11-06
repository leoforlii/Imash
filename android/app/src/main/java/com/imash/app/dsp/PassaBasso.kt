package com.imash.app.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Filtro passa-basso Butterworth del 2° ordine (biquad, formule di R. Bristow-Johnson, Q = 1/√2)
 * su un canale. La parte alta si ottiene per differenza (x - basso), così le due parti
 * sommate ridanno esattamente il segnale: serve allo scambio dei bassi del mashup.
 */
class PassaBasso(sr: Int, frequenza: Double) {

    private val b0: Double
    private val b1: Double
    private val a1: Double
    private val a2: Double
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    init {
        val w0 = 2 * PI * frequenza / sr
        val alfa = sin(w0) / (2 / kotlin.math.sqrt(2.0))
        val a0 = 1 + alfa
        b0 = (1 - cos(w0)) / 2 / a0          // b2 = b0
        b1 = (1 - cos(w0)) / a0
        a1 = -2 * cos(w0) / a0
        a2 = (1 - alfa) / a0
    }

    fun filtra(x: Float): Float {
        val y = b0 * x + b1 * x1 + b0 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x.toDouble()
        y2 = y1
        y1 = y
        return y.toFloat()
    }
}
