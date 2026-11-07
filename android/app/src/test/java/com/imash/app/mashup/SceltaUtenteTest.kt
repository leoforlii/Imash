package com.imash.app.mashup

import com.imash.app.ui.inizi
import org.junit.Assert.assertEquals
import org.junit.Test

class SceltaUtenteTest {

    private fun brano(spb: Double?) = AnalisiBrano(200.0, 0.0, 200.0, null, spb, 120.0, null, FloatArray(1), 1.0)

    @Test
    fun mixATempoScelto() {
        // spb 0,5: 8 misure = 16 s, entro i 20 s.
        assertEquals(Transizione(TipoTransizione.MixATempo, 8), SceltaTransizione.conTipo(brano(0.5), brano(0.6), TipoTransizione.MixATempo))
        // spb 0,7: 8 misure = 22,4 s, troppo; 4 misure.
        assertEquals(Transizione(TipoTransizione.MixATempo, 4), SceltaTransizione.conTipo(brano(0.7), brano(0.5), TipoTransizione.MixATempo))
        assertEquals(Transizione(TipoTransizione.ScambioBassi, 4), SceltaTransizione.conTipo(brano(0.5), brano(0.5), TipoTransizione.ScambioBassi))
    }

    @Test
    fun senzaBattitiResteLaDissolvenza() {
        assertEquals(TipoTransizione.Dissolvenza, SceltaTransizione.conTipo(brano(null), brano(0.5), TipoTransizione.MixATempo).tipo)
    }

    @Test
    fun iniziNelMashup() {
        val piano = Piano(listOf(Segmento(0.0, 100.0), Segmento(5.0, 80.0), Segmento(0.0, 60.0)), doubleArrayOf(10.0, 8.0))
        assertEquals(listOf(0.0, 90.0, 162.0), inizi(piano))
        assertEquals(222.0, piano.totale, 1e-9)
    }
}
