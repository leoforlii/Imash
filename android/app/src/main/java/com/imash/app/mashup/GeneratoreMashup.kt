package com.imash.app.mashup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.imash.app.audio.LettoreAudio
import com.imash.app.audio.LettorePcm
import com.imash.app.audio.Mp3Encoder
import com.imash.app.audio.ScrittorePcm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** Fasi mostrate nella schermata di elaborazione (design: Analisi, Taglio, Unione). */
enum class Fase { Analisi, Taglio, Unione }

/** Le due modalità del design: brani interi (mashup.py) o solo i ritornelli (estrai_ritornelli.py + mashup.py). */
enum class Modalita { CanzoniIntere, Ritornelli }

sealed interface RisultatoMashup {
    class Creato(val piano: Piano, val analisi: List<AnalisiBrano>, val piuCorto: Boolean) : RisultatoMashup
    class TroppeCanzoni(val totale: Double, val massimo: Double) : RisultatoMashup
}

/**
 * Crea il mashup dai brani [brani], nell'ordine dato, in [uscita] (mp3), nella [modalita] scelta.
 * Ogni brano viene decodificato una sola volta; i file intermedi stanno nella cache e vengono
 * cancellati alla fine.
 * 1. Analisi (silenzio, volume, battiti, tonalità e, per i Ritornelli, il ritornello) di più brani
 *    in parallelo; intanto ogni brano viene salvato come PCM 16 bit nella cache.
 * 2. Transizione per ogni coppia con [SceltaTransizione], piano delle durate con [DurationPlanner].
 * 3. Mix con il [MashupRenderer], letto dalla cache e salvato a sua volta come PCM (fase "Taglio").
 * 4. Mix riletto, scalato al picco 0,95 e codificato in mp3 (fase "Unione").
 */
