package com.imash.app.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

/** Un brano scelto dall'utente: nome da mostrare e durata in secondi (0 se sconosciuta). */
data class BranoScelto(val uri: Uri, val nome: String, val durata: Double)

object Brani {

    /**
     * Legge nome e durata di [uri] senza decodificarlo. Tiene il permesso di lettura anche
     * dopo la chiusura della schermata (serve al servizio di elaborazione).
     * null se il file non si può leggere.
     */
    fun leggi(context: Context, uri: Uri): BranoScelto? = runCatching {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val nome = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            ?.substringBeforeLast('.')
            ?: uri.lastPathSegment.orEmpty()
        val durata = MediaMetadataRetriever().run {
            try {
                setDataSource(context, uri)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toDoubleOrNull()?.div(1000) ?: 0.0
            } finally {
                release()
            }
        }
        BranoScelto(uri, nome, durata)
    }.getOrNull()

    /** Rilascia il permesso preso con [leggi] quando il brano viene tolto dalla lista. */
    fun rilascia(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
