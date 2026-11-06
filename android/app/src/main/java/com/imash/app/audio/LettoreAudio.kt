package com.imash.app.audio

import android.content.Context
import android.net.Uri

/**
 * Legge un brano e lo consegna, a flusso, alla frequenza e al numero di canali richiesti.
 * Equivale a `librosa.load(path, sr=…, mono=…)` degli script, ed è l'unico punto dell'app
 * che legge i file audio. Con [maxCampioni] la lettura si ferma appena ne ha consegnati tanti.
 */
object LettoreAudio {

    fun leggi(
        context: Context,
        uri: Uri,
        sampleRate: Int,
        canali: Int,
        maxCampioni: Long = Long.MAX_VALUE,
        ricevitore: RicevitorePcm,
    ) {
        require(canali == 1 || canali == 2) { "Canali supportati: 1 o 2" }
        var consegnati = 0L
        val limitato = RicevitorePcm { blocco, n ->
            val daDare = minOf(n.toLong(), maxCampioni - consegnati).toInt()
            if (daDare > 0) ricevitore.ricevi(blocco, daDare)
            consegnati += daDare
        }
        var resampler: Resampler? = null
        var convertito = Array(canali) { FloatArray(0) }

        Mp3Decoder.decodifica(context, uri) { info, blocco, campioni ->
            val r = resampler ?: Resampler(info.sampleRate, sampleRate, canali, limitato).also { resampler = it }
            if (convertito[0].size < campioni) convertito = Array(canali) { FloatArray(campioni) }
            adattaCanali(blocco, campioni, convertito)
            r.processa(convertito, campioni)
            consegnati < maxCampioni
        }
        if (consegnati < maxCampioni) resampler?.fine()
    }

    /**
     * Converte i canali come `librosa`: mono = media dei canali;
     * stereo da mono = canale duplicato (come `carica` in mashup.py); oltre 2 canali si tengono i primi 2.
     */
    internal fun adattaCanali(da: Array<FloatArray>, campioni: Int, a: Array<FloatArray>) {
        when {
            a.size == 1 -> {
                val m = a[0]
                for (i in 0 until campioni) {
                    var s = 0f
                    for (c in da.indices) s += da[c][i]
                    m[i] = s / da.size
                }
            }
            da.size == 1 -> {
                System.arraycopy(da[0], 0, a[0], 0, campioni)
                System.arraycopy(da[0], 0, a[1], 0, campioni)
            }
            else -> {
                System.arraycopy(da[0], 0, a[0], 0, campioni)
                System.arraycopy(da[1], 0, a[1], 0, campioni)
            }
        }
    }
}
