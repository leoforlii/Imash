package com.imash.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.imash.app.mashup.GeneratoreMashup
import java.io.File

/**
 * Salvataggio nella memoria del telefono con il "Salva con nome" di sistema
 * (Storage Access Framework): l'utente sceglie cartella e nome, non servono permessi.
 */
object Archivio {

    const val TIPO_MP3 = "audio/mpeg"

    /** Brani accettati in ingresso: qualunque audio che Android sa decodificare (mp3, m4a, …). */
    val TIPI_INGRESSO = arrayOf("audio/*")

    /** File temporaneo nella cache dell'app dove si scrive il mashup prima di salvarlo. */
    fun fileTemporaneo(context: Context, nome: String = "mashup.mp3") = File(context.cacheDir, nome)

    /**
     * All'avvio, se non c'è un mashup in corso: cancella file intermedi e mp3 temporanei lasciati da
     * un'elaborazione interrotta (app chiusa dal sistema) e rilascia i permessi sui brani scelti
     * l'altra volta (la lista riparte vuota).
     */
    fun pulisciAvanzi(context: Context) {
        GeneratoreMashup.cartellaLavoro(context).deleteRecursively()
        context.cacheDir.listFiles { f -> f.name.endsWith(".mp3") }?.forEach { it.delete() }
        context.contentResolver.persistedUriPermissions.forEach {
            runCatching { context.contentResolver.releasePersistableUriPermission(it.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    /** Copia [sorgente] nel documento scelto dall'utente con ActivityResultContracts.CreateDocument. */
    fun copiaIn(context: Context, sorgente: File, destinazione: Uri) {
        val uscita = context.contentResolver.openOutputStream(destinazione, "wt")
            ?: error("Impossibile scrivere nel file scelto")
        uscita.use { out -> sorgente.inputStream().use { it.copyTo(out) } }
    }
}
