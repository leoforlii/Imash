package com.imash.app.mashup

import kotlin.math.abs

/** Un brano visto dal pianificatore. Tempi in secondi dall'inizio del file. */
class BranoDaPianificare(
    /** Da dove parte il tratto usato: inizio del suono, o il primo battere se ci sono le misure. */
    val inizio: Double,
    /** Fin dove si può arrivare al massimo (fine del suono, o inizio + 60 s per i ritornelli). */
    val fineMassima: Double,
    /** Inizi di misura (istanti assoluti, il primo = [inizio]); null = taglio libero a secondi. */
    val misure: DoubleArray?,
    val spb: Double?,
)

/** Tratto del brano da usare: da [inizio] per [durata] secondi. */
data class Segmento(val inizio: Double, val durata: Double)

class Piano(
    val segmenti: List<Segmento>,
    /** dissolvenze[i] = durata in secondi della transizione tra il brano i e i+1. */
    val dissolvenze: DoubleArray,
    val transizioni: List<TipoTransizione> = List(dissolvenze.size) { TipoTransizione.Dissolvenza },
) {
    val totale: Double get() = segmenti.sumOf { it.durata } - dissolvenze.sum()
}

sealed interface EsitoPiano {
    /** [piuCorto]: anche usando tutto il materiale non si arriva alla durata scelta. */
    class Ok(val piano: Piano, val piuCorto: Boolean) : EsitoPiano

    /** Oltre il +20% della durata scelta: l'utente deve togliere brani o aumentare la durata. */
    class TroppeCanzoni(val totale: Double, val massimo: Double) : EsitoPiano
}

/**
 * Sceglie quanto usare di ogni brano perché il mashup duri circa [durataObiettivo].
 * È il ciclo di aggiustamento di `mashup.py` (si accorcia il brano più lungo o si allunga il
 * più corto finché il totale è entro la tolleranza), con due differenze:
 * - si prova prima a tagliare a **fine frase** (ogni [MISURE_FRASE] misure, o alla fine naturale
 *   del brano), e solo se così non si arriva alla durata si taglia su una misura qualunque;
 * - la transizione copre esattamente le ultime N misure del brano uscente, così i battiti
 *   restano allineati con il brano entrante (che parte sul battere).
 */
object DurationPlanner {

    const val TOLLERANZA = 15.0         // come in mashup.py (585-615 s su 600)
    const val MARGINE_MASSIMO = 1.2     // oltre il +20%: avviso "Troppe canzoni"
    const val MIN_SEGMENTO = 10.0       // come `validi = misure[misure > 10]`
    const val MISURE_FRASE = 8
    private const val XF_MISURE = 2     // dissolvenza semplice = 2 misure del brano uscente
    private const val PASSO_LIBERO = 4.0
    private const val ITERAZIONI = 400

    /** Dissolvenza semplice: 2 misure, tra 2,5 e 7 s (`xf_sec` di mashup.py). */
    fun dissolvenza(spb: Double?) = (XF_MISURE * 4 * (spb ?: 0.5)).coerceIn(2.5, 7.0)

    /** Durata prevista di una transizione dopo un brano con [spb] secondi per battito. */
    fun durataTransizione(t: Transizione, spb: Double?): Double =
        if (t.tipo == TipoTransizione.Dissolvenza || spb == null) dissolvenza(spb) else t.misure * 4 * spb

