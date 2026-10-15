package com.vajaniluca.accordatorevocale.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vajaniluca.accordatorevocale.ui.theme.PitchColors
import kotlin.math.abs
import kotlin.math.roundToInt

/** Indicatore orizzontale da -50 a +50 cent con la zona verde della tolleranza. */
@Composable
fun CentsMeter(cents: Float?, toleranceCents: Int, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(cents ?: 0f, animationSpec = tween(250), label = "cent")
    val colors = MaterialTheme.colorScheme
    val track = colors.surfaceContainerHighest
    val tick = colors.onSurfaceVariant
    Canvas(modifier) {
        val h = size.height
        val w = size.width
        fun x(c: Float) = w / 2 + c.coerceIn(-50f, 50f) / 50f * (w / 2)
        val barTop = h * 0.3f
        val barHeight = h * 0.4f
        drawRoundRect(track, Offset(0f, barTop), Size(w, barHeight), CornerRadius(barHeight / 2))
        val tol = toleranceCents.toFloat().coerceAtMost(50f)
        drawRect(PitchColors.InTune.copy(alpha = 0.35f), Offset(x(-tol), barTop), Size(x(tol) - x(-tol), barHeight))
        for (c in -50..50 step 10) {
            val len = if (c == 0) h * 0.5f else h * 0.2f
            drawLine(tick.copy(alpha = 0.6f), Offset(x(c.toFloat()), h / 2 - len / 2), Offset(x(c.toFloat()), h / 2 + len / 2), 1.dp.toPx())
        }
        if (cents != null) {
            val color = PitchColors.forCents(animated.toDouble(), toleranceCents)
            drawLine(color, Offset(x(animated), 0f), Offset(x(animated), h), 5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/** Testo che dice se sei intonato, più alto o più basso. */
@Composable
fun DeviationLabel(cents: Double?, toleranceCents: Int, style: TextStyle = MaterialTheme.typography.titleLarge) {
    if (cents == null) {
        Text(" ", style = style)
        return
    }
    // Il numero va a passi di 5 cent: un valore che cambia a ogni frame sarebbe illeggibile.
    val rounded = (cents / 5).roundToInt() * 5
    val text = when {
        abs(cents) <= toleranceCents -> "✓ Intonato (${signed(rounded)} cent)"
        cents > 0 -> "▲ Più alto di $rounded cent"
        else -> "▼ Più basso di ${-rounded} cent"
    }
    Text(text, style = style, fontWeight = FontWeight.SemiBold, color = PitchColors.forCents(cents, toleranceCents))
}

fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

/**
 * Barra del volume del microfono da -70 a 0 dB, con una tacca sulla soglia del filtro rumore:
 * la voce viene ascoltata solo quando la barra supera la tacca.
 */
@Composable
fun LevelBar(levelDb: Double?, thresholdDb: Double?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val track = colors.surfaceContainerHighest
    val quiet = colors.outline
    val marker = colors.onSurface
    Canvas(modifier) {
        fun x(db: Double) = (((db + 70.0) / 70.0).coerceIn(0.0, 1.0) * size.width).toFloat()
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        if (levelDb != null) {
            val above = thresholdDb != null && levelDb > thresholdDb
            drawRoundRect(if (above) PitchColors.InTune else quiet, size = Size(x(levelDb), size.height), cornerRadius = r)
        }
        if (thresholdDb != null) {
            val tx = x(thresholdDb)
            drawLine(marker, Offset(tx, -size.height), Offset(tx, size.height * 2), 2.dp.toPx())
        }
    }
}
