package com.imash.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.datastore by preferencesDataStore(name = "impostazioni")

/** Impostazioni salvate con DataStore: per ora solo la durata del mashup (3-30 min, predefinita 10). */
class Impostazioni(private val context: Context) {

    val durataMinuti: Flow<Int> = context.datastore.data.map {
        (it[DURATA] ?: DURATA_PREDEFINITA).coerceIn(DURATA_MIN, DURATA_MAX)
    }

    suspend fun salvaDurata(minuti: Int) {
        context.datastore.edit { it[DURATA] = minuti.coerceIn(DURATA_MIN, DURATA_MAX) }
    }

    companion object {
        const val DURATA_MIN = 3
        const val DURATA_MAX = 30
        const val DURATA_PREDEFINITA = 10
        private val DURATA = intPreferencesKey("durata_minuti")
    }
}
