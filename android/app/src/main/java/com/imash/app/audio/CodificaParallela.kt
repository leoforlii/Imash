package com.imash.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.OutputStream

/**
 * Codifica in mp3 il mix PCM dividendolo in [Pezzo] codificati in parallelo, ognuno con il suo
 * LAME, e riattacca i frame in un solo file.
 *
 * Perché le giunture non si sentano:
 * - LAME lavora senza serbatoio di bit ([Mp3Encoder] con `perPezzi`), quindi ogni frame sta in
 *   piedi da solo e si può tagliare fra un frame e l'altro;
 * - ogni pezzo (tranne il primo) parte [RISCALDAMENTO] frame prima del suo primo frame e quei
 *   frame si buttano: i filtri di LAME arrivano al punto di giuntura già "caldi", come se la
 *   codifica fosse unica. Il ritardo di LAME è uguale per tutti i pezzi, quindi i frame tenuti
 *   coprono esattamente gli stessi campioni della codifica unica;
 * - ogni pezzo legge anche [MARGINE] frame dopo l'ultimo che tiene (servono a LAME per guardare
 *   avanti), poi i frame in più si buttano.
 */
object CodificaParallela {

    const val CAMPIONI_FRAME = 1152          // MPEG-1 Layer III
    const val RISCALDAMENTO = 3
    const val MARGINE = 3
    /** Sotto questa lunghezza (in frame per pezzo) non conviene dividere. */
    private const val MIN_FRAME_PEZZO = 64

    /**
     * Un pezzo: tiene i frame della codifica unica da [primoFrame] a [fineFrame] escluso (null = fino
     * alla fine), leggendo il mix da [inizioCampioni] a [fineCampioni] e buttando i primi [daSaltare] frame.
     */
    data class Pezzo(
        val primoFrame: Long,
        val fineFrame: Long?,
        val inizioCampioni: Long,
        val fineCampioni: Long,
        val daSaltare: Int,
    ) {
        val daTenere: Long? get() = fineFrame?.let { it - primoFrame }
    }

    /** Divide [totale] campioni in al massimo [pezzi] pezzi su confini di frame. */
    fun pezzi(totale: Long, pezzi: Int): List<Pezzo> {
        val frame = totale / CAMPIONI_FRAME
        val n = minOf(pezzi.toLong(), frame / MIN_FRAME_PEZZO).toInt().coerceAtLeast(1)
        val confini = (0..n).map { k -> if (k == n) null else frame * k / n }
        return (0 until n).map { k ->
            val primo = confini[k]!!
            val fine = confini[k + 1]
            val salta = if (k == 0) 0 else RISCALDAMENTO
            Pezzo(
                primoFrame = primo,
                fineFrame = fine,
                inizioCampioni = (primo - salta) * CAMPIONI_FRAME,
                fineCampioni = fine?.let { minOf(totale, (it + MARGINE) * CAMPIONI_FRAME) } ?: totale,
                daSaltare = salta,
            )
        }
    }

    /**
     * Codifica [mix] ([totale] campioni stereo a [sr] Hz, moltiplicati per [scala]) in [uscita],
     * con al massimo [parallelismo] pezzi alla volta; i file intermedi vanno in [lavoro].
     */
    suspend fun codifica(
        mix: File,
        totale: Long,
        scala: Float,
        sr: Int,
        parallelismo: Int,
        lavoro: File,
        uscita: OutputStream,
        avanzamento: (Double) -> Unit,
    ) = coroutineScope {
        val piano = pezzi(totale, parallelismo)
        val file = piano.indices.map { File(lavoro, "pezzo$it.mp3") }
        val letti = LongArray(piano.size)
        val avvisa = { k: Int, n: Long ->
            synchronized(letti) {
                val prima = letti.sum() / PASSO_AVVISO
                letti[k] += n
                val somma = letti.sum()
                if (somma / PASSO_AVVISO != prima) avanzamento(minOf(1.0, somma.toDouble() / totale))
            }
        }
        try {
            piano.mapIndexed { k, p ->
                async(Dispatchers.Default.limitedParallelism(parallelismo)) {
                    val contesto = currentCoroutineContext()
                    file[k].outputStream().buffered().use { out ->
                        // L'inizializzazione di LAME tocca tabelle globali: una alla volta.
                        val encoder = synchronized(this@CodificaParallela) { Mp3Encoder(sr, 2, out, perPezzi = true) }
                        encoder.use { enc ->
                            LettorePcm.leggi(mix, p.fineCampioni - p.inizioCampioni, scala, p.inizioCampioni) { b, n ->
                                contesto.ensureActive()
                                enc.codifica(b, n)
                                avvisa(k, n.toLong())
                            }
                        }
                    }
                }
            }.awaitAll()
            piano.forEachIndexed { k, p ->
                val byte = file[k].readBytes()
                val inizi = FrameMp3.inizi(byte)
                val primo = p.daSaltare
                val ultimo = p.daTenere?.let { (primo + it).toInt() } ?: inizi.size
                check(ultimo <= inizi.size) { "Pezzo $k: ${inizi.size} frame, ne servivano $ultimo" }
                val da = inizi[primo]
                val a = if (ultimo < inizi.size) inizi[ultimo] else byte.size
                uscita.write(byte, da, a - da)
            }
        } finally {
            file.forEach { it.delete() }
        }
    }

    private const val PASSO_AVVISO = 44_100L * 2     // un aggiornamento ogni ~2 s di audio
}

/** Lettura dei frame di un mp3 MPEG-1 Layer III, come li scrive LAME (senza tag ID3). */
object FrameMp3 {

    private val KBPS = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0)
    private val SR = intArrayOf(44_100, 48_000, 32_000, 0)

    /** Lunghezza in byte del frame che inizia in [i], o -1 se lì non c'è un'intestazione valida. */
    fun lunghezza(b: ByteArray, i: Int): Int {
        if (i + 4 > b.size) return -1
        val b0 = b[i].toInt() and 0xFF
        val b1 = b[i + 1].toInt() and 0xFF
        val b2 = b[i + 2].toInt() and 0xFF
        if (b0 != 0xFF || (b1 and 0xFE) != 0xFA) return -1      // sincronismo, MPEG-1, Layer III
        val kbps = KBPS[b2 shr 4]
        val sr = SR[(b2 shr 2) and 3]
        if (kbps == 0 || sr == 0) return -1
        return 144 * kbps * 1000 / sr + ((b2 shr 1) and 1)
    }

    /** Posizione di ogni frame; fallisce se i byte non sono una sequenza di frame. */
    fun inizi(b: ByteArray): IntArray {
        val inizi = ArrayList<Int>(b.size / 1000 + 1)
        var i = 0
        while (i < b.size) {
            val n = lunghezza(b, i)
            check(n > 0 && i + n <= b.size) { "mp3 non valido al byte $i" }
            inizi += i
            i += n
        }
        return inizi.toIntArray()
    }
}
