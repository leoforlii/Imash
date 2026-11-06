package com.imash.app.mashup

import kotlin.math.abs

enum class TipoTransizione(val nome: String) {
    /** Battiti allineati e dissolvenza equal-power lunga: brani con tempo simile e tonalità compatibili. */
    MixATempo("Mix a tempo"),

    /** Gli alti si incrociano, i bassi si scambiano di colpo a metà: tonalità o energie diverse. */
    ScambioBassi("Scambio dei bassi"),

    /** Dissolvenza semplice di mashup.py: quando un brano non ha battiti affidabili. */
    Dissolvenza("Dissolvenza"),
}

/** Transizione tra due brani: tipo e lunghezza in misure del brano uscente. */
data class Transizione(val tipo: TipoTransizione, val misure: Int)

/**
 * Sceglie la transizione per ogni coppia di brani consecutivi in base a tempo e tonalità.
 * Nella fase 5 l'utente potrà cambiare la scelta: il pianificatore riceve solo la lista.
 */
object SceltaTransizione {

    /** Scarto massimo (s) tra i battiti dei due brani alla fine del mix, oltre si sente. */
    const val DERIVA_MASSIMA = 0.06
    const val MIX_MASSIMO_SEC = 20.0
    private val MISURE_MIX = intArrayOf(8, 4)
    private const val MISURE_SCAMBIO = 4

    fun scegli(uscente: AnalisiBrano, entrante: AnalisiBrano): Transizione {
        val sa = uscente.spb
        val sb = entrante.spb
        if (sa == null || sb == null) return Transizione(TipoTransizione.Dissolvenza, 2)
        val ta = uscente.tonalita
        val tb = entrante.tonalita
        if (ta != null && tb != null && ta.compatibileCon(tb)) {
            for (m in MISURE_MIX) {
                val battiti = 4 * m
                if (abs(sa - sb) * battiti <= DERIVA_MASSIMA && battiti * sa <= MIX_MASSIMO_SEC) {
                    return Transizione(TipoTransizione.MixATempo, m)
                }
            }
        }
        return Transizione(TipoTransizione.ScambioBassi, MISURE_SCAMBIO)
    }

    fun scegliTutte(analisi: List<AnalisiBrano>): List<Transizione> = analisi.zipWithNext(::scegli)
}
