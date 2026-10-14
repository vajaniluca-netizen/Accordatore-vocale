package com.vajaniluca.accordatorevocale.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vajaniluca.accordatorevocale.audio.PitchPoint
import com.vajaniluca.accordatorevocale.music.Notation
import com.vajaniluca.accordatorevocale.music.Notes
import com.vajaniluca.accordatorevocale.ui.theme.PitchColors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Una nota da cantare, con tempi nella stessa scala dei punti del tracciato. */
data class GraphTarget(val midi: Int, val startSec: Double, val endSec: Double)

/**
 * Grafico a rullo di pianoforte: in verticale le note, in orizzontale il tempo.
 * La linea verticale è "adesso": le note da cantare arrivano da destra, la tua voce resta a sinistra.
 */
@Composable
fun PitchGraph(
    points: List<PitchPoint>,
    nowSec: Double,
    lowMidi: Float,
    highMidi: Float,
    notation: Notation,
    modifier: Modifier = Modifier,
    windowSec: Double = 6.0,
    playheadFraction: Float = 0.8f,
    targets: List<GraphTarget> = emptyList(),
    toleranceCents: Int = 20,
) {
    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = TextStyle(fontSize = 10.sp, color = colors.onSurfaceVariant)
    val background = colors.surfaceContainerLow
    val laneShade = colors.onSurface.copy(alpha = 0.05f)
    val lineColor = colors.outlineVariant
    val targetColor = colors.primary
    val freeLineColor = colors.tertiary
    val playheadColor = colors.onSurface.copy(alpha = 0.6f)

    Canvas(modifier.clipToBounds()) {
        drawRect(background)
        val labelWidth = 40.dp.toPx()
        val plotLeft = labelWidth
        val plotWidth = size.width - labelWidth
        val span = highMidi - lowMidi
        if (span <= 0f || plotWidth <= 0f) return@Canvas
        val laneHeight = size.height / span
        val playX = plotLeft + plotWidth * playheadFraction

        fun y(midi: Double): Float = (size.height * (highMidi - midi) / span).toFloat()
        fun x(t: Double): Float = (playX + (t - nowSec) / windowSec * plotWidth).toFloat()

        // Righe delle note, tasti neri più scuri, etichette a sinistra
        val showAllLabels = laneHeight >= 12.dp.toPx()
        for (m in ceil(lowMidi - 0.5f).toInt()..floor(highMidi + 0.5f).toInt()) {
            val yc = y(m.toDouble())
            if (Notes.isBlackKey(m)) {
                drawRect(laneShade, topLeft = Offset(plotLeft, yc - laneHeight / 2), size = Size(plotWidth, laneHeight))
            }
            drawLine(
                lineColor.copy(alpha = if (m % 12 == 0) 0.9f else 0.3f),
                Offset(plotLeft, yc),
                Offset(size.width, yc),
                strokeWidth = 1f,
            )
            if (showAllLabels || !Notes.isBlackKey(m)) {
                val layout = textMeasurer.measure(Notes.name(m, notation), labelStyle)
                val top = yc - layout.size.height / 2f
                if (top >= 0f && top + layout.size.height <= size.height) {
                    drawText(layout, topLeft = Offset(4.dp.toPx(), top))
                }
            }
        }

        clipRect(left = plotLeft) {
            // Note da cantare: fascia larga quanto la tolleranza, con la linea centrale esatta
            val bandHalf = max(toleranceCents / 100f * laneHeight, 3.dp.toPx())
            for (t in targets) {
                val x0 = x(t.startSec)
                val x1 = x(t.endSec)
                if (x1 < plotLeft || x0 > size.width) continue
                val active = nowSec >= t.startSec && nowSec < t.endSec
                val yc = y(t.midi.toDouble())
                val left = max(x0, plotLeft - 10f)
                val right = min(x1, size.width + 10f) - 2.dp.toPx()
                if (right <= left) continue
                drawRoundRect(
                    targetColor.copy(alpha = if (active) 0.35f else 0.18f),
                    topLeft = Offset(left, yc - bandHalf),
                    size = Size(right - left, bandHalf * 2),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                drawLine(
                    targetColor.copy(alpha = if (active) 1f else 0.6f),
                    Offset(left, yc),
                    Offset(right, yc),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Tracciato della voce, colorato in base allo scostamento dalla nota attesa
            val stroke = 3.dp.toPx()
            var previous: PitchPoint? = null
            for (p in points) {
                val m = p.midi
                val prev = previous
                if (m != null && prev?.midi != null && p.timeSec - prev.timeSec < 0.08) {
                    val color = if (targets.isEmpty()) {
                        freeLineColor
                    } else {
                        val target = targets.firstOrNull { p.timeSec >= it.startSec && p.timeSec < it.endSec }
                        if (target == null) freeLineColor.copy(alpha = 0.5f)
                        else PitchColors.forCents((m - target.midi) * 100.0, toleranceCents)
                    }
                    drawLine(
                        color,
                        Offset(x(prev.timeSec), y(prev.midi)),
                        Offset(x(p.timeSec), y(m)),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
                previous = if (m != null) p else null
            }

            val last = points.lastOrNull()
            if (last?.midi != null && nowSec - last.timeSec < 0.15) {
                drawCircle(playheadColor, radius = 5.dp.toPx(), center = Offset(x(last.timeSec), y(last.midi)))
            }
        }

        drawLine(playheadColor, Offset(playX, 0f), Offset(playX, size.height), strokeWidth = 1.5.dp.toPx())
    }
}
