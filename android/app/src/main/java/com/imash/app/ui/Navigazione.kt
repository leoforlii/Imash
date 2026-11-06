package com.imash.app.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.imash.app.mashup.GeneratoreMashup
import com.imash.app.mashup.RisultatoMashup
import com.imash.app.data.Archivio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imash.app.R
import com.imash.app.ui.tema.LocalColoriBrani
import com.imash.app.ui.tema.Spaziature

/** Le schermate del design (design/schermate.html). */
enum class Schermata(val titolo: String) {
    Brani("Imash"),
    Elaborazione("Elaborazione"),
    Risultato("Mashup pronto"),
    Impostazioni("Impostazioni"),
}

@Composable
fun NavigazioneImash(nav: NavHostController = rememberNavController()) {
    NavHost(navController = nav, startDestination = Schermata.Brani.name) {
        Schermata.entries.forEach { schermata ->
            composable(schermata.name) {
                SchermataSegnaposto(
                    schermata = schermata,
                    puoTornare = schermata != Schermata.Brani,
                    indietro = { nav.popBackStack() },
                    vaiA = { nav.navigate(it.name) },
                )
            }
        }
    }
}

/**
 * Fase 1: segnaposto unico per tutte le schermate, per provare tema e navigazione.
 * Verrà sostituito dalle schermate vere nella fase 5.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchermataSegnaposto(
    schermata: Schermata,
    puoTornare: Boolean,
    indietro: () -> Unit,
    vaiA: (Schermata) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(schermata.titolo) },
                navigationIcon = {
                    if (puoTornare) {
                        IconButton(onClick = indietro) {
                            Icon(painterResource(R.drawable.ic_indietro), contentDescription = "Indietro")
                        }
                    }
                },
            )
        },
    ) { margini ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(margini)
                .padding(horizontal = Spaziature.sp4, vertical = Spaziature.sp6),
            verticalArrangement = Arrangement.spacedBy(Spaziature.sp4),
        ) {
            Text("Schermata provvisoria (fase 1)", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Qui arriverà la schermata «${schermata.titolo}» del design.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnteprimaColoriBrani()
            if (schermata == Schermata.Brani) ProvaMashup()
            Schermata.entries.filter { it != schermata }.forEachIndexed { i, altra ->
                if (i == 0) {
                    Button(onClick = { vaiA(altra) }) { Text("Vai a ${altra.titolo}") }
                } else {
                    OutlinedButton(onClick = { vaiA(altra) }) { Text("Vai a ${altra.titolo}") }
                }
            }
        }
    }
}

/**
 * Fase 3 (provvisorio, sostituito dalle schermate vere nella fase 5): sceglie i brani,
 * crea il mashup "Canzoni intere" di 10 minuti e lo salva con il "Salva con nome" di sistema.
 */
@Composable
private fun ProvaMashup() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stato by remember { mutableStateOf("Prova fase 3: scegli almeno 2 brani") }
    var pronto by remember { mutableStateOf(false) }
    val temporaneo = remember { Archivio.fileTemporaneo(context) }

    val salva = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(Archivio.TIPO_MP3),
    ) { uri ->
        if (uri != null) scope.launch {
            stato = runCatching { withContext(Dispatchers.IO) { Archivio.copiaIn(context, temporaneo, uri) } }
                .fold({ "Salvato" }, { "Errore nel salvataggio: ${it.message}" })
        }
    }
    val scegli = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        pronto = false
        scope.launch {
            val inizio = System.currentTimeMillis()
            stato = runCatching {
                withContext(Dispatchers.Default) {
                    GeneratoreMashup(context, uris).genera(GeneratoreMashup.DURATA_PREDEFINITA, temporaneo) { fase, i ->
                        stato = "${fase.name}: brano ${i + 1} di ${uris.size}…"
                    }
                }
            }.fold(
                { risultato ->
                    val sec = (System.currentTimeMillis() - inizio) / 1000.0
                    when (risultato) {
                        is RisultatoMashup.TroppeCanzoni ->
                            "Troppe canzoni: ${minSec(risultato.totale)} (massimo ${minSec(risultato.massimo)})"
                        is RisultatoMashup.Creato -> {
                            pronto = true
                            buildString {
                                append("Pronto in ${"%.0f".format(sec)} s – durata ${minSec(risultato.piano.totale)}")
                                if (risultato.piuCorto) append(" (brani troppo corti)")
                                risultato.piano.segmenti.forEachIndexed { i, s ->
                                    val a = risultato.analisi[i]
                                    append("\n${i + 1}. ${minSec(s.inizio)} → ${minSec(s.inizio + s.durata)}")
                                    append(" di ${minSec(a.durata)}, ${"%.0f".format(a.bpm)} BPM, ${a.tonalita ?: "?"}")
                                    if (i < risultato.piano.dissolvenze.size) {
                                        append("\n   ↳ ${risultato.piano.transizioni[i].nome}")
                                        append(" (${"%.1f".format(risultato.piano.dissolvenze[i])} s)")
                                    }
                                }
                            }
                        }
                    }
                },
                { e -> Log.e("Imash", "Mashup fallito", e); "Errore: ${e.javaClass.simpleName} ${e.message.orEmpty()}" },
            )
        }
    }

    Text(stato, style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(Spaziature.sp2)) {
        Button(onClick = { scegli.launch(Archivio.TIPI_INGRESSO) }) { Text("Scegli mp3") }
        OutlinedButton(enabled = pronto, onClick = { salva.launch("mashup.mp3") }) { Text("Salva mp3") }
    }
}

private fun minSec(secondi: Double): String {
    val s = secondi.toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

/** Prova dei colori dei brani: una barra a segmenti come la barra del mashup. */
@Composable
private fun AnteprimaColoriBrani() {
    val colori = LocalColoriBrani.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.extraSmall),
    ) {
        colori.lista.indices.forEach { i ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(colori.perIndice(i)),
            )
        }
    }
}
