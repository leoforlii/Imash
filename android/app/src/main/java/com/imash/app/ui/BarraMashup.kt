package com.imash.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.imash.app.ui.tema.LocalColoriBrani

/**
 * La barra del mashup (design: `.barra-mashup`): un segmento per brano, largo in proporzione
 * a [durate] (secondi) su una barra che rappresenta [scala] secondi, sfumato sulle giunture.
 * Con [limite] la parte oltre quel punto (secondi) è a strisce con il bordo di errore (`.limite`).
 * Con [stati] (elaborazione) i brani in corso sono a strisce e quelli in attesa vuoti.
 * Con [testina] (risultato) mostra la posizione di ascolto in secondi; con [sposta] un tocco o un
 * trascinamento sulla barra sceglie la posizione.
 */
@Composable
fun BarraMashup(
    durate: List<Double>,
    scala: Double,
    descrizione: String,
    modifier: Modifier = Modifier,
    limite: Double? = null,
    stati: List<StatoSegmento>? = null,
    testina: Double? = null,
    sposta: ((Double) -> Unit)? = null,
    altezza: Dp = 28.dp,
) {
    if (testina == null && sposta == null) {
        Barra(durate, scala, descrizione, modifier, limite, stati, altezza)
        return
    }
    val colore = MaterialTheme.colorScheme.onSurface
    val bordo = MaterialTheme.colorScheme.surface
    // `.barra-contenitore`: 4 dp sopra e sotto per la testina che sporge dalla barra.
    Box(
        modifier
            .fillMaxWidth()
            .height(altezza + 8.dp)
            .then(
                if (sposta == null || scala <= 0) Modifier
                else Modifier
                    .pointerInput(scala) { detectTapGestures { sposta((it.x / size.width * scala).coerceIn(0.0, scala)) } }
                    .pointerInput(scala) {
                        detectHorizontalDragGestures { cambio, _ ->
                            sposta((cambio.position.x / size.width * scala).coerceIn(0.0, scala))
                        }
                    },
            ),
    ) {
        Barra(durate, scala, descrizione, Modifier.padding(vertical = 4.dp), limite, stati, altezza)
        if (testina != null && scala > 0) {
            Canvas(Modifier.fillMaxSize()) {
                val x = (testina / scala).toFloat().coerceIn(0f, 1f) * size.width
                val l = 4.dp.toPx()
                val raggio = CornerRadius(l / 2)
                val b = 2.dp.toPx()
                drawRoundRect(bordo, Offset(x - l / 2 - b, 0f), Size(l + 2 * b, size.height), CornerRadius(l))
                drawRoundRect(colore, Offset(x - l / 2, b), Size(l, size.height - 2 * b), raggio)
            }
        }
    }
}

@Composable
private fun Barra(
    durate: List<Double>,
    scala: Double,
    descrizione: String,
    modifier: Modifier,
    limite: Double?,
    stati: List<StatoSegmento>?,
    altezza: Dp,
) {
    val colori = LocalColoriBrani.current
    val fondo = MaterialTheme.colorScheme.surfaceContainerHighest
    val strisce = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    val errore = MaterialTheme.colorScheme.error
    Canvas(
        modifier
            .fillMaxWidth()
            .height(altezza)
            .clip(MaterialTheme.shapes.extraSmall)
            .semantics { contentDescription = descrizione },
    ) {
        drawRect(fondo)
        if (scala <= 0.0) return@Canvas
        val sfumatura = DISSOLVENZA.toPx()
        var inizio = 0.0
        durate.forEachIndexed { i, d ->
            val x0 = (inizio / scala * size.width).toFloat()
            val x1 = ((inizio + d) / scala * size.width).toFloat()
            val c = colori.perIndice(i)
            val stato = stati?.getOrNull(i) ?: StatoSegmento.Fatto
            if (stato == StatoSegmento.Attesa) {
                // vuoto: resta il fondo
            } else if (stato == StatoSegmento.InCorso) {
                // `.segmento.in-corso`: strisce a 45° del colore e del colore sbiadito sul fondo.
                drawRect(lerp(fondo, c, 0.55f), Offset(x0, 0f), Size(x1 - x0, size.height))
                val passo = 12.dp.toPx() * 1.4142f
                clipRect(left = x0, right = x1) {
                    var k = x0 - size.height
                    while (k < x1) {
                        drawLine(c, Offset(k, size.height), Offset(k + size.height, 0f), 6.dp.toPx())
                        k += passo
                    }
                }
            } else if (i == 0 || stati?.getOrNull(i - 1) == StatoSegmento.Attesa) {
                drawRect(c, Offset(x0, 0f), Size(x1 - x0, size.height))
            } else {
                // Come `mask-image` nel CSS: il segmento entra sfumato sopra la fine del precedente.
                val s0 = x0 - sfumatura
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, c), startX = s0, endX = x0),
                    Offset(s0, 0f), Size(sfumatura, size.height),
                )
                drawRect(c, Offset(x0, 0f), Size(x1 - x0, size.height))
            }
            inizio += d
        }
        if (limite != null && limite < scala) {
            val x = (limite / scala * size.width).toFloat()
            // Strisce a 45° larghe 4 dp ogni 8 dp (misurati di traverso, come nel CSS).
            val passo = 8.dp.toPx() * 1.4142f
            clipRect(left = x) {
                var k = x - size.height
                while (k < size.width) {
                    drawLine(strisce, Offset(k, size.height), Offset(k + size.height, 0f), 4.dp.toPx())
                    k += passo
                }
            }
            drawRect(errore, Offset(x, 0f), Size(2.dp.toPx(), size.height))
        }
    }
}

private val DISSOLVENZA = 10.dp   // --dissolvenza-larghezza

/** Stato di un segmento durante l'elaborazione (design: fatto, in corso a strisce, in attesa). */
enum class StatoSegmento { Fatto, InCorso, Attesa }

/** Durata come m:ss (come nel design: 2:48, 10:00). */
fun minSec(secondi: Double): String {
    val s = secondi.toInt()
    return "%d:%02d".format(s / 60, s % 60)
}
