package com.imash.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.imash.app.R
import com.imash.app.data.Archivio
import com.imash.app.data.BranoScelto
import com.imash.app.mashup.Anteprima
import com.imash.app.mashup.Modalita
import com.imash.app.servizio.Elaborazione
import com.imash.app.ui.tema.LocalColoriBrani
import com.imash.app.ui.tema.Spaziature
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Minimo di brani per creare un mashup (design: "Crea mashup" disattivato con meno di 2). */
const val MIN_BRANI = 2

/** Schermata 1 del design: modalità, anteprima del mashup e lista dei brani riordinabile. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataBrani(
    apriImpostazioni: () -> Unit,
    creaMashup: () -> Unit,
    stato: StatoBrani = viewModel(),
) {
    val brani by stato.brani.collectAsStateWithLifecycle()
    val modalita by stato.modalita.collectAsStateWithLifecycle()
    val minuti by stato.durataMinuti.collectAsStateWithLifecycle()
    val saltati by stato.saltati.collectAsStateWithLifecycle()
    val anteprima = Anteprima.di(brani.map { it.durata }, modalita, minuti * 60.0)

    val scegli = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        stato.aggiungi(it)
    }
    val aggiungi = { scegli.launch(Archivio.TIPI_INGRESSO) }

    val context = LocalContext.current
    val avvia = {
        Elaborazione.avvia(context, brani, modalita, minuti * 60.0)
        creaMashup()
    }
    // Android 13+: il permesso per la notifica di avanzamento (se rifiutato si crea lo stesso).
    val permesso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { avvia() }
    val crea = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permesso.launch(Manifest.permission.POST_NOTIFICATIONS) else avvia()
    }

    val avvisi = remember { SnackbarHostState() }
    LaunchedEffect(saltati) {
        if (saltati > 0) {
            avvisi.showSnackbar(if (saltati == 1) "1 file non leggibile, saltato" else "$saltati file non leggibili, saltati")
            stato.saltatiMostrati()
        }
    }

    val lista = rememberLazyListState()
    val riordino = rememberReorderableLazyListState(lista) { da, a ->
        val i = brani.indexOfFirst { chiave(it) == da.key }
        val j = brani.indexOfFirst { chiave(it) == a.key }
        if (i >= 0 && j >= 0) stato.sposta(i, j)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Imash") },
                actions = {
                    IconButton(onClick = apriImpostazioni) {
                        Icon(painterResource(R.drawable.ic_impostazioni), contentDescription = "Impostazioni")
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Button(
                    onClick = crea,
                    enabled = brani.size >= MIN_BRANI && !anteprima.troppe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(Spaziature.sp4)
                        .heightIn(min = 56.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_crea), contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Crea mashup")
                }
            }
        },
        snackbarHost = { SnackbarHost(avvisi) },
    ) { margini ->
        LazyColumn(
            state = lista,
            contentPadding = PaddingValues(top = margini.calculateTopPadding(), bottom = margini.calculateBottomPadding() + Spaziature.sp4),
            verticalArrangement = Arrangement.spacedBy(Spaziature.sp1),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "modalita") { SceltaModalita(modalita, minuti, stato::scegliModalita) }
            if (brani.isEmpty()) {
                item(key = "vuoto") { StatoVuoto(aggiungi) }
                return@LazyColumn
            }
            if (anteprima.troppe) item(key = "troppe") { AvvisoTroppe(anteprima, minuti, apriImpostazioni) }
            item(key = "anteprima") { AnteprimaMashup(anteprima, brani.size, minuti) }
            item(key = "intestazione") {
                Sezione(
                    "${brani.size} ${if (brani.size == 1) "brano" else "brani"}",
                    if (anteprima.troppe) "${minSec(anteprima.totale)} in totale" else "Trascina per cambiare l'ordine",
                )
            }
            itemsIndexed(brani, key = { _, b -> chiave(b) }) { i, brano ->
                ReorderableItem(riordino, key = chiave(brano)) { trascinato ->
                    RigaBrano(i, brano, trascinato, { stato.togli(brano) }, Modifier.draggableHandle())
                }
            }
            item(key = "aggiungi") {
                FilledTonalButton(onClick = aggiungi, modifier = Modifier.padding(Spaziature.sp4, Spaziature.sp3)) {
                    Icon(painterResource(R.drawable.ic_aggiungi), contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Aggiungi mp3")
                }
            }
        }
    }
}

private fun chiave(b: BranoScelto) = b.uri.toString()

@Composable
private fun SceltaModalita(modalita: Modalita, minuti: Int, scegli: (Modalita) -> Unit) {
    Column(Modifier.padding(horizontal = Spaziature.sp4)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Modalita.entries.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = m == modalita,
                    onClick = { scegli(m) },
                    shape = SegmentedButtonDefaults.itemShape(i, Modalita.entries.size),
                ) { Text(if (m == Modalita.CanzoniIntere) "Canzoni intere" else "Ritornelli") }
            }
        }
        Text(
            if (modalita == Modalita.CanzoniIntere) "Ogni canzone viene accorciata sulle misure per arrivare a $minuti minuti."
            else "Da ogni canzone si prende il ritornello, poi i ritornelli vengono uniti fino a $minuti minuti.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spaziature.sp2, bottom = Spaziature.sp2),
        )
    }
}

@Composable
private fun StatoVuoto(aggiungi: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spaziature.sp6, vertical = Spaziature.sp12),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spaziature.sp3),
    ) {
        Icon(
            painterResource(R.drawable.ic_libreria), contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp),
        )
        Text("Nessun brano scelto", style = MaterialTheme.typography.titleLarge)
        Text(
            "Scegli almeno $MIN_BRANI mp3 dal telefono. Potrai cambiarne l'ordine trascinandoli.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = aggiungi) {
            Icon(painterResource(R.drawable.ic_aggiungi), contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Aggiungi mp3")
        }
    }
}

@Composable
private fun AvvisoTroppe(a: Anteprima, minuti: Int, apriImpostazioni: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.padding(Spaziature.sp4, Spaziature.sp2),
    ) {
        Row(Modifier.padding(Spaziature.sp4), horizontalArrangement = Arrangement.spacedBy(Spaziature.sp3)) {
            Icon(painterResource(R.drawable.ic_errore), contentDescription = null)
            Column {
                Text("Troppe canzoni per $minuti minuti", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Insieme durano ${minSec(a.totale)}, oltre il massimo di ${minSec(a.massimo)}. " +
                        "Togli qualche brano oppure aumenta la durata nelle impostazioni.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = apriImpostazioni,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.padding(top = Spaziature.sp1),
                ) {
                    Icon(painterResource(R.drawable.ic_impostazioni), contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Apri impostazioni")
                }
            }
        }
    }
}

@Composable
private fun AnteprimaMashup(a: Anteprima, quanti: Int, minuti: Int) {
    val risultato = minOf(a.totale, a.obiettivo)
    Column {
        Sezione("Anteprima del mashup", if (a.troppe) "${minSec(a.totale)} su ${minSec(a.obiettivo)}" else minSec(risultato))
        Column(Modifier.padding(horizontal = Spaziature.sp4)) {
            BarraMashup(
                durate = a.durate,
                scala = maxOf(a.totale, a.obiettivo),
                limite = if (a.troppe) a.massimo else null,
                descrizione = "Anteprima: $quanti brani in $minuti minuti",
            )
            Row(Modifier.fillMaxWidth().padding(top = Spaziature.sp2), horizontalArrangement = Arrangement.SpaceBetween) {
                val stile = MaterialTheme.typography.labelMedium
                val colore = MaterialTheme.colorScheme.onSurfaceVariant
                Text("0:00", style = stile, color = colore)
                Text(
                    when {
                        a.troppe -> "massimo ${minSec(a.massimo)}"
                        a.piuCorto -> "${minSec(a.totale)} di brani, meno di ${minSec(a.obiettivo)}"
                        else -> "${minSec(a.totale)} di brani, accorciati a ${minSec(a.obiettivo)}"
                    },
                    style = stile, color = colore,
                )
                Text(minSec(if (a.troppe) a.totale else risultato), style = stile, color = colore)
            }
        }
    }
}

@Composable
private fun Sezione(titolo: String, destra: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = Spaziature.sp4, end = Spaziature.sp4, top = Spaziature.sp4, bottom = Spaziature.sp2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titolo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(destra, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RigaBrano(
    indice: Int,
    brano: BranoScelto,
    trascinato: Boolean,
    togli: () -> Unit,
    maniglia: Modifier,
) {
    val colori = LocalColoriBrani.current
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (trascinato) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = if (trascinato) 3.dp else 0.dp,
        modifier = Modifier.padding(horizontal = Spaziature.sp2),
    ) {
        Row(
            Modifier
                .heightIn(min = 64.dp)
                .padding(start = Spaziature.sp1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spaziature.sp3),
        ) {
            Box(maniglia.size(40.dp, 48.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_trascina),
                    contentDescription = "Trascina ${brano.nome}",
                    tint = if (trascinato) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier
                    .size(32.dp)
                    .background(colori.perIndice(indice), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text("${indice + 1}", color = colori.testo, style = MaterialTheme.typography.labelLarge)
            }
            Column(Modifier.weight(1f)) {
                Text(brano.nome, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (brano.durata > 0) minSec(brano.durata) else "durata sconosciuta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = togli) {
                Icon(painterResource(R.drawable.ic_chiudi), contentDescription = "Rimuovi ${brano.nome}")
            }
        }
    }
}
