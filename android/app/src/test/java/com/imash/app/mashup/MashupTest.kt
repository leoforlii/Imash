package com.imash.app.mashup

import com.imash.app.dsp.Tonalita
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class MashupTest {

    /** Brano intero di [secondi] s con misure ogni 2 s (120 BPM, 4/4). */
    private fun brano(secondi: Double, spb: Double = 0.5) = BranoDaPianificare(
        inizio = 0.0,
        fineMassima = secondi,
        misure = DoubleArray((secondi / (4 * spb)).toInt() + 1) { it * 4 * spb },
        spb = spb,
    )

    private fun ms(m: Int, s: Int) = m * 60.0 + s

    private val dissolvenza = Transizione(TipoTransizione.Dissolvenza, 2)

    /** Stessa transizione fra tutti i brani. */
    private fun tutte(brani: List<BranoDaPianificare>, t: Transizione = dissolvenza) = List(brani.size - 1) { t }

    @Test
    fun esempioDellUtenteTroppeCanzoni() {
        // 3:23 + 4:12 + 1:35 + 4:40 + 3:20 = 17:10 con durata 10 minuti → avviso.
        val brani = listOf(ms(3, 23), ms(4, 12), ms(1, 35), ms(4, 40), ms(3, 20)).map { brano(it) }
        val esito = DurationPlanner.pianifica(brani, tutte(brani), 600.0)
        assertTrue(esito is EsitoPiano.TroppeCanzoni)
        assertEquals(720.0, (esito as EsitoPiano.TroppeCanzoni).massimo, 1e-9)
    }

    @Test
    fun entroIl20PercentoAccorciaSulleMisure() {
        // 11:20 di brani: si accorciano fino a 10:00 ± 15 s, sempre su un inizio di misura.
        val brani = listOf(ms(3, 0), ms(2, 50), ms(3, 0), ms(2, 30)).map { brano(it) }
        val esito = DurationPlanner.pianifica(brani, tutte(brani), 600.0) as EsitoPiano.Ok
        assertEquals(600.0, esito.piano.totale, DurationPlanner.TOLLERANZA)
        assertTrue(!esito.piuCorto)
        esito.piano.segmenti.forEach { s ->
            val misure = s.durata / 2.0
            assertEquals("taglio non su una misura: ${s.durata}", Math.round(misure).toDouble(), misure, 1e-9)
        }
    }

    @Test
    fun braniCortiRestanoInteriESegnalati() {
        val brani = listOf(brano(120.0), brano(100.0))
        val esito = DurationPlanner.pianifica(brani, tutte(brani), 600.0) as EsitoPiano.Ok
        assertTrue(esito.piuCorto)
        assertEquals(listOf(120.0, 100.0), esito.piano.segmenti.map { it.durata })
        // Dissolvenza = 2 misure = 8 battiti da 0,5 s = 4 s.
        assertEquals(4.0, esito.piano.dissolvenze[0], 1e-9)
        assertEquals(216.0, esito.piano.totale, 1e-9)
    }

    @Test
    fun taglioAFineFraseEMixSulleUltimeMisure() {
        // 4 brani da 3:00 a 120 BPM, mix di 8 misure (16 s): tagli ogni 8 misure o alla fine naturale.
        val brani = List(4) { brano(180.0) }
        val mix = Transizione(TipoTransizione.MixATempo, 8)
        val esito = DurationPlanner.pianifica(brani, tutte(brani, mix), 600.0) as EsitoPiano.Ok
        assertEquals(600.0, esito.piano.totale, DurationPlanner.TOLLERANZA)
        esito.piano.segmenti.forEach { s ->
            val frasi = s.durata / 16.0
            assertTrue("taglio non a fine frase: ${s.durata}", abs(frasi - Math.round(frasi)) < 1e-9 || s.durata == 180.0)
        }
        esito.piano.dissolvenze.forEach { assertEquals(16.0, it, 1e-9) }
        assertEquals(List(3) { TipoTransizione.MixATempo }, esito.piano.transizioni)
    }

    @Test
    fun senzaFrasiPossibiliTagliaSulleMisure() {
        // Con passi da 16 s non si entra in 600 ± 15: si ripiega sulle misure da 2 s.
        val brani = listOf(brano(330.0), brano(331.0))
        val esito = DurationPlanner.pianifica(brani, tutte(brani), 600.0) as EsitoPiano.Ok
        assertEquals(600.0, esito.piano.totale, DurationPlanner.TOLLERANZA)
    }

    // ---------- Scelta della transizione ----------

    private fun analisi(spb: Double?, tonalita: Tonalita?) = AnalisiBrano(
        durata = 200.0, inizioSuono = 0.0, fineSuono = 200.0,
        misure = spb?.let { doubleArrayOf(0.0, 4 * it) }, spb = spb, bpm = spb?.let { 60 / it } ?: 0.0,
        tonalita = tonalita, energia = FloatArray(1), secondiPerBlocco = 1.0,
    )

    private val doMagg = Tonalita(0, false)
    private val solMagg = Tonalita(7, false)

    @Test
    fun stessoTempoETonalitaCompatibiliMixLungo() {
        val t = SceltaTransizione.scegli(analisi(0.5, doMagg), analisi(0.501, solMagg))
        assertEquals(Transizione(TipoTransizione.MixATempo, 8), t)
    }

    @Test
    fun tempoUnPoDiversoMixPiuCorto() {
        // 0,003 s per battito: 32 battiti = 0,096 s di scarto (troppo), 16 = 0,048 s (va bene).
        val t = SceltaTransizione.scegli(analisi(0.5, doMagg), analisi(0.503, doMagg))
        assertEquals(Transizione(TipoTransizione.MixATempo, 4), t)
    }

    @Test
    fun tonalitaIncompatibiliScambioDeiBassi() {
        // Il caso dei brani 1 → 2 dell'utente: Si minore → La maggiore.
        val t = SceltaTransizione.scegli(analisi(0.488, Tonalita(11, true)), analisi(0.488, Tonalita(9, false)))
        assertEquals(Transizione(TipoTransizione.ScambioBassi, 4), t)
    }

    @Test
    fun senzaBattitiDissolvenzaSemplice() {
        val t = SceltaTransizione.scegli(analisi(null, doMagg), analisi(0.5, doMagg))
        assertEquals(TipoTransizione.Dissolvenza, t.tipo)
    }

    @Test
    fun dissolvenzaLimitataTra2e7Secondi() {
        assertEquals(2.5, DurationPlanner.dissolvenza(0.2), 1e-9)
        assertEquals(7.0, DurationPlanner.dissolvenza(1.5), 1e-9)
        assertEquals(4.0, DurationPlanner.dissolvenza(null), 1e-9)
    }

    @Test
    fun senzaBattitiTagliaASecondi() {
        val b = BranoDaPianificare(5.0, 60.0, misure = null, spb = null)
        val tagli = DurationPlanner.tagliPossibili(b)
        assertEquals(10.0, tagli.first(), 1e-9)
        assertTrue(tagli.last() <= 55.0)
    }

    // ---------- Renderer ----------

    private val sr = 1000

    /** Sorgente finta: il brano i è un valore costante 0,5 (sx) e -0,5 (dx) lungo [lunghezze]. */
    private fun sorgente(lunghezze: IntArray) = SorgenteBrani { i, max, r ->
        val n = minOf(lunghezze[i].toLong(), max).toInt()
        r.ricevi(arrayOf(FloatArray(n) { 0.5f }, FloatArray(n) { -0.5f }), n)
    }

    private fun renderizza(piano: Piano, guadagni: FloatArray, lunghezze: IntArray): Array<FloatArray> {
        val r = MashupRenderer(piano, guadagni, sorgente(lunghezze), sr)
        val sx = ArrayList<Float>()
        val dx = ArrayList<Float>()
        r.renderizza({ b, n -> for (i in 0 until n) { sx.add(b[0][i]); dx.add(b[1][i]) } })
        assertEquals(r.campioniTotali, sx.size.toLong())
        return arrayOf(sx.toFloatArray(), dx.toFloatArray())
    }

    @Test
    fun lunghezzaFadeEDissolvenzaEqualPower() {
        // Due brani da 20 s, il primo da 2 s del file; dissolvenza 4 s.
        val piano = Piano(listOf(Segmento(2.0, 20.0), Segmento(0.0, 20.0)), doubleArrayOf(4.0))
        val out = renderizza(piano, floatArrayOf(1f, 1f), intArrayOf(30_000, 30_000))
        assertEquals(36 * sr, out[0].size)
        assertEquals(0f, out[0].first(), 1e-6f)                 // fade-in
        assertEquals(0f, out[0].last(), 1e-6f)                  // fade-out
        assertEquals(0.5f, out[0][10 * sr], 1e-6f)              // dentro il primo brano
        // Al centro della dissolvenza equal-power: 0,5·cos(π/4) + 0,5·sin(π/4) = 0,5·√2.
        assertEquals((0.5 * sqrt(2.0)).toFloat(), out[0][18 * sr], 2e-3f)
        assertEquals(-0.5f, out[1][25 * sr], 1e-6f)
    }

    @Test
    fun scambioDeiBassiPassaIlBassoAMeta() {
        // Brano 1 = continua 0,5, brano 2 = continua -0,3 (tutto "basso"); transizione di 8 s.
        val piano = Piano(
            listOf(Segmento(0.0, 20.0), Segmento(0.0, 20.0)), doubleArrayOf(8.0), listOf(TipoTransizione.ScambioBassi),
        )
        val valori = floatArrayOf(0.5f, -0.3f)
        val sorgente = SorgenteBrani { i, max, r ->
            val n = max.toInt()
            r.ricevi(arrayOf(FloatArray(n) { valori[i] }, FloatArray(n) { valori[i] }), n)
        }
        val sx = ArrayList<Float>()
        MashupRenderer(piano, floatArrayOf(1f, 1f), sorgente, sr).renderizza({ b, n -> for (i in 0 until n) sx.add(b[0][i]) })
        assertEquals(0.5f, sx[14 * sr], 1e-2f)     // primo quarto della transizione: basso del brano 1
        assertEquals(-0.3f, sx[18 * sr], 1e-2f)    // ultimo quarto: basso del brano 2
    }

    @Test
    fun guadagnoPerSegmento() {
        val piano = Piano(listOf(Segmento(0.0, 10.0), Segmento(0.0, 10.0)), doubleArrayOf(2.5))
        val out = renderizza(piano, floatArrayOf(2f, 0.5f), intArrayOf(10_000, 10_000))
        assertEquals(1.0f, out[0][4 * sr], 1e-6f)
        assertEquals(0.25f, out[0][12 * sr], 1e-6f)
    }

    @Test
    fun fileCortoCompletatoConSilenzio() {
        val piano = Piano(listOf(Segmento(0.0, 10.0), Segmento(0.0, 10.0)), doubleArrayOf(2.5))
        val out = renderizza(piano, floatArrayOf(1f, 1f), intArrayOf(5_000, 10_000))
        assertEquals(0f, out[0][6 * sr], 1e-6f)
    }

    @Test
    fun guadagnoPortaRmsA012() {
        assertEquals(0.12 / 0.3, MashupRenderer.guadagno(0.3).toDouble(), 1e-6)
    }

    // ---------- Analisi ----------

    @Test
    fun analisiTagliaIlSilenzioEMisuraRms() {
        val srA = 44_100
        val a = AnalizzatoreBrano(srA)
        // 2 s di silenzio, 6 s di onda quadra ±0,2, 2 s di silenzio.
        val n = srA * 10
        val sx = FloatArray(n) { i -> if (i in 2 * srA until 8 * srA) (if ((i / 50) % 2 == 0) 0.2f else -0.2f) else 0f }
        var pos = 0
        while (pos < n) {
            val k = minOf(4096, n - pos)
            a.ricevi(arrayOf(sx.copyOfRange(pos, pos + k), sx.copyOfRange(pos, pos + k)), k)
            pos += k
        }
        val r = a.concludi()
        assertEquals(10.0, r.durata, 1e-6)
        assertEquals(2.0, r.inizioSuono, 0.05)
        assertEquals(8.0, r.fineSuono, 0.05)
        assertEquals(0.2, r.rms(3.0, 7.0), 1e-3)
        assertTrue(abs(r.rms(0.0, 1.5)) < 1e-6)
    }

    @Test
    fun ilMixNonSuperaMaiIlLimite() {
        // Caso peggiore per lo scambio dei bassi: onde quadre lente a piena ampiezza, in opposizione,
        // più rumore; il limite deve valere per ogni campione di ogni tipo di transizione.
        val rnd = java.util.Random(7)
        val n = 25 * sr
        val segnali = listOf(
            FloatArray(n) { if ((it / 37) % 2 == 0) 1f else -1f },
            FloatArray(n) { if ((it / 41) % 2 == 0) -0.8f else 0.8f },
            FloatArray(n) { rnd.nextFloat() * 2 - 1 },
        )
        val sorgente = SorgenteBrani { i, max, r ->
            val m = minOf(n.toLong(), max).toInt()
            r.ricevi(arrayOf(segnali[i].copyOf(m), segnali[i].copyOf(m)), m)
        }
        val guadagni = floatArrayOf(1.3f, 0.9f, 1.1f)
        val picchi = floatArrayOf(1f, 0.8f, segnali[2].maxOf { abs(it) })
        val limite = MashupRenderer.limiteMix(guadagni, picchi)
        for (tipo in listOf(TipoTransizione.ScambioBassi, TipoTransizione.MixATempo)) {
            val piano = Piano(List(3) { Segmento(0.0, 20.0) }, doubleArrayOf(8.0, 8.0), listOf(tipo, tipo))
            var massimo = 0f
            MashupRenderer(piano, guadagni, sorgente, sr)
                .renderizza({ b, k -> for (i in 0 until k) massimo = maxOf(massimo, abs(b[0][i])) })
            assertTrue("$tipo: picco $massimo oltre il limite $limite", massimo <= limite)
        }
    }
}
