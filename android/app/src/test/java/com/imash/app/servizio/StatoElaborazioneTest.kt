package com.imash.app.servizio

import com.imash.app.mashup.AnalisiBrano
import com.imash.app.mashup.Fase
import com.imash.app.mashup.MetodoRitornello
import com.imash.app.mashup.Modalita
import com.imash.app.mashup.Passo
import com.imash.app.mashup.Piano
import com.imash.app.mashup.Ritornello
import com.imash.app.mashup.Segmento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatoElaborazioneTest {

    private fun nuovo() = StatoElaborazione(listOf("a", "b", "c"), listOf(100.0, 100.0, 100.0), Modalita.Ritornelli, 0L)

    private fun analisi(metodo: MetodoRitornello?) = AnalisiBrano(
        100.0, 0.0, 100.0, null, null, 0.0, null, FloatArray(1), 1.0,
        metodo?.let { Ritornello(10.0, 70.0, it) },
    )

    @Test
    fun analisiInParallelo() {
        var s = nuovo().applica(Passo.AnalisiIniziata(0)).applica(Passo.AnalisiIniziata(1))
        assertNull(s.avanzamento[2].fase)
        assertTrue(s.avanzamento[0].attivo && s.avanzamento[1].attivo)
        s = s.applica(Passo.Lettura(0, 50.0))
        assertEquals(StatoElaborazione.PESO_ANALISI * 0.7 * 0.5 / 3, s.frazione, 1e-9)
        s = s.applica(Passo.AnalisiFinita(1, analisi(MetodoRitornello.Energia)))
        assertEquals(Fase.Taglio, s.avanzamento[1].fase)
        assertFalse(s.avanzamento[1].attivo)
        assertTrue(s.avanzamento[1].ritornelloNonTrovato)
        assertEquals(1, s.corrente)                       // un brano analizzato: "Brano 2 di 3"
        assertEquals(Fase.Analisi, s.fase)
    }

    @Test
    fun taglioEUnioneSegnanoIBraniFatti() {
        var s = nuovo()
        for (i in 0..2) s = s.applica(Passo.AnalisiIniziata(i)).applica(Passo.AnalisiFinita(i, analisi(MetodoRitornello.Ripetizione)))
        assertEquals(StatoElaborazione.PESO_ANALISI, s.frazione, 1e-9)
        // 3 tratti da 40 s con 2 dissolvenze da 10 s: 100 s, confini a 0,4 / 0,7 / 1.
        s = s.applica(Passo.Pianificato(Piano(List(3) { Segmento(0.0, 40.0) }, doubleArrayOf(10.0, 10.0))))
        assertEquals(listOf(0.4, 0.7, 1.0), s.confini!!.map { Math.round(it * 1000) / 1000.0 })
        s = s.applica(Passo.Taglio(1))
        assertEquals(Fase.Taglio, s.fase)
        assertEquals(Fase.Unione, s.avanzamento[0].fase)
        assertTrue(s.avanzamento[1].attivo)
        s = s.applica(Passo.Unione(0.5))
        assertTrue(s.avanzamento[0].fatto)
        assertFalse(s.avanzamento[1].fatto)
        assertTrue(s.avanzamento[1].attivo)
        assertEquals(1, s.corrente)
        assertEquals(0.6 + 0.25 + 0.15 * 0.5, s.frazione, 1e-9)
        s = s.concluso(Esito.Annullato)
        assertFalse(s.inCorso)
        assertTrue(s.avanzamento.none { it.attivo })
    }
}
