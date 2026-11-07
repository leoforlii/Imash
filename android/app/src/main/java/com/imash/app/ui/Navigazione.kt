package com.imash.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imash.app.data.Archivio
import com.imash.app.mashup.MetodoRitornello
import com.imash.app.servizio.Elaborazione
import com.imash.app.servizio.Esito
import com.imash.app.servizio.ServizioMashup
import com.imash.app.ui.tema.Spaziature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Le schermate del design (design/schermate.html). */
enum class Schermata { Brani, Elaborazione, Risultato, Impostazioni }

@Composable
fun NavigazioneImash(nav: NavHostController = rememberNavController()) {
    val tornaAiBrani = { nav.popBackStack(Schermata.Brani.name, inclusive = false); Unit }
    val versoRisultato = {
        nav.navigate(Schermata.Risultato.name) { popUpTo(Schermata.Brani.name) }
    }
    // Riaprendo l'app (per es. dalla notifica) si torna all'elaborazione in corso o al risultato.
    LaunchedEffect(Unit) {
        val s = Elaborazione.stato.value ?: return@LaunchedEffect
        if (s.esito is Esito.Pronto) versoRisultato() else nav.navigate(Schermata.Elaborazione.name)
    }
    NavHost(navController = nav, startDestination = Schermata.Brani.name) {
        composable(Schermata.Brani.name) {
            SchermataBrani(
                apriImpostazioni = { nav.navigate(Schermata.Impostazioni.name) },
                creaMashup = { nav.navigate(Schermata.Elaborazione.name) },
            )
        }
        composable(Schermata.Impostazioni.name) {
            SchermataImpostazioni(indietro = { nav.popBackStack() })
        }
        composable(Schermata.Elaborazione.name) {
            SchermataElaborazione(pronto = versoRisultato, tornaAiBrani = tornaAiBrani)
        }
        composable(Schermata.Risultato.name) {
            RisultatoProvvisorio(nuovoMashup = { Elaborazione.chiudi(); tornaAiBrani() })
        }
    }
}

/**
 * Passo 3 della fase 5 (provvisorio, sostituito dalla schermata Risultato nel passo 4):
 * riepilogo del piano e "Salva mp3" con il "Salva con nome" di sistema.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RisultatoProvvisorio(nuovoMashup: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stato by Elaborazione.stato.collectAsStateWithLifecycle()
    var messaggio by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { NotificationManagerCompat.from(context).cancel(ServizioMashup.ID_ESITO) }
    val temporaneo = remember { Archivio.fileTemporaneo(context) }
    val salva = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Archivio.TIPO_MP3)) { uri ->
        if (uri != null) scope.launch {
            messaggio = runCatching { withContext(Dispatchers.IO) { Archivio.copiaIn(context, temporaneo, uri) } }
                .fold({ "Salvato" }, { "Errore nel salvataggio: ${it.message}" })
        }
    }
    val s = stato ?: return
    val risultato = (s.esito as? Esito.Pronto)?.risultato ?: return

    Scaffold(topBar = { TopAppBar(title = { Text("Mashup pronto") }) }) { margini ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(margini)
                .padding(Spaziature.sp4)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spaziature.sp3),
        ) {
            Text(minSec(risultato.piano.totale), style = MaterialTheme.typography.displayMedium)
            Text(
                buildString {
                    append("Schermata provvisoria (passo 3)")
                    if (risultato.piuCorto) append(": brani troppo corti per la durata scelta")
                    risultato.piano.segmenti.forEachIndexed { i, seg ->
                        val a = risultato.analisi[i]
                        append("\n${i + 1}. ${s.nomi[i]}: ${minSec(seg.inizio)} → ${minSec(seg.inizio + seg.durata)}")
                        append(", ${"%.0f".format(a.bpm)} BPM, ${a.tonalita ?: "?"}")
                        a.ritornello?.let { r ->
                            val come = if (r.metodo == MetodoRitornello.Ripetizione) "ripetizione" else "energia"
                            append("\n   ritornello ${minSec(r.inizio)} → ${minSec(r.fine)} ($come)")
                        }
                        if (i < risultato.piano.dissolvenze.size) {
                            append("\n   ↳ ${risultato.piano.transizioni[i].nome}")
                            append(" (${"%.1f".format(risultato.piano.dissolvenze[i])} s)")
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (messaggio.isNotEmpty()) Text(messaggio, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Spaziature.sp2)) {
                Button(onClick = { salva.launch("mashup.mp3") }) { Text("Salva mp3") }
                OutlinedButton(onClick = nuovoMashup) { Text("Nuovo mashup") }
            }
        }
    }
}
