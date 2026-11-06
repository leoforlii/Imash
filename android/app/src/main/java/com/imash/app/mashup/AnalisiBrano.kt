package com.imash.app.mashup

import com.imash.app.audio.RicevitorePcm
import com.imash.app.audio.Resampler
import com.imash.app.dsp.BeatTracker
import com.imash.app.dsp.Croma
import com.imash.app.dsp.Tonalita
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Quello che serve sapere di un brano per costruire il mashup.
 * Tempi in secondi dall'inizio del file.
 */
class AnalisiBrano(
    val durata: Double,
    /** Primo e ultimo istante non silenziosi (taglio del silenzio iniziale e finale). */
    val inizioSuono: Double,
    val fineSuono: Double,
    /**
     * Inizi di misura: un battito ogni 4 partendo dal battere (vedi [faseBattere]), estesi
     * all'indietro fino all'inizio del suono; null se meno di 16 battiti.
     */
    val misure: DoubleArray?,
    /** Secondi per battito (mediana); null se i battiti non sono affidabili. */
    val spb: Double?,
    val bpm: Double,
    /** null se il brano non ha un'armonia riconoscibile. */
    val tonalita: Tonalita?,
    private val energia: FloatArray,
    private val secondiPerBlocco: Double,
) {
    /** RMS stereo del tratto [da, a), come `np.sqrt(np.mean(y ** 2))` in `normalizza_rms`. */
    fun rms(da: Double, a: Double): Double {
        val i0 = (da / secondiPerBlocco).toInt().coerceIn(0, energia.size)
        val i1 = (a / secondiPerBlocco).toInt().coerceIn(i0, energia.size)
        if (i1 <= i0) return 0.0
        var s = 0.0
        for (i in i0 until i1) s += energia[i]
        return sqrt(s / (i1 - i0))
    }
}

/**
 * Analizza un brano letto a flusso a [srUscita] Hz stereo (lo stesso formato del mashup):
 * - energia per blocchi → taglio del silenzio e RMS di qualunque tratto;
 * - mono ricampionato a [SR_ANALISI] Hz → battiti con [BeatTracker] (come mashup.py).
 * Non dipende da Android: riceve i blocchi con [ricevi] e restituisce il risultato con [concludi].
 */
class AnalizzatoreBrano(private val srUscita: Int) : RicevitorePcm {

    private var energia = FloatArray(1024)
    private var blocchi = 0
    private var accumulo = 0.0
    private var nelBlocco = 0
    private var campioni = 0L

    private var mono = FloatArray(SR_ANALISI * 60)
    private var lunghezzaMono = 0
    private val monoBlocco = arrayOf(FloatArray(4096))
    private val resampler = Resampler(srUscita, SR_ANALISI, 1) { b, n ->
        if (lunghezzaMono + n > mono.size) mono = mono.copyOf(maxOf(mono.size * 2, lunghezzaMono + n))
        System.arraycopy(b[0], 0, mono, lunghezzaMono, n)
        lunghezzaMono += n
    }

    override fun ricevi(blocco: Array<FloatArray>, campioni: Int) {
        val sx = blocco[0]
        val dx = blocco[1]
        if (monoBlocco[0].size < campioni) monoBlocco[0] = FloatArray(campioni)
        val m = monoBlocco[0]
        for (i in 0 until campioni) {
            accumulo += (sx[i] * sx[i] + dx[i] * dx[i]) / 2.0
            if (++nelBlocco == BLOCCO) chiudiBlocco()
            m[i] = (sx[i] + dx[i]) / 2
        }
        resampler.processa(monoBlocco, campioni)
        this.campioni += campioni
    }

    private fun chiudiBlocco() {
        if (blocchi == energia.size) energia = energia.copyOf(blocchi * 2)
        energia[blocchi++] = (accumulo / nelBlocco).toFloat()
        accumulo = 0.0
        nelBlocco = 0
    }

