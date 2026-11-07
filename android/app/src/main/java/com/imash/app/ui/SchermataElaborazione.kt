package com.imash.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imash.app.R
import com.imash.app.mashup.Fase
import com.imash.app.mashup.Modalita
import com.imash.app.servizio.AvanzamentoBrano
import com.imash.app.servizio.Elaborazione
import com.imash.app.servizio.Esito
import com.imash.app.servizio.StatoElaborazione
import com.imash.app.ui.tema.Spaziature
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Schermata 2 del design: avanzamento brano per brano, con le fasi Analisi, Taglio, Unione. */
@Composable
fun SchermataElaborazione(pronto: () -> Unit, tornaAiBrani: () -> Unit) {
    val context = LocalContext.current
    val stato by Elaborazione.stato.collectAsStateWithLifecycle()
    val s = stato
    var conferma by remember { mutableStateOf(false) }

    LaunchedEffect(s?.esito) {
        when (s?.esito) {
            null -> if (s == null) tornaAiBrani()
            is Esito.Pronto -> pronto()
            Esito.Annullato -> { Elaborazione.chiudi(context); tornaAiBrani() }
            else -> Unit
        }
    }
    BackHandler(enabled = s?.inCorso == true) { conferma = true }
    if (s == null) return

    if (conferma) {
        AlertDialog(
            onDismissRequest = { conferma = false },
            title = { Text("Interrompere il mashup?") },
            text = { Text("Il lavoro fatto finora andrà perso.") },
            confirmButton = { TextButton(onClick = { conferma = false; Elaborazione.annulla() }) { Text("Interrompi") } },
            dismissButton = { TextButton(onClick = { conferma = false }) { Text("Continua") } },
        )
    }

    Scaffold(
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                val modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(Spaziature.sp4)
                    .heightIn(min = 56.dp)
                if (s.inCorso) {
                    OutlinedButton(onClick = { conferma = true }, modifier = modifier) { Text("Annulla") }
                } else {
                    Button(onClick = { Elaborazione.chiudi(context); tornaAiBrani() }, modifier = modifier) { Text("Torna ai brani") }
                }
            }
        },
    ) { margini ->
        LazyColumn(Modifier.fillMaxSize().padding(margini)) {
            item { Intestazione(s) }
            when (val e = s.esito) {
                is Esito.TroppeCanzoni -> item {
                    AvvisoErrore(
                        "Troppe canzoni",
                        "Dopo l'analisi i brani durano ${minSec(e.totale)}, oltre il massimo di ${minSec(e.massimo)}. " +
                            "Togli qualche brano oppure aumenta la durata nelle impostazioni.",
                    )
                }
                is Esito.Errore -> item { AvvisoErrore("Mashup non riuscito", e.messaggio) }
                else -> Unit
            }
            item {
                Text(
                    "Brani",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spaziature.sp4, top = Spaziature.sp6, bottom = Spaziature.sp2),
                )
            }
            itemsIndexed(s.nomi) { i, nome -> RigaPasso(nome, s.avanzamento[i], s.durate[i]) }
        }
    }
}

@Composable
private fun Intestazione(s: StatoElaborazione) {
    val percentuale = (s.frazione * 100).roundToInt()
    // Ridisegna ogni secondo per aggiornare la stima del tempo rimasto.
    var adesso by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(s.inCorso) {
        while (s.inCorso) {
            adesso = System.currentTimeMillis()
            delay(1000)
        }
    }
    Column(Modifier.padding(horizontal = Spaziature.sp4)) {
        Text(
            if (s.inCorso) "Sto creando il mashup" else "Mashup interrotto",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = Spaziature.sp6),
        )
        Text(
            "${if (s.modalita == Modalita.Ritornelli) "Ritornelli" else "Canzoni intere"}, ${s.nomi.size} brani",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spaziature.sp1, bottom = Spaziature.sp4),
        )
        Text(
            "$percentuale%",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "$percentuale per cento" },
        )
        BarraMashup(
            durate = s.nomi.map { 1.0 },
            scala = s.nomi.size.toDouble(),
            descrizione = "Avanzamento del mashup: $percentuale per cento",
            stati = s.avanzamento.map {
                when {
                    it.fatto -> StatoSegmento.Fatto
                    it.fase != null -> StatoSegmento.InCorso
                    else -> StatoSegmento.Attesa
                }
            },
            modifier = Modifier.padding(top = Spaziature.sp2),
        )
        Row(Modifier.fillMaxWidth().padding(top = Spaziature.sp2), horizontalArrangement = Arrangement.SpaceBetween) {
            val stile = MaterialTheme.typography.labelMedium
            val colore = MaterialTheme.colorScheme.onSurfaceVariant
            Text("Brano ${s.corrente + 1} di ${s.nomi.size}", style = stile, color = colore)
            if (s.inCorso) Text(tempoRimasto(adesso - s.inizio, s.frazione), style = stile, color = colore)
        }
    }
}

