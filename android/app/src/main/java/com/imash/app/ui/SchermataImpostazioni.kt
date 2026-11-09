package com.imash.app.ui

import androidx.compose.foundation.layout.Arrangement
import com.imash.app.data.Licenze
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imash.app.R
import com.imash.app.data.Impostazioni
import com.imash.app.mashup.DurationPlanner
import com.imash.app.ui.tema.Spaziature
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Schermata 4 del design: durata del mashup con un cursore da 3 a 30 minuti, salvata con DataStore. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataImpostazioni(indietro: () -> Unit, apriLicenze: () -> Unit) {
    val context = LocalContext.current
    val impostazioni = remember { Impostazioni(context.applicationContext) }
    val salvati by impostazioni.durataMinuti.collectAsStateWithLifecycle(Impostazioni.DURATA_PREDEFINITA)
    val scope = rememberCoroutineScope()

    // Valore mostrato mentre si trascina; si salva solo quando si lascia il cursore.
    var cursore by remember { mutableFloatStateOf(salvati.toFloat()) }
    LaunchedEffect(salvati) { cursore = salvati.toFloat() }
    val minuti = cursore.roundToInt()
    val salva = { m: Int -> scope.launch { impostazioni.salvaDurata(m) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = {
                    IconButton(onClick = indietro) {
                        Icon(painterResource(R.drawable.ic_indietro), contentDescription = "Torna ai brani")
                    }
                },
            )
        },
    ) { margini ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(margini)
                .verticalScroll(rememberScrollState())
                .padding(Spaziature.sp4),
            verticalArrangement = Arrangement.spacedBy(Spaziature.sp3),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("Durata del mashup", style = MaterialTheme.typography.titleMedium)
                Text("$minuti min", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            Column {
                Slider(
                    value = cursore,
                    onValueChange = { cursore = it },
                    onValueChangeFinished = { salva(cursore.roundToInt()) },
                    valueRange = Impostazioni.DURATA_MIN.toFloat()..Impostazioni.DURATA_MAX.toFloat(),
                    steps = Impostazioni.DURATA_MAX - Impostazioni.DURATA_MIN - 1,   // passi di 1 minuto
                    modifier = Modifier.semantics {
                        contentDescription = "Durata del mashup"
                        stateDescription = "$minuti minuti"
                    },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val stile = MaterialTheme.typography.labelMedium
                    val colore = MaterialTheme.colorScheme.onSurfaceVariant
                    Text("${Impostazioni.DURATA_MIN} min", style = stile, color = colore)
                    Text("${Impostazioni.DURATA_MAX} min", style = stile, color = colore)
                }
            }
            Text(
                "In \"Canzoni intere\" i brani possono durare fino al 20% in più " +
                    "(con $minuti minuti: fino a ${minSec(minuti * 60 * DurationPlanner.MARGINE_MASSIMO)}) e vengono accorciati. " +
                    "Oltre, l'app ti chiede di togliere qualche brano.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = { cursore = Impostazioni.DURATA_PREDEFINITA.toFloat(); salva(Impostazioni.DURATA_PREDEFINITA) },
                enabled = minuti != Impostazioni.DURATA_PREDEFINITA,
            ) { Text("Ripristina ${Impostazioni.DURATA_PREDEFINITA} minuti") }
            Informazioni(apriLicenze)
        }
    }
}

/** Versione, privacy, diritto d'autore, codice sorgente e licenze (per distribuire l'app agli amici). */
@Composable
private fun Informazioni(apriLicenze: () -> Unit) {
    val context = LocalContext.current
    val link = LocalUriHandler.current
    val versione = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    HorizontalDivider(Modifier.padding(vertical = Spaziature.sp2))
    Text("Informazioni", style = MaterialTheme.typography.titleMedium)
    Text("Imash ${versione.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
    Text(
        "Privacy: Imash non raccoglie e non invia nessun dato. Funziona senza internet e tutto resta sul " +
            "telefono: i brani li scegli tu e i file di lavoro vengono cancellati.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "Diritto d'autore: i mashup sono per uso personale. Pubblicarli o condividerli con canzoni protette " +
            "può violare i diritti di chi le ha scritte e registrate.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spaziature.sp2)) {
        TextButton(onClick = apriLicenze) { Text("Licenze") }
        TextButton(onClick = { runCatching { link.openUri(Licenze.CODICE_SORGENTE) } }) { Text("Codice sorgente") }
    }
}
