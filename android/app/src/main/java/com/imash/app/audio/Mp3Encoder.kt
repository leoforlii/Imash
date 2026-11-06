package com.imash.app.audio

import java.io.Closeable
import java.io.OutputStream

/**
 * Codifica mp3 con LAME (via JNI), a flusso, verso [uscita].
 * Sostituisce `sf.write(..., format="MP3", compression_level=0.0)` degli script:
 * qualità massima, quindi CBR 320 kbps.
 */
class Mp3Encoder(
    sampleRate: Int,
    private val canali: Int,
    private val uscita: OutputStream,
    kbps: Int = 320,
) : Closeable {

    private var handle = nativeInit(sampleRate, canali, kbps)
    private var bufferMp3 = ByteArray(0)

    init {
        require(handle != 0L) { "LAME non accetta questi parametri ($sampleRate Hz, $canali canali)" }
    }

    /** Audio planare in [-1, 1]: blocco[0] = sinistra, blocco[1] = destra (se stereo). */
    fun codifica(blocco: Array<FloatArray>, campioni: Int) {
        val necessari = (1.25 * campioni).toInt() + 7200   // dimensione indicata in lame.h
        if (bufferMp3.size < necessari) bufferMp3 = ByteArray(necessari)
        val destra = if (canali > 1) blocco[1] else null
        val n = nativeEncode(handle, blocco[0], destra, campioni, bufferMp3)
        check(n >= 0) { "Errore LAME $n" }
        uscita.write(bufferMp3, 0, n)
    }

    override fun close() {
        if (handle == 0L) return
        try {
            if (bufferMp3.size < 7200) bufferMp3 = ByteArray(7200)
            val n = nativeFlush(handle, bufferMp3)
            if (n > 0) uscita.write(bufferMp3, 0, n)
            uscita.flush()
        } finally {
            nativeClose(handle)
            handle = 0L
        }
    }

    private companion object {
        init {
            System.loadLibrary("imash_audio")
        }

        @JvmStatic external fun nativeInit(sampleRate: Int, canali: Int, kbps: Int): Long
        @JvmStatic external fun nativeEncode(
            handle: Long, sinistra: FloatArray, destra: FloatArray?, campioni: Int, uscita: ByteArray,
        ): Int
        @JvmStatic external fun nativeFlush(handle: Long, uscita: ByteArray): Int
        @JvmStatic external fun nativeClose(handle: Long)
    }
}
