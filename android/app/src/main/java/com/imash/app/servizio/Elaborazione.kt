package com.imash.app.servizio

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.imash.app.data.Archivio
import com.imash.app.data.BranoScelto
import com.imash.app.mashup.GeneratoreMashup
import com.imash.app.mashup.Modalita
import com.imash.app.mashup.TipoTransizione
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * L'elaborazione in corso o l'ultimo mashup creato (uno alla volta), condivisi fra le schermate
 * e [ServizioMashup]. Vive quanto il processo dell'app: il servizio in primo piano lo tiene vivo
 * a schermo spento.
 */
object Elaborazione {

    private val _stato = MutableStateFlow<StatoElaborazione?>(null)
    val stato: StateFlow<StatoElaborazione?> = _stato.asStateFlow()

    /**
     * Il generatore tiene analisi e brani decodificati per ricomporre senza rifare l'analisi.
     * Riceve solo l'application context, che vive quanto il processo: nessuna perdita di memoria.
     */
    @SuppressLint("StaticFieldLeak")
    internal var generatore: GeneratoreMashup? = null
        private set
    internal var durataObiettivo = 0.0
        private set

    /** Transizione scelta dall'utente per ogni passaggio (null = automatica). */
    internal var scelte: List<TipoTransizione?> = emptyList()

    /** Il lavoro del servizio, per poterlo annullare. */
    internal var lavoro: Job? = null

    /** Avvia il servizio; ignorato se c'è già un'elaborazione in corso. */
    fun avvia(context: Context, brani: List<BranoScelto>, modalita: Modalita, durataObiettivo: Double) {
        if (occupato()) return
        generatore?.pulisci()
        generatore = GeneratoreMashup(context.applicationContext, brani.map { it.uri }, modalita)
        this.durataObiettivo = durataObiettivo
        scelte = List(maxOf(0, brani.size - 1)) { null }
        _stato.value = StatoElaborazione(
            nomi = brani.map { it.nome },
            durate = brani.map { it.durata },
            modalita = modalita,
            inizio = System.currentTimeMillis(),
        )
        avviaServizio(context)
    }

    /** Ricrea il mashup pronto con la transizione [tipo] al [passaggio] (fra il brano i e i+1). */
    fun ricomponi(context: Context, passaggio: Int, tipo: TipoTransizione) {
        val s = _stato.value ?: return
        if (s.esito !is Esito.Pronto || occupato() || passaggio !in scelte.indices) return
        scelte = scelte.toMutableList().also { it[passaggio] = tipo }
        aggiorna { it.copy(ricomposizione = 0.0, messaggio = null) }
        avviaServizio(context)
    }

    fun annulla() {
        lavoro?.cancel()
    }

    fun segnaSalvato() = aggiorna { it.copy(salvato = true) }

    fun messaggioMostrato() = aggiorna { it.copy(messaggio = null) }

    /** Dimentica il mashup ("Nuovo mashup"): cancella i file intermedi e l'mp3 temporaneo. */
    fun chiudi(context: Context) {
        if (occupato()) return
        generatore?.pulisci()
        generatore = null
        Archivio.fileTemporaneo(context).delete()
        _stato.value = null
    }

    private fun occupato() = _stato.value?.let { it.inCorso || it.ricomposizione != null } == true

    private fun avviaServizio(context: Context) {
        ContextCompat.startForegroundService(context, Intent(context, ServizioMashup::class.java))
    }

    internal fun aggiorna(f: (StatoElaborazione) -> StatoElaborazione) {
        _stato.update { it?.let(f) }
    }
}
