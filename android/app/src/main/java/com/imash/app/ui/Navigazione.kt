package com.imash.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.imash.app.servizio.Elaborazione
import com.imash.app.servizio.Esito


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
            SchermataRisultato(esci = tornaAiBrani)
        }
    }
}
