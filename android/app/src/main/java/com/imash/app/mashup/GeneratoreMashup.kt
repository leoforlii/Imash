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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

/** Fasi mostrate nella schermata di elaborazione (design: Analisi, Taglio, Unione). */
enum class Fase { Analisi, Taglio, Unione }

/** Avanzamento del [GeneratoreMashup], per la schermata di elaborazione e la notifica. */
sealed interface Passo {
    data class AnalisiIniziata(val brano: Int) : Passo
    /** [secondi] di audio del brano già decodificati (ogni ~[SECONDI_LETTURA] s). */
    data class Lettura(val brano: Int, val secondi: Double) : Passo
    data class AnalisiFinita(val brano: Int, val analisi: AnalisiBrano) : Passo
    data class Pianificato(val piano: Piano) : Passo
    /** Il mix è arrivato al brano [brano]. */
    data class Taglio(val brano: Int) : Passo
    /** Frazione del mix già codificata in mp3, da 0 a 1. */
    data class Unione(val frazione: Double) : Passo

    companion object {
        const val SECONDI_LETTURA = 5.0
    }
}

/** Le due modalità del design: brani interi (mashup.py) o solo i ritornelli (estrai_ritornelli.py + mashup.py). */
enum class Modalita { CanzoniIntere, Ritornelli }

sealed interface RisultatoMashup {
    class Creato(val piano: Piano, val analisi: List<AnalisiBrano>, val piuCorto: Boolean) : RisultatoMashup
    class TroppeCanzoni(val totale: Double, val massimo: Double) : RisultatoMashup
}

/**
 * Crea il mashup dai brani [brani], nell'ordine dato, in [uscita] (mp3), nella [modalita] scelta.
 * Ogni brano viene decodificato una sola volta; i file intermedi stanno nella cache finché non si
 * chiama [pulisci], così si può ricomporre il mashup con altre transizioni senza rifare l'analisi.
 * 1. Analisi (silenzio, volume, battiti, tonalità e, per i Ritornelli, il ritornello) di più brani
 *    in parallelo; intanto ogni brano viene salvato come PCM 16 bit nella cache.
 * 2. Transizione per ogni coppia con [SceltaTransizione], piano delle durate con [DurationPlanner].
 * 3. Mix con il [MashupRenderer], letto dalla cache e salvato a sua volta come PCM (fase "Taglio").
 * 4. Mix riletto, scalato al picco 0,95 e codificato in mp3 (fase "Unione").
 * Si può interrompere annullando la coroutine: il controllo avviene a ogni blocco letto.
 */
