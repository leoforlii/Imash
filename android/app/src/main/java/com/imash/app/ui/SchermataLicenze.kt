package com.imash.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import com.imash.app.R
import com.imash.app.data.Componente
import com.imash.app.data.Licenze
import com.imash.app.ui.tema.Spaziature

/** Componenti di terze parti inclusi nell'app e testo completo delle loro licenze. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchermataLicenze(indietro: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Licenze") },
                navigationIcon = {
                    IconButton(onClick = indietro) {
                        Icon(painterResource(R.drawable.ic_indietro), contentDescription = "Torna alle impostazioni")
                    }
                },
            )
        },
    ) { margini ->
        LazyColumn(
            Modifier.fillMaxSize().padding(margini),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spaziature.sp4),
            verticalArrangement = Arrangement.spacedBy(Spaziature.sp3),
        ) {
            item {
                Text(
                    "Imash usa questi componenti di terze parti, ognuno con la sua licenza.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(Licenze.componenti) { SchedaComponente(it) }
        }
    }
}

@Composable
private fun SchedaComponente(c: Componente) {
    val risorse = LocalResources.current
    val link = LocalUriHandler.current
    var aperta by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(Spaziature.sp4), verticalArrangement = Arrangement.spacedBy(Spaziature.sp1)) {
            Text(c.nome, style = MaterialTheme.typography.titleMedium)
            Text("${c.autori} · ${c.licenza}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            c.nota?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spaziature.sp1)) }
            Row(horizontalArrangement = Arrangement.spacedBy(Spaziature.sp2)) {
                TextButton(onClick = { runCatching { link.openUri(c.sito) } }) { Text("Sito") }
                TextButton(onClick = { aperta = !aperta }) { Text(if (aperta) "Nascondi licenza" else "Mostra licenza") }
            }
            if (aperta) {
                val testo = remember(c.testo) {
                    risorse.openRawResource(c.testo).bufferedReader().use { it.readText() }
                }
                Text(testo, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
