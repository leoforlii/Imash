package com.imash.app.servizio

import com.imash.app.mashup.Fase
import com.imash.app.mashup.MetodoRitornello
import com.imash.app.mashup.Modalita
import com.imash.app.mashup.Passo
import com.imash.app.mashup.RisultatoMashup

/**
 * A che punto è un brano. [fase] è la fase in corso o la prossima da fare (null = non ancora
 * iniziato); [attivo] = ci si sta lavorando adesso.
 */
data class AvanzamentoBrano(
    val fase: Fase? = null,
    val attivo: Boolean = false,
    val fatto: Boolean = false,
    /** Secondi già decodificati durante l'analisi. */
    val letti: Double = 0.0,
    val ritornelloNonTrovato: Boolean = false,
)

sealed interface Esito {
    class Pronto(val risultato: RisultatoMashup.Creato) : Esito
    class TroppeCanzoni(val totale: Double, val massimo: Double) : Esito
    class Errore(val messaggio: String) : Esito
    data object Annullato : Esito
}

/**
 * Stato di un'elaborazione: avanzamento di ogni brano ed esito (null finché è in corso).
 * Non dipende da Android (i file da leggere sono in [Elaborazione]), così si prova con i test.
 */
data class StatoElaborazione(
    val nomi: List<String>,
    /** Durata di ogni brano in secondi (0 se sconosciuta), per stimare l'avanzamento della lettura. */
    val durate: List<Double>,
    val modalita: Modalita,
    val inizio: Long,
    val avanzamento: List<AvanzamentoBrano> = List(nomi.size) { AvanzamentoBrano() },
    /** Fine di ogni brano nel mashup, in frazione della durata totale (noto dopo la pianificazione). */
    val confini: List<Double>? = null,
    /** Brano a cui è arrivato il mix (-1 = non ancora iniziato). */
    val taglio: Int = -1,
    /** Frazione già codificata in mp3 (-1 = non ancora iniziata). */
    val unione: Double = -1.0,
    val esito: Esito? = null,
    /** Mentre si ricompone il mashup con un'altra transizione: avanzamento da 0 a 1, altrimenti null. */
    val ricomposizione: Double? = null,
    /** Il mashup attuale è stato salvato con "Salva mp3". */
    val salvato: Boolean = false,
    /** Messaggio da mostrare una volta (per es. ricomposizione non riuscita), poi azzerato. */
    val messaggio: String? = null,
) {
    val inCorso: Boolean get() = esito == null

    val fase: Fase get() = when {
        unione >= 0 -> Fase.Unione
        taglio >= 0 -> Fase.Taglio
        else -> Fase.Analisi
    }

    /** Avanzamento complessivo da 0 a 1, con un peso per fase (l'analisi è la parte più lunga). */
    val frazione: Double get() {
        val analisi = avanzamento.indices.sumOf { i ->
            val a = avanzamento[i]
            when {
                a.fase != null && a.fase != Fase.Analisi -> 1.0
                a.attivo -> PESO_LETTURA * (if (durate[i] > 0) minOf(1.0, a.letti / durate[i]) else 0.5)
                else -> 0.0
            }
        } / nomi.size
        val tagliati = when {
            unione >= 0 -> 1.0
            taglio >= 0 -> taglio.toDouble() / nomi.size
            else -> 0.0
        }
        return PESO_ANALISI * analisi + PESO_TAGLIO * tagliati + PESO_UNIONE * maxOf(0.0, unione)
    }

    /** Brano da mostrare in "Brano X di N" (da 0). */
    val corrente: Int get() = when (fase) {
        Fase.Analisi -> avanzamento.count { it.fase != null && it.fase != Fase.Analisi }.coerceAtMost(nomi.size - 1)
        Fase.Taglio -> taglio
        Fase.Unione -> avanzamento.indexOfFirst { !it.fatto }.let { if (it < 0) nomi.size - 1 else it }
    }

    fun applica(passo: Passo): StatoElaborazione = when (passo) {
        is Passo.AnalisiIniziata -> cambia(passo.brano) { it.copy(fase = Fase.Analisi, attivo = true) }
        is Passo.Lettura -> cambia(passo.brano) { it.copy(letti = passo.secondi) }
        is Passo.AnalisiFinita -> cambia(passo.brano) {
            it.copy(
                fase = Fase.Taglio, attivo = false,
                ritornelloNonTrovato = passo.analisi.ritornello?.metodo == MetodoRitornello.Energia,
            )
        }
        is Passo.Pianificato -> if (ricomposizione != null) this else {
            val p = passo.piano
            var fine = 0.0
            val confini = p.segmenti.indices.map { i ->
                fine += p.segmenti[i].durata - (if (i > 0) p.dissolvenze[i - 1] else 0.0)
                fine / p.totale
            }
            copy(confini = confini)
        }
        is Passo.Taglio -> if (ricomposizione != null) copy(ricomposizione = PESO_RICOMPOSIZIONE_TAGLIO * passo.brano / nomi.size) else copy(
            taglio = passo.brano,
            avanzamento = avanzamento.mapIndexed { i, a ->
                when {
                    i < passo.brano -> a.copy(fase = Fase.Unione, attivo = false)
                    i == passo.brano -> a.copy(fase = Fase.Taglio, attivo = true)
                    else -> a
                }
            },
        )
        is Passo.Unione -> if (ricomposizione != null) {
            copy(ricomposizione = PESO_RICOMPOSIZIONE_TAGLIO + (1 - PESO_RICOMPOSIZIONE_TAGLIO) * passo.frazione)
        } else {
            val f = passo.frazione
            val attivo = confini?.indexOfFirst { f < it - 1e-9 } ?: -1
            copy(
                unione = f,
                avanzamento = avanzamento.mapIndexed { i, a ->
                    val fatto = confini != null && f >= confini[i] - 1e-9
                    a.copy(fase = Fase.Unione, fatto = fatto, attivo = i == attivo)
                },
            )
        }
    }

    /** Tutti i brani uniti, a elaborazione conclusa con successo. */
    fun concluso(esito: Esito): StatoElaborazione =
        if (esito is Esito.Pronto) copy(esito = esito, unione = 1.0, avanzamento = avanzamento.map { it.copy(fatto = true, attivo = false) })
        else copy(esito = esito, avanzamento = avanzamento.map { it.copy(attivo = false) })

    private fun cambia(i: Int, f: (AvanzamentoBrano) -> AvanzamentoBrano) =
        copy(avanzamento = avanzamento.mapIndexed { j, a -> if (j == i) f(a) else a })

    companion object {
        // Pesi delle fasi, dalle prove sull'emulatore (l'analisi prende la maggior parte del tempo).
        const val PESO_ANALISI = 0.6
        const val PESO_TAGLIO = 0.25
        const val PESO_UNIONE = 0.15
        /** Parte dell'analisi di un brano occupata dalla decodifica (il resto: battiti, croma, ritornello). */
        private const val PESO_LETTURA = 0.7
        /** Nella ricomposizione c'è solo mix e codifica. */
        private const val PESO_RICOMPOSIZIONE_TAGLIO = 0.6
    }
}
