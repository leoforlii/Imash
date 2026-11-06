package com.imash.app.mashup

import com.imash.app.audio.RicevitorePcm
import com.imash.app.dsp.PassaBasso
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

/** Legge il brano [indice] a 44,1 kHz stereo, fermandosi dopo [maxCampioni]. */
fun interface SorgenteBrani {
    fun leggi(indice: Int, maxCampioni: Long, ricevitore: RicevitorePcm)
}

/**
 * Costruisce il mashup a flusso, come la parte finale di `mashup.py`:
 * ogni segmento viene portato allo stesso volume (`normalizza_rms`), unito al precedente con
 * la transizione del piano, poi fade-in di 1 s e fade-out di 4 s:
 * - Mix a tempo e Dissolvenza: dissolvenza equal-power (`crossfade`);
 * - Scambio dei bassi: gli alti fanno la dissolvenza equal-power, i bassi (sotto [FREQ_BASSI])
 *   passano dal brano uscente all'entrante in un battito, a metà della transizione.
 * Si tiene in memoria solo la coda del brano precedente (al massimo ~20 s).
 * La normalizzazione finale del picco si fa chiamando [renderizza] due volte (vedi [GeneratoreMashup]).
 */
class MashupRenderer(
    private val piano: Piano,
    /** Moltiplicatore di volume di ogni segmento (0,12 / RMS del segmento). */
    private val guadagni: FloatArray,
    private val sorgente: SorgenteBrani,
    private val sr: Int,
) {
    private val inizi = LongArray(piano.segmenti.size) { (piano.segmenti[it].inizio * sr).roundToLong() }
    private val lunghezze = LongArray(piano.segmenti.size) { (piano.segmenti[it].durata * sr).roundToLong() }
    private val dissolvenze = LongArray(piano.dissolvenze.size) { (piano.dissolvenze[it] * sr).roundToLong() }

    /** Lunghezza esatta del mashup in campioni. */
    val campioniTotali: Long = lunghezze.sum() - dissolvenze.sum()

    private val fadeIn = minOf((FADE_IN_SEC * sr).toLong(), campioniTotali)
    private val fadeOut = minOf((FADE_OUT_SEC * sr).toLong(), campioniTotali)

    fun renderizza(uscita: RicevitorePcm, avanzamento: (brano: Int) -> Unit = {}) {
        val emettitore = Emettitore(uscita)
        var coda = Array(2) { FloatArray(0) }                 // coda del brano precedente, già col suo volume

        for (b in piano.segmenti.indices) {
            avanzamento(b)
            val lunghezza = lunghezze[b]
            val xfIngresso = if (b > 0) dissolvenze[b - 1].toInt() else 0
            val scambio = b > 0 && piano.transizioni[b - 1] == TipoTransizione.ScambioBassi
            val bassiCoda = Array(2) { PassaBasso(sr, FREQ_BASSI) }
            val bassiNuovo = Array(2) { PassaBasso(sr, FREQ_BASSI) }
            val rampaBassi = maxOf(1, xfIngresso / 16)            // 1 battito su 4 misure
            val inizioScambio = xfIngresso / 2 - rampaBassi / 2
            val xfUscita = if (b < dissolvenze.size) dissolvenze[b].toInt() else 0
            val inizioCoda = lunghezza - xfUscita
            val nuovaCoda = Array(2) { FloatArray(xfUscita) }
            val g = guadagni[b]
            var k = 0L                                         // posizione nel segmento
            var letti = 0L                                     // posizione nel file

            fun campione(sx: Float, dx: Float) {
                val l = sx * g
                val r = dx * g
                when {
                    k < xfIngresso -> {
                        // Equal-power: t da 0 a π/2 inclusi, come np.linspace(0, π/2, n).
                        val t = if (xfIngresso > 1) PI / 2 * k / (xfIngresso - 1) else PI / 2
                        val c = cos(t).toFloat()
                        val s = sin(t).toFloat()
                        val i = k.toInt()
                        if (scambio) {
                            val q = ((k - inizioScambio).toFloat() / rampaBassi).coerceIn(0f, 1f)
                            emettitore.emetti(
                                scambiaBassi(coda[0][i], l, bassiCoda[0], bassiNuovo[0], c, s, q),
                                scambiaBassi(coda[1][i], r, bassiCoda[1], bassiNuovo[1], c, s, q),
                            )
                        } else {
                            emettitore.emetti(coda[0][i] * c + l * s, coda[1][i] * c + r * s)
                        }
                    }
                    k >= inizioCoda -> {
                        val i = (k - inizioCoda).toInt()
                        nuovaCoda[0][i] = l
                        nuovaCoda[1][i] = r
                    }
                    else -> emettitore.emetti(l, r)
                }
                k++
            }

            sorgente.leggi(b, inizi[b] + lunghezza) { blocco, n ->
                for (i in 0 until n) {
                    if (letti++ < inizi[b]) continue
                    if (k < lunghezza) campione(blocco[0][i], blocco[1][i])
                }
            }
            while (k < lunghezza) campione(0f, 0f)            // file più corto del previsto
            coda = nuovaCoda
        }
        emettitore.svuota()
    }

    /** Alti in equal-power ([c], [s]); bassi dal vecchio al nuovo secondo [q] (0 → 1). */
    private fun scambiaBassi(
        vecchio: Float, nuovo: Float, filtroVecchio: PassaBasso, filtroNuovo: PassaBasso,
        c: Float, s: Float, q: Float,
    ): Float {
        val bv = filtroVecchio.filtra(vecchio)
        val bn = filtroNuovo.filtra(nuovo)
        return (vecchio - bv) * c + (nuovo - bn) * s + bv * (1 - q) + bn * q
    }

    /** Accumula i campioni in blocchi, applica fade-in e fade-out sulla posizione globale. */
    private inner class Emettitore(private val uscita: RicevitorePcm) {
        private val blocco = Array(2) { FloatArray(BLOCCO) }
        private var n = 0
        private var posizione = 0L

        fun emetti(sx: Float, dx: Float) {
            var f = 1f
            if (posizione < fadeIn) f = if (fadeIn > 1) posizione.toFloat() / (fadeIn - 1) else 1f
            val daFine = campioniTotali - 1 - posizione
            if (daFine < fadeOut) f *= if (fadeOut > 1) daFine.toFloat() / (fadeOut - 1) else 0f
            blocco[0][n] = sx * f
            blocco[1][n] = dx * f
            posizione++
            if (++n == BLOCCO) svuota()
        }

        fun svuota() {
            if (n > 0) uscita.ricevi(blocco, n)
            n = 0
        }
    }

    companion object {
        const val FADE_IN_SEC = 1.0
        const val FADE_OUT_SEC = 4.0
        const val RMS_OBIETTIVO = 0.12     // `normalizza_rms(y, target=0.12)`
        const val PICCO = 0.95f            // `mashup *= 0.95 / np.max(np.abs(mashup))`
        const val FREQ_BASSI = 180.0       // Hz: cassa e basso
        private const val BLOCCO = 4096

        fun guadagno(rms: Double): Float = (RMS_OBIETTIVO / (rms + 1e-9)).toFloat()
    }
}
