package com.imash.app.audio

import org.junit.Assert.assertThrows
import org.junit.Test

/** Valori assurdi letti da un file audio vengono rifiutati subito (scansione Vantage, mval-001). */
class ValoriAudioTest {

    @Test
    fun formatoAssurdoRifiutato() {
        assertThrows(IllegalArgumentException::class.java) { InfoAudio(0, 2) }
        assertThrows(IllegalArgumentException::class.java) { InfoAudio(-44_100, 2) }
        assertThrows(IllegalArgumentException::class.java) { InfoAudio(44_100, 0) }
        assertThrows(IllegalArgumentException::class.java) { InfoAudio(44_100, 64) }
        InfoAudio(44_100, 2)
        InfoAudio(8_000, 1)
    }

    @Test
    fun ricampionamentoDaZeroHzRifiutato() {
        assertThrows(IllegalArgumentException::class.java) { Resampler(0, 44_100, 2) { _, _ -> } }
    }
}
