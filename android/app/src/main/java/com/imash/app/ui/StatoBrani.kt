package com.imash.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.imash.app.data.BranoScelto
import com.imash.app.data.Brani
import com.imash.app.data.Impostazioni
import com.imash.app.mashup.Modalita
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Stato della schermata dei brani: lista ordinata, modalità e durata scelta nelle impostazioni. */
class StatoBrani(app: Application) : AndroidViewModel(app) {

    private val _brani = MutableStateFlow<List<BranoScelto>>(emptyList())
    val brani: StateFlow<List<BranoScelto>> = _brani.asStateFlow()

    private val _modalita = MutableStateFlow(Modalita.CanzoniIntere)
    val modalita: StateFlow<Modalita> = _modalita.asStateFlow()

    /** File saltati all'ultima aggiunta perché non leggibili (per lo snackbar); 0 dopo averlo mostrato. */
    private val _saltati = MutableStateFlow(0)
    val saltati: StateFlow<Int> = _saltati.asStateFlow()

    val durataMinuti: StateFlow<Int> = Impostazioni(app).durataMinuti
        .stateIn(viewModelScope, SharingStarted.Eagerly, Impostazioni.DURATA_PREDEFINITA)

    fun scegliModalita(m: Modalita) { _modalita.value = m }

    /** Aggiunge in fondo i brani scelti, saltando quelli già in lista e quelli non leggibili. */
    fun aggiungi(uris: List<Uri>) {
        val nuovi = uris.distinct().filter { u -> _brani.value.none { it.uri == u } }
        if (nuovi.isEmpty()) return
        viewModelScope.launch {
            val letti = withContext(Dispatchers.IO) { nuovi.map { Brani.leggi(getApplication(), it) } }
            _brani.update { it + letti.filterNotNull() }
            _saltati.value = letti.count { it == null }
        }
    }

    fun togli(brano: BranoScelto) {
        _brani.update { it - brano }
        Brani.rilascia(getApplication(), brano.uri)
    }

    fun sposta(da: Int, a: Int) {
        _brani.update { lista ->
            if (da !in lista.indices || a !in lista.indices) lista
            else lista.toMutableList().apply { add(a, removeAt(da)) }
        }
    }

    fun saltatiMostrati() { _saltati.value = 0 }
}
