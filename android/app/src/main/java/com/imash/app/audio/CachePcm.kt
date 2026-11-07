package com.imash.app.audio

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Audio stereo PCM a 16 bit (sinistra/destra alternati, little-endian) in un file della cache.
 * Serve a decodificare ogni brano una sola volta: l'analisi lo scrive, il mix lo rilegge.
 * Ogni campione viene moltiplicato per [scala] prima di essere scritto (e limitato a ±1).
 */
class ScrittorePcm(file: File, private val scala: Float = 1f) : RicevitorePcm, Closeable {

    private val uscita = BufferedOutputStream(FileOutputStream(file), BUFFER)
    private var byte = ByteBuffer.allocate(0)

    /** Massimo valore assoluto ricevuto, già moltiplicato per [scala] (prima del limite a ±1). */
    var picco = 0f
        private set

    /** Campioni oltre ±1 dopo la [scala], quindi limitati: devono restare 0. */
    var saturati = 0L
        private set

    override fun ricevi(blocco: Array<FloatArray>, campioni: Int) {
        val necessari = campioni * BYTE_PER_CAMPIONE
        if (byte.capacity() < necessari) byte = ByteBuffer.allocate(necessari).order(ByteOrder.LITTLE_ENDIAN)
        byte.clear()
        val sx = blocco[0]
        val dx = blocco[1]
        for (i in 0 until campioni) {
            byte.putShort(converti(sx[i]))
            byte.putShort(converti(dx[i]))
        }
        uscita.write(byte.array(), 0, necessari)
    }

    private fun converti(x: Float): Short {
        val v = x * scala
        val a = abs(v)
        if (a > picco) picco = a
        if (a > 1f) saturati++
        return (v.coerceIn(-1f, 1f) * MASSIMO).roundToInt().toShort()
    }

    override fun close() = uscita.close()

    companion object {
        internal const val BYTE_PER_CAMPIONE = 4      // 2 canali × 16 bit
        internal const val MASSIMO = 32767f
        internal const val BUFFER = 1 shl 16
    }
}

object LettorePcm {

    private const val BLOCCO = 4096

    /**
     * Rilegge un file di [ScrittorePcm] e lo consegna a [ricevitore] a blocchi, moltiplicato per
     * [scala]. Si ferma dopo [maxCampioni] campioni o alla fine del file.
     */
    fun leggi(file: File, maxCampioni: Long = Long.MAX_VALUE, scala: Float = 1f, ricevitore: RicevitorePcm) {
        val byte = ByteArray(BLOCCO * ScrittorePcm.BYTE_PER_CAMPIONE)
        val buffer = ByteBuffer.wrap(byte).order(ByteOrder.LITTLE_ENDIAN)
        val blocco = Array(2) { FloatArray(BLOCCO) }
        val k = scala / ScrittorePcm.MASSIMO
        var consegnati = 0L
        BufferedInputStream(FileInputStream(file), ScrittorePcm.BUFFER).use { ingresso ->
            while (consegnati < maxCampioni) {
                val letti = leggiTutto(ingresso, byte)
                val n = minOf((letti / ScrittorePcm.BYTE_PER_CAMPIONE).toLong(), maxCampioni - consegnati).toInt()
                if (n <= 0) break
                buffer.clear()
                for (i in 0 until n) {
                    blocco[0][i] = buffer.short * k
                    blocco[1][i] = buffer.short * k
                }
                ricevitore.ricevi(blocco, n)
                consegnati += n
                if (letti < byte.size) break
            }
        }
    }

    /** Riempie [byte] il più possibile (read può restituire meno byte del richiesto). */
    private fun leggiTutto(ingresso: BufferedInputStream, byte: ByteArray): Int {
        var totale = 0
        while (totale < byte.size) {
            val n = ingresso.read(byte, totale, byte.size - totale)
            if (n < 0) break
            totale += n
        }
        return totale
    }
}
