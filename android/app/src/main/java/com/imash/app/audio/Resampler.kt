package com.imash.app.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Cambio di frequenza di campionamento a flusso, con filtro sinc finestrato (Blackman).
 * Sostituisce il parametro `sr=` di `librosa.load`.
 *
 * Uso: chiamare [processa] per ogni blocco in ingresso e [fine] una volta alla fine;
 * i blocchi convertiti arrivano al [RicevitorePcm] passato.
 */
class Resampler(
    private val daSr: Int,
    private val aSr: Int,
    private val canali: Int,
    private val uscita: RicevitorePcm,
) {
    private val passo = daSr.toDouble() / aSr               // campioni d'ingresso per campione d'uscita
    private val taglio = min(1.0, aSr.toDouble() / daSr) * 0.95 // frequenza di taglio (1 = Nyquist d'ingresso)
    private val metaLarghezza = ZERI / taglio                // metà lunghezza del filtro, in campioni d'ingresso
    private val identico = daSr == aSr

    // Ingresso non ancora consumato, per canale. inizioBuf = indice assoluto di buf[0].
    private var buf = Array(canali) { FloatArray(4096) }
    private var lunghezzaBuf = 0
    private var inizioBuf = 0L
    private var prossimaUscita = 0L                         // indice assoluto del prossimo campione d'uscita
    private var totaleIngresso = 0L
    private val bloccoUscita = Array(canali) { FloatArray(BLOCCO) }
    private val pesi = FloatArray(2 * metaLarghezza.toInt() + 3)

    fun processa(blocco: Array<FloatArray>, campioni: Int) {
        if (identico) {
            uscita.ricevi(blocco, campioni)
            return
        }
        aggiungi(blocco, campioni)
        totaleIngresso += campioni
        produci(finale = false)
    }

    fun fine() {
        if (identico) return
        // Zeri in coda per completare gli ultimi campioni.
        val zeri = metaLarghezza.toInt() + 2
        aggiungi(Array(canali) { FloatArray(zeri) }, zeri)
        produci(finale = true)
    }

    private fun aggiungi(blocco: Array<FloatArray>, campioni: Int) {
        if (lunghezzaBuf + campioni > buf[0].size) {
            val nuova = maxOf(buf[0].size * 2, lunghezzaBuf + campioni)
            buf = Array(canali) { c -> buf[c].copyOf(nuova) }
        }
        for (c in 0 until canali) System.arraycopy(blocco[c], 0, buf[c], lunghezzaBuf, campioni)
        lunghezzaBuf += campioni
    }

    private fun produci(finale: Boolean) {
        val fineBuf = inizioBuf + lunghezzaBuf
        var n = 0
        while (true) {
            val t = prossimaUscita * passo                   // istante in campioni d'ingresso
            if (finale && t >= totaleIngresso) break
            val primo = floor(t - metaLarghezza).toLong() + 1
            val ultimo = floor(t + metaLarghezza).toLong()
            if (ultimo >= fineBuf) break                     // servono altri campioni
            // Pesi del filtro calcolati una volta e usati per tutti i canali.
            val taps = (ultimo - primo + 1).toInt()
            for (j in 0 until taps) pesi[j] = kernel(t - (primo + j))
            val base = (primo - inizioBuf).toInt()           // può essere < 0 solo all'inizio
            val j0 = maxOf(0, -base)
            for (c in 0 until canali) {
                var somma = 0f
                val dati = buf[c]
                for (j in j0 until taps) somma += dati[base + j] * pesi[j]
                bloccoUscita[c][n] = somma
            }
            prossimaUscita++
            if (++n == BLOCCO) {
                uscita.ricevi(bloccoUscita, n)
                n = 0
            }
        }
        if (n > 0) uscita.ricevi(bloccoUscita, n)
        scarta()
    }

    /** Elimina dal buffer i campioni che non servono più. */
    private fun scarta() {
        val primoUtile = floor(prossimaUscita * passo - metaLarghezza).toLong() + 1
        val daTogliere = (primoUtile - inizioBuf).toInt().coerceIn(0, lunghezzaBuf)
        if (daTogliere == 0) return
        for (c in 0 until canali) System.arraycopy(buf[c], daTogliere, buf[c], 0, lunghezzaBuf - daTogliere)
        lunghezzaBuf -= daTogliere
        inizioBuf += daTogliere
    }

    /** Risposta del filtro a distanza x (campioni d'ingresso), letta dalla tabella. */
    private fun kernel(x: Double): Float {
        val u = abs(x) * taglio * RISOLUZIONE                // posizione nella tabella
        val i = u.toInt()
        if (i >= TABELLA.size - 1) return 0f
        val f = (u - i).toFloat()
        return ((TABELLA[i] * (1 - f) + TABELLA[i + 1] * f) * taglio).toFloat()
    }

    companion object {
        private const val ZERI = 10          // passaggi per lo zero del sinc per lato
        private const val RISOLUZIONE = 512  // punti di tabella per passaggio per lo zero
        private const val BLOCCO = 4096

        /** sinc(u) · finestra di Blackman, per u da 0 a ZERI. */
        private val TABELLA = FloatArray(ZERI * RISOLUZIONE + 2) { i ->
            val u = i.toDouble() / RISOLUZIONE
            if (u > ZERI) return@FloatArray 0f
            val sinc = if (u == 0.0) 1.0 else sin(PI * u) / (PI * u)
            val w = (u / ZERI + 1) / 2                       // da 0,5 (centro) a 1 (bordo)
            val blackman = 0.42 - 0.5 * cos(2 * PI * w) + 0.08 * cos(4 * PI * w)
            (sinc * blackman).toFloat()
        }
    }
}
