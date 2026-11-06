package com.imash.app.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

/** Spaziature dai token --sp-* (griglia da 4 dp). */
object Spaziature {
    val sp1 = 4.dp
    val sp2 = 8.dp
    val sp3 = 12.dp
    val sp4 = 16.dp   // margine laterale delle schermate
    val sp6 = 24.dp
    val sp8 = 32.dp
    val sp12 = 48.dp
}

// Forme dai token --forma-*.
private val FormeImash = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TemaImash(scuro: Boolean = isSystemInDarkTheme(), contenuto: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalColoriBrani provides if (scuro) ColoriBraniScuro else ColoriBraniChiaro,
    ) {
        MaterialTheme(
            colorScheme = if (scuro) SchemaScuro else SchemaChiaro,
            typography = TipografiaImash,
            shapes = FormeImash,
            content = contenuto,
        )
    }
}
