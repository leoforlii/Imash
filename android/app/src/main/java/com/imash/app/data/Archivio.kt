package com.imash.app.data

import android.content.Context
import android.net.Uri
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

    /** Copia [sorgente] nel documento scelto dall'utente con ActivityResultContracts.CreateDocument. */
    fun copiaIn(context: Context, sorgente: File, destinazione: Uri) {
        val uscita = context.contentResolver.openOutputStream(destinazione, "wt")
            ?: error("Impossibile scrivere nel file scelto")
        uscita.use { out -> sorgente.inputStream().use { it.copyTo(out) } }
    }
}