class GeneratoreMashup(
    private val context: Context,
    private val brani: List<Uri>,
    private val modalita: Modalita = Modalita.CanzoniIntere,
) {

    private val lavoro = cartellaLavoro(context)
    private val pcm = List(brani.size) { File(lavoro, "brano$it.pcm") }
    private var analizzati: Pair<List<AnalisiBrano>, FloatArray>? = null

    /**
     * Crea il mashup. La prima volta analizza i brani; le volte successive riusa l'analisi e i
     * brani già decodificati e rifà solo pianificazione, mix e codifica, con le transizioni
     * [scelte] dall'utente (null = scelta automatica).
     */
    suspend fun genera(
        durataObiettivo: Double,
        uscita: File,
        scelte: List<TipoTransizione?> = emptyList(),
        avanzamento: (Passo) -> Unit,
    ): RisultatoMashup {
        val (analisi, picchi) = analizzati ?: try {
            lavoro.deleteRecursively()
            lavoro.mkdirs()
            analizzaInParallelo(pcm, avanzamento).also { analizzati = it }
        } catch (e: Throwable) {
            pulisci()
            throw e
        }
        return componi(analisi, picchi, durataObiettivo, uscita, scelte, avanzamento)
    }

    /** Cancella i file intermedi: dopo, [genera] rifà l'analisi. */
    fun pulisci() {
        lavoro.deleteRecursively()
        analizzati = null
    }

    private suspend fun componi(
        analisi: List<AnalisiBrano>,
        picchi: FloatArray,
        durataObiettivo: Double,
        uscita: File,
        scelte: List<TipoTransizione?>,
        avanzamento: (Passo) -> Unit,
    ): RisultatoMashup {
        val contesto = currentCoroutineContext()
        val daPianificare = analisi.map(::daPianificare)
        val transizioni = SceltaTransizione.scegliTutte(analisi).mapIndexed { i, t ->
            val tipo = scelte.getOrNull(i)
            if (tipo == null || tipo == t.tipo) t else SceltaTransizione.conTipo(analisi[i], analisi[i + 1], tipo)
        }
        val piano = when (val esito = DurationPlanner.pianifica(daPianificare, transizioni, durataObiettivo)) {
            is EsitoPiano.TroppeCanzoni -> return RisultatoMashup.TroppeCanzoni(esito.totale, esito.massimo)
            is EsitoPiano.Ok -> esito
        }
        avanzamento(Passo.Pianificato(piano.piano))

        val guadagni = FloatArray(brani.size) { i ->
            val s = piano.piano.segmenti[i]
            MashupRenderer.guadagno(analisi[i].rms(s.inizio, s.inizio + s.durata))
        }
        val renderer = MashupRenderer(piano.piano, guadagni, { i, max, ricevitore ->
            LettorePcm.leggi(pcm[i], max) { b, n ->
                contesto.ensureActive()
                ricevitore.ricevi(b, n)
            }
        }, SR)

        // Il mix può superare ±1 prima della normalizzazione: per salvarlo a 16 bit senza
        // tagliarlo lo si riduce prima del limite massimo possibile, poi lo si riporta al picco 0,95.
        val mix = File(lavoro, "mix.pcm")
        val riduzione = 1f / MashupRenderer.limiteMix(guadagni, picchi)
        val scrittore = ScrittorePcm(mix, riduzione)
        scrittore.use { renderer.renderizza(it) { b -> avanzamento(Passo.Taglio(b)) } }
        if (scrittore.saturati > 0) Log.w("Imash", "Mix: ${scrittore.saturati} campioni limitati a ±1")

        val scala = if (scrittore.picco > 0f) MashupRenderer.PICCO / scrittore.picco else 1f
        val totale = renderer.campioniTotali
        var codificati = 0L
        uscita.outputStream().buffered().use { file ->
            Mp3Encoder(SR, 2, file).use { encoder ->
                avanzamento(Passo.Unione(0.0))
                val passo = SR * 2L                 // un aggiornamento ogni 2 s di mix
                LettorePcm.leggi(mix, scala = scala) { b, n ->
                    contesto.ensureActive()
                    encoder.codifica(b, n)
                    val prima = codificati / passo
                    codificati += n
                    if (codificati / passo != prima) avanzamento(Passo.Unione(codificati.toDouble() / totale))
                }
            }
        }
        mix.delete()
        return RisultatoMashup.Creato(piano.piano, analisi, piano.piuCorto)
    }

    /**
     * Decodifica e analizza i brani, al massimo [PARALLELISMO] alla volta, salvando ognuno in
     * [pcm]. Restituisce le analisi e il picco di ogni brano, nell'ordine dei brani.
     */
    private suspend fun analizzaInParallelo(
        pcm: List<File>,
        avanzamento: (Passo) -> Unit,
    ): Pair<List<AnalisiBrano>, FloatArray> = coroutineScope {
        val esiti = brani.mapIndexed { i, uri ->
            async(Dispatchers.Default.limitedParallelism(PARALLELISMO)) {
                avanzamento(Passo.AnalisiIniziata(i))
                val contesto = currentCoroutineContext()
                val analizzatore = AnalizzatoreBrano(SR, cercaRitornello = modalita == Modalita.Ritornelli)
                val scrittore = ScrittorePcm(pcm[i])
                val passo = (Passo.SECONDI_LETTURA * SR).toLong()
                var letti = 0L
                scrittore.use {
                    LettoreAudio.leggi(context, uri, SR, 2) { b, n ->
                        contesto.ensureActive()
                        analizzatore.ricevi(b, n)
                        it.ricevi(b, n)
                        val prima = letti / passo
                        letti += n
                        if (letti / passo != prima) avanzamento(Passo.Lettura(i, letti.toDouble() / SR))
                    }
                }
                val analisi = analizzatore.concludi()
                avanzamento(Passo.AnalisiFinita(i, analisi))
                analisi to scrittore.picco
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
        /** Cartella dei file intermedi nella cache dell'app. */
        fun cartellaLavoro(context: Context) = File(context.cacheDir, "lavoro")

        const val SR = 44_100              // SR di mashup.py

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
