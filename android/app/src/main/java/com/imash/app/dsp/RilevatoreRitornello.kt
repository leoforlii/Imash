package com.imash.app.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Tratto [inizio, fine) in secondi. */
data class Tratto(val inizio: Double, val fine: Double)

/**
 * Porting di `pychorus` 0.1 come lo usa `trova_ritornello` in `estrai_ritornelli.py`
 * (Jiang & Müller: il ritornello è la sezione che si ripete più volte).
 * - matrice di somiglianza tempo-tempo e tempo-ritardo sul cromagramma;
 * - riduzione del rumore della matrice tempo-ritardo ([riduciRumore], `TimeLagSimilarityMatrix.denoise`);
 * - righe di massimo locale, segmenti sopra soglia, punteggio per sovrapposizione, miglior segmento.
 * Lavora sul cromagramma con n_fft [N_FFT] (circa 5,4 frame al secondo a 22050 Hz).
 */
object RilevatoreRitornello {

    const val N_FFT = 16_384                     // pychorus.constants.N_FFT
    const val MAX_CLIP_SEC = 60.0                // estrai_ritornelli.py
    private const val SMOOTHING_SIZE_SEC = 2.5
    private const val LINE_THRESHOLD = 0.15
    private const val MIN_LINES = 10
    private const val NUM_ITERATIONS = 8
    private const val OVERLAP_PERCENT_MARGIN = 0.2
    private const val TRUNCATE = 4.0             // default di scipy.ndimage.gaussian_filter1d
    private val RADICE_12 = sqrt(12.0).toFloat()

    /** Segmento ripetuto: colonne [inizio, fine) sulla riga [ritardo] della matrice tempo-ritardo. */
    internal class Linea(val inizio: Int, val fine: Int, val ritardo: Int)

    /**
     * Come `trova_ritornello(chroma, song_length_sec, clip_length)`: inizio e fine del ritornello
     * lungo almeno [clipSec] secondi, o null se non ci sono abbastanza ripetizioni.
     */
    fun trova(croma: Array<FloatArray>, durata: Double, clipSec: Double): Tratto? {
        val n = croma.size
        if (n < 2 || durata <= 0) return null
        val cromaSr = n / durata
        val matrice = riduciRumore(croma, (SMOOTHING_SIZE_SEC * cromaSr).toInt().coerceAtLeast(1))
        val righe = righeDiMassimo(matrice)
        val lunghezzaMin = clipSec * cromaSr
        val linee = trovaLinee(matrice, righe, lunghezzaMin)
        if (linee.isEmpty()) return null
        val migliore = miglioreLinea(linee, OVERLAP_PERCENT_MARGIN * clipSec * cromaSr, lunghezzaMin)
        val inizio = migliore.inizio / cromaSr
        return Tratto(inizio, min(migliore.fine / cromaSr, inizio + MAX_CLIP_SEC))
    }

    /** `1 - ||a - b|| / √12`: somiglianza fra due frame di croma. */
    private fun somiglianza(a: FloatArray, b: FloatArray): Float {
        var s = 0f
        for (c in 0 until 12) {
            val d = a[c] - b[c]
            s += d * d
        }
        return 1 - sqrt(s) / RADICE_12
    }

