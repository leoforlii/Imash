package com.imash.app.mashup

import android.content.Context
import android.net.Uri
import com.imash.app.audio.LettoreAudio
import com.imash.app.audio.Mp3Encoder
import java.io.File
import kotlin.math.abs

/** Fasi mostrate nella schermata di elaborazione (design: Analisi, Taglio, Unione). */
enum class Fase { Analisi, Taglio, Unione }

sealed interface RisultatoMashup {
    class Creato(val piano: Piano, val analisi: List<AnalisiBrano>, val piuCorto: Boolean) : RisultatoMashup
    class TroppeCanzoni(val totale: Double, val massimo: Double) : RisultatoMashup
}

/**
 * Crea il mashup "Canzoni intere" dai brani [brani], nell'ordine dato, in [uscita] (mp3).
 * 1. Analisi di ogni brano (silenzio, volume, battiti/misure).
 * 2. Transizione per ogni coppia con [SceltaTransizione], piano delle durate con [DurationPlanner].
 * 3. Primo passaggio del [MashupRenderer] per trovare il picco (fase "Taglio").
 * 4. Secondo passaggio, scalato al picco 0,95 e codificato in mp3 (fase "Unione").
 */
class GeneratoreMashup(private val context: Context, private val brani: List<Uri>) {

    fun genera(
        durataObiettivo: Double,
        uscita: File,
        avanzamento: (Fase, brano: Int) -> Unit,
    ): RisultatoMashup {
        val analisi = brani.mapIndexed { i, uri ->
            avanzamento(Fase.Analisi, i)
            AnalizzatoreBrano(SR).also { a -> LettoreAudio.leggi(context, uri, SR, 2, ricevitore = a) }.concludi()
        }

        val daPianificare = analisi.map {
            BranoDaPianificare(it.misure?.firstOrNull() ?: it.inizioSuono, it.fineSuono, it.misure, it.spb)
        }
        val transizioni = SceltaTransizione.scegliTutte(analisi)
        val piano = when (val esito = DurationPlanner.pianifica(daPianificare, transizioni, durataObiettivo)) {
            is EsitoPiano.TroppeCanzoni -> return RisultatoMashup.TroppeCanzoni(esito.totale, esito.massimo)
            is EsitoPiano.Ok -> esito
        }

        val guadagni = FloatArray(brani.size) { i ->
            val s = piano.piano.segmenti[i]
            MashupRenderer.guadagno(analisi[i].rms(s.inizio, s.inizio + s.durata))
        }
        val sorgente = SorgenteBrani { i, max, ricevitore ->
            LettoreAudio.leggi(context, brani[i], SR, 2, max, ricevitore)
        }
        val renderer = MashupRenderer(piano.piano, guadagni, sorgente, SR)

        var picco = 0f
        renderer.renderizza({ b, n ->
            for (c in 0..1) for (i in 0 until n) picco = maxOf(picco, abs(b[c][i]))
        }) { avanzamento(Fase.Taglio, it) }

        val scala = if (picco > 0f) MashupRenderer.PICCO / picco else 1f
        uscita.outputStream().buffered().use { file ->
            Mp3Encoder(SR, 2, file).use { encoder ->
                renderer.renderizza({ b, n ->
                    for (c in 0..1) for (i in 0 until n) b[c][i] *= scala
                    encoder.codifica(b, n)
                }) { avanzamento(Fase.Unione, it) }
            }
        }
        return RisultatoMashup.Creato(piano.piano, analisi, piano.piuCorto)
    }

    companion object {
        const val SR = 44_100              // SR di mashup.py
        const val DURATA_PREDEFINITA = 600.0
    }
}