class GeneratoreMashup(
    private val context: Context,
    private val brani: List<Uri>,
    private val modalita: Modalita = Modalita.CanzoniIntere,
) {

    suspend fun genera(
        durataObiettivo: Double,
        uscita: File,
        avanzamento: (Fase, brano: Int) -> Unit,
    ): RisultatoMashup {
        val lavoro = File(context.cacheDir, "lavoro").apply { deleteRecursively(); mkdirs() }
        try {
            return genera(durataObiettivo, uscita, lavoro, avanzamento)
        } finally {
            lavoro.deleteRecursively()
        }
    }

    private suspend fun genera(
        durataObiettivo: Double,
        uscita: File,
        lavoro: File,
        avanzamento: (Fase, brano: Int) -> Unit,
    ): RisultatoMashup {
        val pcm = List(brani.size) { File(lavoro, "brano$it.pcm") }
        val (analisi, picchi) = analizzaInParallelo(pcm, avanzamento)

        val daPianificare = analisi.map(::daPianificare)
        val transizioni = SceltaTransizione.scegliTutte(analisi)
        val piano = when (val esito = DurationPlanner.pianifica(daPianificare, transizioni, durataObiettivo)) {
            is EsitoPiano.TroppeCanzoni -> return RisultatoMashup.TroppeCanzoni(esito.totale, esito.massimo)
            is EsitoPiano.Ok -> esito
        }

        val guadagni = FloatArray(brani.size) { i ->
            val s = piano.piano.segmenti[i]
            MashupRenderer.guadagno(analisi[i].rms(s.inizio, s.inizio + s.durata))
        }
        val renderer = MashupRenderer(piano.piano, guadagni, { i, max, ricevitore ->
            LettorePcm.leggi(pcm[i], max, ricevitore = ricevitore)
        }, SR)

        // Il mix può superare ±1 prima della normalizzazione: per salvarlo a 16 bit senza
        // tagliarlo lo si riduce prima del limite massimo possibile, poi lo si riporta al picco 0,95.
        val mix = File(lavoro, "mix.pcm")
        val riduzione = 1f / MashupRenderer.limiteMix(guadagni, picchi)
        val scrittore = ScrittorePcm(mix, riduzione)
        scrittore.use { renderer.renderizza(it) { b -> avanzamento(Fase.Taglio, b) } }
        if (scrittore.saturati > 0) Log.w("Imash", "Mix: ${scrittore.saturati} campioni limitati a ±1")
        pcm.forEach { it.delete() }

        val scala = if (scrittore.picco > 0f) MashupRenderer.PICCO / scrittore.picco else 1f
        val totale = renderer.campioniTotali
        var codificati = 0L
        uscita.outputStream().buffered().use { file ->
            Mp3Encoder(SR, 2, file).use { encoder ->
                avanzamento(Fase.Unione, 0)
                LettorePcm.leggi(mix, scala = scala) { b, n ->
                    encoder.codifica(b, n)
                    val prima = (codificati * brani.size / totale).toInt()
                    codificati += n
                    val dopo = (codificati * brani.size / totale).toInt().coerceAtMost(brani.size - 1)
                    if (dopo != prima) avanzamento(Fase.Unione, dopo)
                }
            }
        }
        return RisultatoMashup.Creato(piano.piano, analisi, piano.piuCorto)
    }

    /**
     * Decodifica e analizza i brani, al massimo [PARALLELISMO] alla volta, salvando ognuno in
     * [pcm]. Restituisce le analisi e il picco di ogni brano, nell'ordine dei brani.
     */
    private suspend fun analizzaInParallelo(
        pcm: List<File>,
        avanzamento: (Fase, brano: Int) -> Unit,
    ): Pair<List<AnalisiBrano>, FloatArray> = coroutineScope {
        val conclusi = AtomicInteger()
        avanzamento(Fase.Analisi, 0)
        val esiti = brani.mapIndexed { i, uri ->
            async(Dispatchers.Default.limitedParallelism(PARALLELISMO)) {
                val analizzatore = AnalizzatoreBrano(SR, cercaRitornello = modalita == Modalita.Ritornelli)
                val scrittore = ScrittorePcm(pcm[i])
                scrittore.use {
                    LettoreAudio.leggi(context, uri, SR, 2) { b, n ->
                        analizzatore.ricevi(b, n)
                        it.ricevi(b, n)
                    }
                }
                val risultato = analizzatore.concludi() to scrittore.picco
                avanzamento(Fase.Analisi, conclusi.incrementAndGet().coerceAtMost(brani.size - 1))
                risultato
            }
        }.awaitAll()
        esiti.map { it.first } to FloatArray(esiti.size) { esiti[it].second }
    }

    private fun daPianificare(a: AnalisiBrano): BranoDaPianificare {
        val r = a.ritornello
        return if (modalita == Modalita.Ritornelli && r != null) tratto(a, r.inizio, minOf(r.fine, a.fineSuono))
        else BranoDaPianificare(a.misure?.firstOrNull() ?: a.inizioSuono, a.fineSuono, a.misure, a.spb)
    }

    companion object {
        const val SR = 44_100              // SR di mashup.py
        const val DURATA_PREDEFINITA = 600.0

        /** Brani analizzati insieme: uno per core, al massimo 4 per non esaurire la memoria. */
        val PARALLELISMO = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
    }
}

/**
 * Tratto di un brano da [inizio] a [fine] (per i Ritornelli): parte dalla misura più vicina a
 * [inizio], così il brano entrante resta sul battere, e tiene solo le misure fino a [fine].
 * Senza almeno 2 misure nel tratto si taglia a secondi.
 */
internal fun tratto(a: AnalisiBrano, inizio: Double, fine: Double): BranoDaPianificare {
    val misure = a.misure
    if (misure != null && a.spb != null) {
        val primo = misure.filter { it < fine }.minByOrNull { kotlin.math.abs(it - inizio) }
        if (primo != null) {
            val dentro = misure.filter { it >= primo - 1e-9 && it <= fine + 1e-9 }.toDoubleArray()
            if (dentro.size >= 2) return BranoDaPianificare(primo, fine, dentro, a.spb)
        }
    }
    return BranoDaPianificare(inizio, fine, null, a.spb)
}