    /**
     * Matrice tempo-ritardo ripulita: L[r][t] = somiglianza(t, t - r) (circolare prima del triu),
     * come `TimeLagSimilarityMatrix` + `denoise(time_time_matrix, smoothing_size)`.
     * Le medie mobili di `convolve2d(mode="full")` sono calcolate con somme scorrevoli.
     */
    internal fun riduciRumore(croma: Array<FloatArray>, s: Int): Array<FloatArray> {
        val n = croma.size
        val ritardo = Array(n) { r -> FloatArray(n) { t -> somiglianza(croma[t], croma[(t - r + n) % n]) } }
        val pulita = Array(n) { FloatArray(n) }

        // Somme verticali scorrevoli: giu[t] = righe r-s+1..r, su[t] = righe r..r+s-1 (zeri fuori).
        val giu = DoubleArray(n)
        val su = DoubleArray(n)
        for (r in 0 until minOf(s - 1, n)) for (t in 0 until n) su[t] += ritardo[r][t]
        val orizzontale = DoubleArray(n + 1)
        val tempoTempo = FloatArray(n)
        val diagonale = DoubleArray(n + 1)

        for (r in 0 until n) {
            for (t in 0 until n) giu[t] += ritardo[r][t]
            if (r - s >= 0) for (t in 0 until n) giu[t] -= ritardo[r - s][t]
            if (r + s - 1 < n) for (t in 0 until n) su[t] += ritardo[r + s - 1][t]
            if (r - 1 >= 0) for (t in 0 until n) su[t] -= ritardo[r - 1][t]

            prefisso(ritardo[r], orizzontale)
            for (t in 0 until n) tempoTempo[t] = somiglianza(croma[r], croma[t])
            prefisso(tempoTempo, diagonale)

            val riga = ritardo[r]
            val uscita = pulita[r]
            for (t in 0 until n) {
                val sinistra = mediaIndietro(orizzontale, t, s)
                val destra = mediaAvanti(orizzontale, t, s, n)
                val maxOrizzontale = max(sinistra, destra)
                val g = giu[t] / s
                val u = su[t] / s
                val su2 = mediaIndietro(diagonale, t, s)
                val giu2 = mediaAvanti(diagonale, t, s, n)
                val maxAltri = maxOf(g, u, su2, giu2)
                val minAltri = minOf(g, u, su2, giu2)
                val soppressione = if (maxOrizzontale > maxAltri) minAltri else maxAltri
                uscita[t] = if (t >= r) (riga[t] - soppressione).toFloat() else 0f     // np.triu
            }
        }
        val temp = FloatArray(n)
        val pesi = pesiGaussiani(s.toDouble())
        for (r in 0 until n) {
            filtroGaussiano(pulita[r], pesi, temp)
            for (t in 0 until n) pulita[r][t] = if (r < 5) 0f else max(temp[t], 0f)
        }
        return pulita
    }

    private fun prefisso(v: FloatArray, p: DoubleArray) {
        p[0] = 0.0
        for (i in v.indices) p[i + 1] = p[i] + v[i]
    }

    /** Media di v[t-s+1..t] con zeri fuori dall'array (diviso sempre per s, come convolve2d). */
    private fun mediaIndietro(p: DoubleArray, t: Int, s: Int) = (p[t + 1] - p[max(0, t - s + 1)]) / s

    /** Media di v[t..t+s-1] con zeri fuori dall'array. */
    private fun mediaAvanti(p: DoubleArray, t: Int, s: Int, n: Int) = (p[min(n, t + s)] - p[t]) / s

    /** Pesi di `gaussian_filter1d(sigma)`: raggio int(4σ + 0,5), somma 1. */
    private fun pesiGaussiani(sigma: Double): FloatArray {
        val raggio = (TRUNCATE * sigma + 0.5).toInt()
        val w = DoubleArray(2 * raggio + 1) { exp(-0.5 * ((it - raggio) / sigma).let { x -> x * x }) }
        val somma = w.sum()
        return FloatArray(w.size) { (w[it] / somma).toFloat() }
    }

    /** Convoluzione con bordi "reflect" di scipy (d c b a | a b c d | d c b a). */
    private fun filtroGaussiano(v: FloatArray, pesi: FloatArray, uscita: FloatArray) {
        val n = v.size
        val raggio = pesi.size / 2
        for (t in 0 until n) {
            var s = 0f
            for (k in pesi.indices) s += pesi[k] * v[rifletti(t + k - raggio, n)]
            uscita[t] = s
        }
    }

    internal fun rifletti(i: Int, n: Int): Int {
        val periodo = 2 * n
        val m = ((i % periodo) + periodo) % periodo
        return if (m < n) m else periodo - 1 - m
    }

