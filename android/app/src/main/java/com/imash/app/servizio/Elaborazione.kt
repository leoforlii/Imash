package com.imash.app.servizio

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.imash.app.data.BranoScelto
import com.imash.app.mashup.Modalita
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * L'elaborazione in corso (una alla volta), condivisa fra le schermate e [ServizioMashup].
 * Vive quanto il processo dell'app: il servizio in primo piano lo tiene vivo a schermo spento.
 */
object Elaborazione {

    private val _stato = MutableStateFlow<StatoElaborazione?>(null)
    val stato: StateFlow<StatoElaborazione?> = _stato.asStateFlow()

    /** Richiesta letta dal servizio. */
    internal var uris: List<Uri> = emptyList()
        private set
    internal var durataObiettivo = 0.0
        private set

    /** Il lavoro del servizio, per poterlo annullare. */
    internal var lavoro: Job? = null

    /** Avvia il servizio; ignorato se c'è già un'elaborazione in corso. */
    fun avvia(context: Context, brani: List<BranoScelto>, modalita: Modalita, durataObiettivo: Double) {
        if (_stato.value?.inCorso == true) return
        uris = brani.map { it.uri }
        this.durataObiettivo = durataObiettivo
        _stato.value = StatoElaborazione(
            nomi = brani.map { it.nome },
            durate = brani.map { it.durata },
            modalita = modalita,
            inizio = System.currentTimeMillis(),
        )
        ContextCompat.startForegroundService(context, Intent(context, ServizioMashup::class.java))
    }

    fun annulla() {
        lavoro?.cancel()
    }

    /** Dimentica l'elaborazione conclusa (dopo "Nuovo mashup" o tornando ai brani). */
    fun chiudi() {
        if (_stato.value?.inCorso == true) return
        _stato.value = null
    }

    internal fun aggiorna(f: (StatoElaborazione) -> StatoElaborazione) {
        _stato.update { it?.let(f) }
    }
}
