package com.imash.app.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder

/** Riceve blocchi di audio planare: blocco[canale][i], i < campioni. */
fun interface RicevitorePcm {
    fun ricevi(blocco: Array<FloatArray>, campioni: Int)
}

/** Formato dell'audio decodificato. */
data class InfoAudio(val sampleRate: Int, val canali: Int)

/**
 * Come [RicevitorePcm], ma con il formato (si conosce solo quando il decoder parte).
 * Restituisce false per interrompere la decodifica.
 */
fun interface RicevitoreDecodifica {
    fun ricevi(info: InfoAudio, blocco: Array<FloatArray>, campioni: Int): Boolean
}

/**
 * Decodifica un file audio (mp3 o altro formato supportato da Android) con MediaCodec,
 * a flusso: l'intero brano non viene mai tenuto in memoria qui.
 * Equivale alla lettura di `librosa.load` negli script Python.
 */
object Mp3Decoder {

    private const val TIMEOUT_US = 10_000L

    fun decodifica(context: Context, uri: Uri, ricevitore: RicevitoreDecodifica): InfoAudio {
        val estrattore = MediaExtractor()
        try {
            estrattore.setDataSource(context, uri, null)
            val traccia = (0 until estrattore.trackCount).firstOrNull { i ->
                estrattore.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IllegalArgumentException("Nessuna traccia audio nel file")
            estrattore.selectTrack(traccia)
            val formato = estrattore.getTrackFormat(traccia)
            val codec = avviaCodec(formato)
            try {
                return ciclo(estrattore, codec, formato, ricevitore)
            } finally {
                codec.release()
            }
        } finally {
            estrattore.release()
        }
    }

    /**
     * Crea e avvia il decoder. Su dispositivi lenti l'avvio può scadere (errore 0x80000000
     * visto sull'emulatore): si riprova fino a [TENTATIVI] volte prima di arrendersi.
     */
    private fun avviaCodec(formato: MediaFormat): MediaCodec {
        var ultimoErrore: Exception? = null
        repeat(TENTATIVI) { tentativo ->
            val codec = MediaCodec.createDecoderByType(formato.getString(MediaFormat.KEY_MIME)!!)
            try {
                codec.configure(formato, null, null, 0)
                codec.start()
                return codec
            } catch (e: Exception) {
                codec.release()
                ultimoErrore = e
                Thread.sleep(300L * (tentativo + 1))
            }
        }
        throw IllegalStateException("Il decoder audio non si avvia", ultimoErrore)
    }

    private const val TENTATIVI = 3

    private fun ciclo(
        estrattore: MediaExtractor,
        codec: MediaCodec,
        formatoIngresso: MediaFormat,
        ricevitore: RicevitoreDecodifica,
    ): InfoAudio {
        var info = InfoAudio(
            formatoIngresso.getInteger(MediaFormat.KEY_SAMPLE_RATE),
            formatoIngresso.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
        )
        var pcmFloat = false
        var blocco = Array(info.canali) { FloatArray(0) }
        val bufInfo = MediaCodec.BufferInfo()
        var ingressoFinito = false

        while (true) {
            if (!ingressoFinito) {
                val iIn = codec.dequeueInputBuffer(TIMEOUT_US)
                if (iIn >= 0) {
                    val buf = codec.getInputBuffer(iIn)!!
                    val n = estrattore.readSampleData(buf, 0)
                    if (n < 0) {
                        codec.queueInputBuffer(iIn, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        ingressoFinito = true
                    } else {
                        codec.queueInputBuffer(iIn, 0, n, estrattore.sampleTime, 0)
                        estrattore.advance()
                    }
                }
            }

            val iOut = codec.dequeueOutputBuffer(bufInfo, TIMEOUT_US)
            when {
                iOut == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val f = codec.outputFormat
                    info = InfoAudio(
                        f.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                        f.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                    )
                    pcmFloat = f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                }
                iOut >= 0 -> {
                    if (bufInfo.size > 0) {
                        val buf = codec.getOutputBuffer(iOut)!!.order(ByteOrder.nativeOrder())
                        buf.position(bufInfo.offset).limit(bufInfo.offset + bufInfo.size)
                        val byteCampione = if (pcmFloat) 4 else 2
                        val campioni = bufInfo.size / (byteCampione * info.canali)
                        if (blocco.size != info.canali || blocco[0].size < campioni) {
                            blocco = Array(info.canali) { FloatArray(campioni) }
                        }
                        // Da interlacciato a planare.
                        for (i in 0 until campioni) {
                            for (c in 0 until info.canali) {
                                blocco[c][i] = if (pcmFloat) buf.float else buf.short / 32768f
                            }
                        }
                        if (!ricevitore.ricevi(info, blocco, campioni)) {
                            codec.releaseOutputBuffer(iOut, false)
                            return info
                        }
                    }
                    codec.releaseOutputBuffer(iOut, false)
                    if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return info
                }
            }
        }
    }
}