    /** `local_maxima_rows`: righe la cui somma media è un massimo locale stretto. */
    internal fun righeDiMassimo(matrice: Array<FloatArray>): IntArray {
        val n = matrice.size
        val medie = DoubleArray(n) { r -> matrice[r].sumOf { it.toDouble() } / (n - r) }
        return (1 until n - 1).filter { medie[it] > medie[it - 1] && medie[it] > medie[it + 1] }.toIntArray()
    }

    /** `detect_lines`: abbassa la soglia del 5% finché trova almeno [MIN_LINES] segmenti (max 8 volte). */
    internal fun trovaLinee(matrice: Array<FloatArray>, righe: IntArray, lunghezzaMin: Double): List<Linea> {
        var soglia = LINE_THRESHOLD
        var linee = emptyList<Linea>()
        repeat(NUM_ITERATIONS) {
            linee = trovaLineeConSoglia(matrice, righe, soglia, lunghezzaMin)
            if (linee.size >= MIN_LINES) return linee
            soglia *= 0.95
        }
        return linee
    }

    /**
     * `detect_lines_helper`. Come nell'originale, l'inizio di un segmento non si azzera passando
     * alla riga successiva e un segmento che arriva a fine riga non viene chiuso: comportamento
     * mantenuto per dare gli stessi risultati dello script.
     */
    private fun trovaLineeConSoglia(
        matrice: Array<FloatArray>, righe: IntArray, soglia: Double, lunghezzaMin: Double,
    ): List<Linea> {
        val n = matrice.size
        val linee = ArrayList<Linea>()
        var inizio = -1
        for (r in righe) {
            if (r < lunghezzaMin) continue
            for (t in r until n) {
                if (matrice[r][t] > soglia) {
                    if (inizio < 0) inizio = t
                } else {
                    if (inizio >= 0 && t - inizio > lunghezzaMin) linee.add(Linea(inizio, t, r))
                    inizio = -1
                }
            }
        }
        return linee
    }

    /** `count_overlapping_lines` + `best_segment`: più sovrapposizioni, a parità la più lunga. */
    internal fun miglioreLinea(linee: List<Linea>, margine: Double, lunghezzaMin: Double): Linea {
        val punteggi = IntArray(linee.size)
        for ((i, a) in linee.withIndex()) {
            for (b in linee) {
                val ritardiDiversi = abs(b.ritardo - a.ritardo) > lunghezzaMin
                val verticale = b.inizio < a.inizio + margine && b.fine > a.fine - margine
                val diagonale = (b.inizio - b.ritardo) < (a.inizio - a.ritardo + margine) &&
                    (b.fine - b.ritardo) > (a.fine - a.ritardo - margine)
                if (ritardiDiversi && (verticale || diagonale)) punteggi[i]++
            }
        }
        // sort(reverse=True) di Python è stabile: a parità vince la prima linea trovata.
        return linee.indices.sortedWith(
            compareByDescending<Int> { punteggi[it] }.thenByDescending { linee[it].fine - linee[it].inizio },
        ).first().let { linee[it] }
    }

    /**
     * `segmento_piu_forte`: la finestra di [durataSec] secondi con l'energia media più alta di [y]
     * (mono a [sr] Hz); il primo massimo, come np.argmax.
     */
    fun segmentoPiuForte(y: FloatArray, sr: Int, durataSec: Double = 20.0): Tratto {
        val finestra = (durataSec * sr).toInt()
        if (y.size <= finestra) return Tratto(0.0, y.size.toDouble() / sr)
        var somma = 0.0
        for (i in 0 until finestra) somma += y[i].toDouble() * y[i]
        var migliore = somma
        var inizio = 0
        for (i in 1..y.size - finestra) {
            somma += y[i + finestra - 1].toDouble() * y[i + finestra - 1] - y[i - 1].toDouble() * y[i - 1]
            if (somma > migliore + 1e-9) {
                migliore = somma
                inizio = i
            }
        }
        val start = inizio.toDouble() / sr
        return Tratto(start, start + durataSec)
    }
}