/** Stima dal tempo trascorso e dalla frazione fatta. */
private fun tempoRimasto(trascorsoMs: Long, frazione: Double): String {
    if (frazione < 0.05 || trascorsoMs < 3000) return "stima del tempo in corso"
    val sec = trascorsoMs / 1000.0 * (1 - frazione) / frazione
    return when {
        sec < 20 -> "pochi secondi rimasti"
        sec < 60 -> "meno di un minuto rimasto"
        sec < 90 -> "circa 1 minuto rimasto"
        else -> "circa ${(sec / 60).roundToInt()} minuti rimasti"
    }
}

@Composable
private fun AvvisoErrore(titolo: String, testo: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.padding(Spaziature.sp4, Spaziature.sp4, Spaziature.sp4, 0.dp),
    ) {
        Row(Modifier.padding(Spaziature.sp4), horizontalArrangement = Arrangement.spacedBy(Spaziature.sp3)) {
            Icon(painterResource(R.drawable.ic_errore), contentDescription = null)
            Column {
                Text(titolo, style = MaterialTheme.typography.titleSmall)
                Text(testo, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RigaPasso(nome: String, a: AvanzamentoBrano, durata: Double) {
    val iniziato = a.fase != null
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = Spaziature.sp4, vertical = Spaziature.sp2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spaziature.sp3),
    ) {
        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            when {
                a.fatto -> Icon(painterResource(R.drawable.ic_fatto), contentDescription = "Fatto", tint = MaterialTheme.colorScheme.primary)
                a.attivo -> CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.tertiary, strokeWidth = 3.dp)
                else -> Icon(painterResource(R.drawable.ic_attesa), contentDescription = "In attesa", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                nome,
                style = MaterialTheme.typography.bodyLarge,
                color = if (iniziato || a.fatto) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val nota = if (a.ritornelloNonTrovato) ". Ritornello non trovato: usato il tratto più intenso" else ""
            when {
                a.fatto -> Text("Unito$nota", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                !iniziato -> Text("In attesa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    Fasi(a)
                    if (a.attivo && a.fase == Fase.Analisi && durata > 0) {
                        LinearProgressIndicator(
                            progress = { minOf(1f, (a.letti / durata).toFloat()) },
                            modifier = Modifier.fillMaxWidth().padding(top = Spaziature.sp2),
                        )
                    }
                    if (nota.isNotEmpty()) {
                        Text(nota.removePrefix(". "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Le tre fasi come etichette (`.fasi`): fatte, quella attiva, quelle da fare. Mai solo colore: c'è il testo. */
@Composable
private fun Fasi(a: AvanzamentoBrano) {
    val corrente = a.fase ?: return
    Row(
        Modifier
            .padding(top = Spaziature.sp1)
            .semantics { contentDescription = "Fase: ${corrente.name.lowercase()}${if (a.attivo) " in corso" else " in attesa"}" },
        horizontalArrangement = Arrangement.spacedBy(Spaziature.sp1),
    ) {
        Fase.entries.forEach { f ->
            val (fondo, testo) = when {
                f < corrente -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                f == corrente && a.attivo -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                else -> Color.Transparent to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                f.name,
                style = MaterialTheme.typography.labelSmall,
                color = testo,
                modifier = Modifier
                    .background(fondo, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = Spaziature.sp2, vertical = 2.dp),
            )
        }
    }
}
