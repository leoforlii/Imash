package com.imash.app.mashup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnteprimaTest {

    @Test
    fun esempioDelDesignSonoTroppeCanzoni() {
        // 3:23 + 4:12 + 1:35 + 4:40 + 3:20 = 17:10 contro un massimo di 12:00.
        val a = Anteprima.di(listOf(203.0, 252.0, 95.0, 280.0, 200.0), Modalita.CanzoniIntere, 600.0)
        assertEquals(1030.0, a.totale, 1e-9)
        assertEquals(720.0, a.massimo, 1e-9)
        assertTrue(a.troppe)
    }

    @Test
    fun entroIlVentiPerCentoSiAccorcia() {
        val a = Anteprima.di(listOf(168.0, 185.0, 151.0, 156.0), Modalita.CanzoniIntere, 600.0)   // 11:00
        assertFalse(a.troppe)
        assertFalse(a.piuCorto)
    }

    @Test
    fun ritornelliContanoAlMassimoSessantaSecondi() {
        val a = Anteprima.di(listOf(200.0, 45.0, 300.0), Modalita.Ritornelli, 600.0)
        assertEquals(listOf(60.0, 45.0, 60.0), a.durate)
        assertTrue(a.piuCorto)
        assertFalse(a.troppe)
    }
}
