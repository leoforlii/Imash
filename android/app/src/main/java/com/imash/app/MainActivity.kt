package com.imash.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.imash.app.data.Archivio
import com.imash.app.servizio.Elaborazione
import com.imash.app.ui.NavigazioneImash
import com.imash.app.ui.tema.TemaImash

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && Elaborazione.stato.value == null) Archivio.pulisciAvanzi(this)
        setContent {
            TemaImash {
                NavigazioneImash()
            }
        }
    }
}
