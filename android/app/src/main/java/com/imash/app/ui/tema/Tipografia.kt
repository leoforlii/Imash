package com.imash.app.ui.tema

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.imash.app.R

// Font variabili inclusi in res/font (licenza OFL), uno per ogni peso usato dai token.
@OptIn(ExperimentalTextApi::class)
private fun famiglia(res: Int, vararg pesi: Int) = FontFamily(
    pesi.map { peso ->
        Font(
            resId = res,
            weight = FontWeight(peso),
            variationSettings = FontVariation.Settings(FontVariation.weight(peso)),
        )
    },
)

private val Display = famiglia(R.font.bricolage_grotesque, 400, 600)
private val Testo = famiglia(R.font.figtree, 400, 600)

/** Un TextStyle dal formato dei token: peso, dimensione / interlinea. */
private fun stile(famiglia: FontFamily, peso: Int, dimensione: Int, interlinea: Int) = TextStyle(
    fontFamily = famiglia,
    fontWeight = FontWeight(peso),
    fontSize = dimensione.sp,
    lineHeight = interlinea.sp,
)

// Scala da design/tokens.css (--tipo-*).
internal val TipografiaImash = Typography(
    displayLarge = stile(Display, 400, 57, 64),
    displayMedium = stile(Display, 600, 45, 52),
    displaySmall = stile(Display, 600, 36, 44),
    headlineLarge = stile(Display, 600, 32, 40),
    headlineMedium = stile(Display, 600, 28, 36),
    headlineSmall = stile(Display, 600, 24, 32),
    titleLarge = stile(Display, 600, 22, 28),
    titleMedium = stile(Testo, 600, 16, 24),
    titleSmall = stile(Testo, 600, 14, 20),
    bodyLarge = stile(Testo, 400, 16, 24),
    bodyMedium = stile(Testo, 400, 14, 20),
    bodySmall = stile(Testo, 400, 12, 16),
    labelLarge = stile(Testo, 600, 14, 20),
    labelMedium = stile(Testo, 600, 12, 16),
    labelSmall = stile(Testo, 600, 11, 16),
)

/** Cifre a larghezza fissa per durate e percentuali (tabular-nums). */
fun TextStyle.cifreFisse(): TextStyle = copy(fontFeatureSettings = "tnum")
