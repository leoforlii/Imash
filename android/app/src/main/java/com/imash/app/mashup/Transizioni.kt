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
 * L'utente può cambiarla nella schermata del risultato ([conTipo]): il pianificatore riceve solo la lista.
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

    /** Si può scegliere fra Mix a tempo e Scambio dei bassi solo se entrambi i brani hanno i battiti. */
    fun modificabile(uscente: AnalisiBrano, entrante: AnalisiBrano) = uscente.spb != null && entrante.spb != null

    /**
     * Transizione di tipo [tipo] scelto dall'utente. Il Mix a tempo usa 8 misure se restano entro
     * [MIX_MASSIMO_SEC], altrimenti 4 (anche se i tempi non sono abbastanza vicini: l'ha voluto l'utente).
     * Senza battiti resta la dissolvenza.
     */
    fun conTipo(uscente: AnalisiBrano, entrante: AnalisiBrano, tipo: TipoTransizione): Transizione {
        val sa = uscente.spb
        if (!modificabile(uscente, entrante) || sa == null) return Transizione(TipoTransizione.Dissolvenza, 2)
        return when (tipo) {
            TipoTransizione.MixATempo -> Transizione(tipo, MISURE_MIX.firstOrNull { 4 * it * sa <= MIX_MASSIMO_SEC } ?: MISURE_MIX.last())
            TipoTransizione.ScambioBassi -> Transizione(tipo, MISURE_SCAMBIO)
            TipoTransizione.Dissolvenza -> Transizione(tipo, 2)
        }
    }
}
