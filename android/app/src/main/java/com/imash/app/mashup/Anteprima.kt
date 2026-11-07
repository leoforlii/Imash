package com.imash.app.mashup

import com.imash.app.dsp.RilevatoreRitornello

/**
 * Stima della durata del mashup prima dell'analisi, per la barra di anteprima della schermata
 * dei brani: in "Canzoni intere" conta tutto il brano, nei "Ritornelli" al massimo la finestra
 * di 60 s. Le transizioni non sono ancora note, quindi è un po' più lunga del risultato:
 * il controllo vero resta quello di [DurationPlanner.pianifica].
 */
class Anteprima(
    /** Quanto conta ogni brano, in secondi, nell'ordine della lista. */
    val durate: List<Double>,
    val obiettivo: Double,
) {
    val totale: Double = durate.sum()
    val massimo: Double = obiettivo * DurationPlanner.MARGINE_MASSIMO

    /** Oltre il +20%: avviso "Troppe canzoni" e "Crea mashup" disattivato. */
    val troppe: Boolean get() = totale > massimo + 1e-9

    /** Anche con tutto il materiale non si arriva alla durata scelta. */
    val piuCorto: Boolean get() = totale < obiettivo - 1e-9

    companion object {
        fun di(durateBrani: List<Double>, modalita: Modalita, obiettivo: Double) = Anteprima(
            durateBrani.map { if (modalita == Modalita.Ritornelli) minOf(it, RilevatoreRitornello.MAX_CLIP_SEC) else it },
            obiettivo,
        )
    }
}
