package com.imash.app.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.io.File

/** Lettore dell'anteprima del mashup (un mp3 locale) per la schermata del risultato. */
class Lettore(file: File) {
    private val player = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        setDataSource(file.absolutePath)
        prepare()
        setOnCompletionListener { inRiproduzione = false; posizioneMs = durataMs }
    }

    val durataMs: Int = player.duration
    var posizioneMs by mutableIntStateOf(0)
        private set
    var inRiproduzione by mutableStateOf(false)
        private set

    fun alterna() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (posizioneMs >= durataMs) vaiA(0)
            player.start()
        }
        inRiproduzione = player.isPlaying
    }

    fun pausa() {
        if (player.isPlaying) player.pause()
        inRiproduzione = false
    }

    fun vaiA(ms: Int) {
        val p = ms.coerceIn(0, durataMs)
        player.seekTo(p.toLong(), MediaPlayer.SEEK_CLOSEST)
        posizioneMs = p
    }

    fun salta(deltaMs: Int) = vaiA(posizioneMs + deltaMs)

    internal fun aggiornaPosizione() {
        if (player.isPlaying) posizioneMs = player.currentPosition
    }

    internal fun rilascia() = player.release()
}

/** Un [Lettore] per [file], ricreato quando cambia [versione] (mashup ricomposto) e rilasciato all'uscita. */
@Composable
fun rememberLettore(file: File, versione: Any): Lettore? {
    val lettore = remember(versione) { runCatching { Lettore(file) }.getOrNull() }
    DisposableEffect(lettore) { onDispose { lettore?.rilascia() } }
    LaunchedEffect(lettore, lettore?.inRiproduzione) {
        while (lettore != null && lettore.inRiproduzione) {
            lettore.aggiornaPosizione()
            delay(200)
        }
    }
    return lettore
}