    fun pianifica(brani: List<BranoDaPianificare>, transizioni: List<Transizione>, durataObiettivo: Double): EsitoPiano {
        require(brani.isNotEmpty() && transizioni.size == brani.size - 1)
        val previste = DoubleArray(transizioni.size) { durataTransizione(transizioni[it], brani[it].spb) }

        val tutto = brani.sumOf { tagliPossibili(it, 1).last() } - previste.sum()
        val massimo = durataObiettivo * MARGINE_MASSIMO
        if (tutto > massimo + 1e-9) return EsitoPiano.TroppeCanzoni(tutto, massimo)

        val aFrase = cerca(brani, previste, durataObiettivo, MISURE_FRASE)
        val durate = if (abs(aFrase.sum() - previste.sum() - durataObiettivo) <= TOLLERANZA) aFrase
        else cerca(brani, previste, durataObiettivo, 1)

        val dissolvenze = DoubleArray(transizioni.size) { i ->
            val limite = minOf(durate[i], durate[i + 1]) / 2
            minOf(limite, durataEsatta(brani[i], durate[i], transizioni[i], limite))
        }
        val piano = Piano(
            brani.indices.map { Segmento(brani[it].inizio, durate[it]) },
            dissolvenze,
            transizioni.map { it.tipo },
        )
        return EsitoPiano.Ok(piano, piuCorto = piano.totale < durataObiettivo - TOLLERANZA)
    }

    /** Ciclo di mashup.py sui tagli possibili a passi di [passo] misure; restituisce le durate. */
    private fun cerca(brani: List<BranoDaPianificare>, previste: DoubleArray, obiettivo: Double, passo: Int): DoubleArray {
        val tagli = brani.map { tagliPossibili(it, passo) }
        val scelta = IntArray(brani.size) { tagli[it].lastIndex }   // indice del taglio scelto per brano
        fun durata(i: Int) = tagli[i][scelta[i]]
        for (giro in 0 until ITERAZIONI) {
            val tot = brani.indices.sumOf(::durata) - previste.sum()
            if (abs(tot - obiettivo) <= TOLLERANZA) break
            if (tot > obiettivo) {
                // Togli un passo al brano più lungo che si può accorciare.
                val i = brani.indices.filter { scelta[it] > 0 }.maxByOrNull(::durata) ?: break
                scelta[i]--
            } else {
                // Aggiungi un passo al brano più corto che ha ancora spazio.
                val i = brani.indices.filter { scelta[it] < tagli[it].lastIndex }.minByOrNull(::durata) ?: break
                scelta[i]++
            }
        }
        return DoubleArray(brani.size, ::durata)
    }

    /**
     * Durata reale della transizione dopo il brano [b] tagliato a [durata]: dall'inizio della
     * misura che sta N misure prima del taglio fino al taglio. Se supera [limite] si dimezza N.
     */
    private fun durataEsatta(b: BranoDaPianificare, durata: Double, t: Transizione, limite: Double): Double {
        val misure = b.misure
        if (t.tipo == TipoTransizione.Dissolvenza || misure == null || b.spb == null) return dissolvenza(b.spb)
        val fine = b.inizio + durata
        val j = misure.indices.minBy { abs(misure[it] - fine) }
        var n = t.misure
        while (n > 1 && (j - n < 0 || fine - misure[j - n] > limite)) n /= 2
        return if (j - n >= 0) fine - misure[j - n] else n * 4 * b.spb
    }

    /**
     * Durate possibili (crescenti) per un brano: fino all'inizio di una misura ogni [passo]
     * (contando dal primo battere) dopo [MIN_SEGMENTO], più la fine naturale del brano;
     * a passi di 4 s se i battiti non sono affidabili. Se il brano è troppo corto si usa tutto.
     */
    internal fun tagliPossibili(b: BranoDaPianificare, passo: Int = 1): DoubleArray {
        val disponibile = b.fineMassima - b.inizio
        val tagli = if (b.misure != null) {
            val tutte = b.misure.map { it - b.inizio }.filter { it <= disponibile + 1e-9 }
            tutte.filterIndexed { i, d -> d >= MIN_SEGMENTO && (i % passo == 0 || i == tutte.lastIndex) }
        } else {
            generateSequence(MIN_SEGMENTO) { it + PASSO_LIBERO }.takeWhile { it <= disponibile }.toList()
        }
        return if (tagli.isEmpty()) doubleArrayOf(disponibile) else tagli.toDoubleArray()
    }
}
