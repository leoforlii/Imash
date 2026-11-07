package com.imash.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imash.app.R
import com.imash.app.data.Archivio
import com.imash.app.mashup.Modalita
import com.imash.app.mashup.Piano
import com.imash.app.mashup.SceltaTransizione
import com.imash.app.mashup.TipoTransizione
import com.imash.app.servizio.Elaborazione
import com.imash.app.servizio.Esito
import com.imash.app.servizio.ServizioMashup
import com.imash.app.ui.tema.LocalColoriBrani
import com.imash.app.ui.tema.Spaziature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Schermata 3 del design: la barra diventa il lettore dell'anteprima, si salta da un brano
 * all'altro, si cambia la transizione di ogni passaggio e si salva l'mp3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataRisultato(esci: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stato by Elaborazione.stato.collectAsStateWithLifecycle()
    val s = stato
    val pronto = s?.esito as? Esito.Pronto
    val avvisi = remember { SnackbarHostState() }
    var conferma by remember { mutableStateOf(false) }
    val mp3 = remember { Archivio.fileTemporaneo(context) }
    val lettore = pronto?.let { rememberLettore(mp3, it) }
    val ricomponendo = s?.ricomposizione != null

    LaunchedEffect(Unit) { NotificationManagerCompat.from(context).cancel(ServizioMashup.ID_ESITO) }
    LaunchedEffect(ricomponendo) { if (ricomponendo) lettore?.pausa() }
    LaunchedEffect(s?.messaggio) {
        s?.messaggio?.let {
            Elaborazione.messaggioMostrato()
            avvisi.showSnackbar(it)
        }
    }
    val esciDavvero = {
        lettore?.pausa()
        Elaborazione.chiudi(context)
        esci()
    }
    BackHandler { if (s?.salvato == true) esciDavvero() else conferma = true }

    val salva = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Archivio.TIPO_MP3)) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching { withContext(Dispatchers.IO) { Archivio.copiaIn(context, mp3, uri) } }
            if (ok.isSuccess) Elaborazione.segnaSalvato()
            avvisi.showSnackbar(ok.fold({ "Mashup salvato" }, { "Errore nel salvataggio: ${it.message}" }))
        }
    }

    if (s == null || pronto == null) return
    val piano = pronto.risultato.piano
    val inizi = inizi(piano)
    val totaleSec = piano.totale
    val posizione = (lettore?.posizioneMs ?: 0) / 1000.0
    val corrente = inizi.indexOfLast { it <= posizione + 1e-3 }.coerceAtLeast(0)

    if (conferma) {
        AlertDialog(
            onDismissRequest = { conferma = false },
            title = { Text("Uscire senza salvare?") },
            text = { Text("Il mashup non è stato salvato e andrà perso.") },
            confirmButton = { TextButton(onClick = { conferma = false; esciDavvero() }) { Text("Esci") } },
            dismissButton = { TextButton(onClick = { conferma = false }) { Text("Resta") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mashup pronto") },
                navigationIcon = {
                    IconButton(onClick = { if (s.salvato) esciDavvero() else conferma = true }) {
                        Icon(painterResource(R.drawable.ic_indietro), contentDescription = "Torna ai brani")
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(Spaziature.sp4),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Button(
                        onClick = { salva.launch("mashup.mp3") },
                        enabled = !ricomponendo,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Icon(painterResource(R.drawable.ic_salva), contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text("Salva mp3")
                    }
                    TextButton(
                        onClick = { if (s.salvato) esciDavvero() else conferma = true },
                        enabled = !ricomponendo,
                    ) { Text("Nuovo mashup") }
                }
            }
        },
        snackbarHost = { SnackbarHost(avvisi) },
    ) { margini ->
        LazyColumn(Modifier.fillMaxSize().padding(margini)) {
            item {
                Column(Modifier.padding(horizontal = Spaziature.sp4)) {
                    Text(minSec(totaleSec), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    val cosa = if (s.modalita == Modalita.Ritornelli) "ritornelli uniti" else "canzoni unite"
                    Text(
                        "${s.nomi.size} $cosa" +
                            if (pronto.risultato.piuCorto) ": i brani non bastano per ${minSec(Elaborazione.durataObiettivo)}" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spaziature.sp4),
                    )
                    if (ricomponendo) Ricomposizione(s.ricomposizione)
                    BarraMashup(
                        durate = inizi.indices.map { i -> (inizi.getOrElse(i + 1) { totaleSec }) - inizi[i] },
                        scala = totaleSec,
                        descrizione = "Lettore: ${minSec(posizione)} di ${minSec(totaleSec)}",
                        testina = posizione,
                        sposta = if (lettore == null || ricomponendo) null else { sec -> lettore.vaiA((sec * 1000).roundToInt()) },
                    )
                    Row(Modifier.fillMaxWidth().padding(top = Spaziature.sp1), horizontalArrangement = Arrangement.SpaceBetween) {
                        val stile = MaterialTheme.typography.labelMedium
                        val colore = MaterialTheme.colorScheme.onSurfaceVariant
                        Text(minSec(posizione), style = stile, color = colore)
                        Text(minSec(totaleSec), style = stile, color = colore)
                    }
                    Comandi(lettore, abilitati = lettore != null && !ricomponendo)
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = Spaziature.sp4, end = Spaziature.sp4, top = Spaziature.sp4, bottom = Spaziature.sp2),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Nel mashup", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tocca per saltare", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(s.nomi.size) { i ->
                RigaNelMashup(
                    indice = i,
                    nome = s.nomi[i],
                    inizio = inizi[i],
                    inAscolto = lettore?.inRiproduzione == true && i == corrente,
                    abilitata = lettore != null && !ricomponendo,
                    salta = { lettore?.vaiA((inizi[i] * 1000).roundToInt()) },
                )
                if (i < s.nomi.size - 1) {
                    val analisi = pronto.risultato.analisi
                    Passaggio(
                        tipo = piano.transizioni[i],
                        secondi = piano.dissolvenze[i],
                        modificabile = SceltaTransizione.modificabile(analisi[i], analisi[i + 1]) && !ricomponendo,
                        scegli = { tipo -> Elaborazione.ricomponi(context, i, tipo) },
                    )
                }
            }
        }
    }
}

/** Inizio di ogni brano nel mashup (secondi): il brano i entra dove inizia la transizione dal precedente. */
internal fun inizi(piano: Piano): List<Double> {
    val inizi = ArrayList<Double>(piano.segmenti.size)
    var t = 0.0
    piano.segmenti.forEachIndexed { i, seg ->
        inizi += t
        t += seg.durata - piano.dissolvenze.getOrElse(i) { 0.0 }
    }
    return inizi
}

@Composable
private fun Ricomposizione(frazione: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().padding(bottom = Spaziature.sp4),
    ) {
        Column(Modifier.padding(Spaziature.sp4), verticalArrangement = Arrangement.spacedBy(Spaziature.sp2)) {
            Text("Aggiorno il mashup con la nuova transizione… ${(frazione * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(progress = { frazione.toFloat() }, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = Elaborazione::annulla, modifier = Modifier.align(Alignment.End)) { Text("Annulla") }
        }
    }
}

@Composable
private fun Comandi(lettore: Lettore?, abilitati: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Spaziature.sp3),
        horizontalArrangement = Arrangement.spacedBy(Spaziature.sp6, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { lettore?.salta(-10_000) }, enabled = abilitati) {
            Icon(painterResource(R.drawable.ic_indietro_10), contentDescription = "Indietro di 10 secondi")
        }
        FilledIconButton(
            onClick = { lettore?.alterna() },
            enabled = abilitati,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.size(72.dp),
        ) {
            val suona = lettore?.inRiproduzione == true
            Icon(
                painterResource(if (suona) R.drawable.ic_pausa else R.drawable.ic_play),
                contentDescription = if (suona) "Pausa" else "Ascolta",
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = { lettore?.salta(10_000) }, enabled = abilitati) {
            Icon(painterResource(R.drawable.ic_avanti_10), contentDescription = "Avanti di 10 secondi")
        }
    }
}

@Composable
private fun RigaNelMashup(indice: Int, nome: String, inizio: Double, inAscolto: Boolean, abilitata: Boolean, salta: () -> Unit) {
    val colori = LocalColoriBrani.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = abilitata, onClickLabel = "Ascolta da qui", onClick = salta)
            .background(if (inAscolto) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spaziature.sp4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spaziature.sp3),
    ) {
        Box(
            Modifier.size(32.dp).background(colori.perIndice(indice), MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) { Text("${indice + 1}", color = colori.testo, style = MaterialTheme.typography.labelLarge) }
        Text(nome, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(
            "da ${minSec(inizio)}${if (inAscolto) ", in ascolto" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Il passaggio fra due brani: transizione usata e, se ci sono i battiti, il menu per cambiarla. */
@Composable
private fun Passaggio(tipo: TipoTransizione, secondi: Double, modificabile: Boolean, scegli: (TipoTransizione) -> Unit) {
    var aperto by remember { mutableStateOf(false) }
    Box(Modifier.padding(start = 60.dp)) {
        TextButton(onClick = { aperto = true }, enabled = modificabile) {
            Text(
                "↳ ${tipo.nome} (${"%.1f".format(secondi)} s)" + if (tipo == TipoTransizione.Dissolvenza) ", senza battiti" else "",
                style = MaterialTheme.typography.labelLarge,
            )
            if (modificabile) Icon(painterResource(R.drawable.ic_espandi), contentDescription = "Cambia transizione", Modifier.size(18.dp))
        }
        DropdownMenu(expanded = aperto, onDismissRequest = { aperto = false }) {
            listOf(TipoTransizione.MixATempo, TipoTransizione.ScambioBassi).forEach { t ->
                DropdownMenuItem(
                    text = { Text(t.nome + if (t == tipo) " (attuale)" else "") },
                    enabled = t != tipo,
                    onClick = { aperto = false; scegli(t) },
                )
            }
        }
    }
}
