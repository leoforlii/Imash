package com.imash.app.ui.tema

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Valori copiati da design/tokens.css (unica fonte). Se cambiano lì, vanno aggiornati qui.

internal val SchemaChiaro = lightColorScheme(
    primary = Color(0xFF0E6378),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8EAFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF4C626A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6F0),
    onSecondaryContainer = Color(0xFF071E25),
    tertiary = Color(0xFFB3266F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E5),
    onTertiaryContainer = Color(0xFF3E0022),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5F8FA),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFF5F8FA),
    onSurface = Color(0xFF171C1F),
    onSurfaceVariant = Color(0xFF3F484C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF3F5),
    surfaceContainer = Color(0xFFE9EEF0),
    surfaceContainerHigh = Color(0xFFE3E8EB),
    surfaceContainerHighest = Color(0xFFDDE3E5),
    outline = Color(0xFF6F797C),
    outlineVariant = Color(0xFFBFC8CC),
    inverseSurface = Color(0xFF2C3134),
    inverseOnSurface = Color(0xFFECF1F4),
    inversePrimary = Color(0xFF8AD0EA),
    scrim = Color(0xFF000000),
)

internal val SchemaScuro = darkColorScheme(
    primary = Color(0xFF8AD0EA),
    onPrimary = Color(0xFF003543),
    primaryContainer = Color(0xFF004E5F),
    onPrimaryContainer = Color(0xFFB8EAFF),
    secondary = Color(0xFFB3CAD3),
    onSecondary = Color(0xFF1E333B),
    secondaryContainer = Color(0xFF354A52),
    onSecondaryContainer = Color(0xFFCFE6F0),
    tertiary = Color(0xFFFFB0CD),
    onTertiary = Color(0xFF640039),
    tertiaryContainer = Color(0xFF8D0053),
    onTertiaryContainer = Color(0xFFFFD9E5),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1A20),
    onBackground = Color(0xFFDEE3E6),
    surface = Color(0xFF0F1A20),
    onSurface = Color(0xFFDEE3E6),
    onSurfaceVariant = Color(0xFFBFC8CC),
    surfaceContainerLowest = Color(0xFF0A1419),
    surfaceContainerLow = Color(0xFF162227),
    surfaceContainer = Color(0xFF1B272C),
    surfaceContainerHigh = Color(0xFF253237),
    surfaceContainerHighest = Color(0xFF303D42),
    outline = Color(0xFF899296),
    outlineVariant = Color(0xFF3F484C),
    inverseSurface = Color(0xFFDEE3E6),
    inverseOnSurface = Color(0xFF2C3134),
    inversePrimary = Color(0xFF0E6378),
    scrim = Color(0xFF000000),
)

/** Colori dei brani (token --brano-1 … --brano-6): ciclo di 6, testo sopra = [testo]. */
@Immutable
data class ColoriBrani(val lista: List<Color>, val testo: Color) {
    fun perIndice(indice: Int): Color = lista[indice % lista.size]
}

internal val ColoriBraniChiaro = ColoriBrani(
    lista = listOf(
        Color(0xFF0E6378), Color(0xFFB3266F), Color(0xFF8A6100),
        Color(0xFF6750A4), Color(0xFF3F6B4A), Color(0xFFA04100),
    ),
    testo = Color(0xFFFFFFFF),
)

internal val ColoriBraniScuro = ColoriBrani(
    lista = listOf(
        Color(0xFF8AD0EA), Color(0xFFFFB0CD), Color(0xFFF1C048),
        Color(0xFFCFBCFF), Color(0xFFA5D3AE), Color(0xFFFFB68E),
    ),
    testo = Color(0xFF10202A),
)

val LocalColoriBrani = staticCompositionLocalOf { ColoriBraniChiaro }