    fun concludi(): AnalisiBrano {
        if (nelBlocco > 0) chiudiBlocco()
        resampler.fine()
        val secPerBlocco = BLOCCO.toDouble() / srUscita
        val durata = campioni.toDouble() / srUscita

        // Silenzio: blocchi più di SILENZIO_DB sotto il blocco più forte (come librosa.effects.trim).
        val e = energia.copyOf(blocchi)
        val massimo = e.maxOrNull() ?: 0f
        val soglia = massimo * Math.pow(10.0, -SILENZIO_DB / 10).toFloat()
        val primo = e.indexOfFirst { it > soglia && it > 0f }
        val ultimo = e.indexOfLast { it > soglia && it > 0f }
        val inizioSuono = if (primo < 0) 0.0 else primo * secPerBlocco
        val fineSuono = if (ultimo < 0) durata else minOf(durata, (ultimo + 1) * secPerBlocco)

        val y = mono.copyOf(lunghezzaMono)
        val battiti = BeatTracker(SR_ANALISI).analizza(y)
        val croma = Croma(SR_ANALISI, CROMA_N_FFT)
        val frameCroma = croma.calcola(y)
        val tonalita = Tonalita.stima(DoubleArray(12) { c -> frameCroma.sumOf { it[c].toDouble() } })

        val tempi = battiti.tempi
        val (misure, spb) = if (tempi.size < MIN_BATTITI) {
            null to null
        } else {
            val diff = DoubleArray(tempi.size - 1) { tempi[it + 1] - tempi[it] }.sorted()
            val mediana = if (diff.size % 2 == 1) diff[diff.size / 2]
            else (diff[diff.size / 2 - 1] + diff[diff.size / 2]) / 2
            val fase = faseBattere(tempi, frameCroma, croma.framePerSecondo)
            val battere = DoubleArray((tempi.size - fase + 3) / 4) { tempi[fase + it * 4] }
            grigliaMisure(battere, 4 * mediana, inizioSuono) to mediana
        }
        return AnalisiBrano(durata, inizioSuono, fineSuono, misure, spb, battiti.bpm, tonalita, e, secPerBlocco)
    }

    companion object {
        const val SR_ANALISI = 22_050      // frequenza di analisi di librosa
        const val BLOCCO = 2048            // ~46 ms a 44,1 kHz
        const val SILENZIO_DB = 50.0
        const val MIN_BATTITI = 16         // come `if len(beat_times) < 16` in mashup.py
        const val CROMA_N_FFT = 4096       // ~5 Hz per bin a 22050 Hz, hop 1024 (~46 ms)
    }
}

/**
 * Quale dei primi 4 battiti è il primo della misura: quello in cui, in media, l'armonia
 * cambia di più (gli accordi cambiano quasi sempre sul battere). Il croma viene mediato
 * battito per battito e il cambio misurato come 1 - somiglianza del coseno.
 */
internal fun faseBattere(tempi: DoubleArray, croma: Array<FloatArray>, framePerSecondo: Double): Int {
    if (tempi.size < 9 || croma.isEmpty()) return 0
    fun frame(t: Double) = (t * framePerSecondo).roundToInt().coerceIn(0, croma.size - 1)
    val medie = Array(tempi.size - 1) { b ->
        val f0 = frame(tempi[b])
        val f1 = maxOf(f0 + 1, frame(tempi[b + 1]))
        DoubleArray(12) { c -> (f0 until minOf(f1, croma.size)).sumOf { croma[it][c].toDouble() } }
    }
    val punteggio = DoubleArray(4)
    val conteggio = IntArray(4)
    for (b in 1 until medie.size) {
        val p = medie[b - 1]
        val q = medie[b]
        var pq = 0.0
        var pp = 0.0
        var qq = 0.0
        for (c in 0 until 12) {
            pq += p[c] * q[c]
            pp += p[c] * p[c]
            qq += q[c] * q[c]
        }
        if (pp == 0.0 || qq == 0.0) continue
        punteggio[b % 4] += 1 - pq / sqrt(pp * qq)
        conteggio[b % 4]++
    }
    return (0 until 4).maxBy { if (conteggio[it] == 0) 0.0 else punteggio[it] / conteggio[it] }
}

/**
 * Estende all'indietro la griglia delle misure, a passi di [misura] secondi, fino a [inizioSuono]:
 * così il brano può partire su un battere senza perdere l'introduzione prima del primo battito.
 */
internal fun grigliaMisure(battere: DoubleArray, misura: Double, inizioSuono: Double): DoubleArray {
    val validi = battere.filter { it >= inizioSuono - 0.05 }
    if (validi.isEmpty()) return battere
    val prima = floor((validi[0] - inizioSuono) / misura).toInt().coerceAtLeast(0)
    return DoubleArray(prima) { validi[0] - (prima - it) * misura } + validi.toDoubleArray()
}
