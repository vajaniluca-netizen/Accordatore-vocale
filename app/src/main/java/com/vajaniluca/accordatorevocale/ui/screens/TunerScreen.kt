package com.vajaniluca.accordatorevocale.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vajaniluca.accordatorevocale.AppViewModel
import com.vajaniluca.accordatorevocale.audio.PitchFrame
import com.vajaniluca.accordatorevocale.music.Notes
import com.vajaniluca.accordatorevocale.ui.components.CentsMeter
import com.vajaniluca.accordatorevocale.ui.components.DeviationLabel
import com.vajaniluca.accordatorevocale.ui.components.PitchGraph
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TunerScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val frame by vm.current.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val audioError by vm.audioError.collectAsStateWithLifecycle()

    // Tiene visibile l'ultima nota per un attimo dopo che smetti di cantare, per evitare sfarfallii.
    var lastVoiced by remember { mutableStateOf<PitchFrame?>(null) }
    LaunchedEffect(frame) { if (frame?.midi != null) lastVoiced = frame }
    val current = frame
    val held = lastVoiced
    val shown = when {
        current?.midi != null -> current
        current != null && held != null && current.timeSec - held.timeSec < 0.7 -> held
        else -> null
    }
    val stale = shown != null && shown !== current

    // Il grafico segue la voce spostandosi solo quando esci dalla zona centrale.
    var center by remember { mutableFloatStateOf(60f) }
    LaunchedEffect(current?.midi) {
        val m = current?.midi
        if (m != null && abs(m - center) > 4.5) center = m.roundToInt().toFloat()
    }
    val animatedCenter by animateFloatAsState(center, animationSpec = tween(400), label = "centro")

    Column(
        modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (audioError) ErrorBanner("Impossibile usare il microfono. Controlla che nessun'altra app lo stia usando.")

        val midi = shown?.midi
        val nearest = shown?.note ?: midi?.roundToInt()
        val cents = if (midi != null && nearest != null) (midi - nearest) * 100 else null
        val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (stale) 0.4f else 1f)

        Spacer(Modifier.height(8.dp))
        Text(
            text = nearest?.let { Notes.name(it, settings.notation) } ?: "–",
            fontSize = 76.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = shown?.frequency?.let { "%.1f Hz".format(it) } ?: "Canta una nota",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (nearest != null && settings.notation == com.vajaniluca.accordatorevocale.music.Notation.ITALIAN) {
                Text(
                    Notes.name(nearest, com.vajaniluca.accordatorevocale.music.Notation.ENGLISH),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        CentsMeter(cents?.toFloat(), settings.toleranceCents, Modifier.fillMaxWidth().height(56.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("−50", style = MaterialTheme.typography.labelSmall)
            Text("0", style = MaterialTheme.typography.labelSmall)
            Text("+50 cent", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(8.dp))
        DeviationLabel(if (stale) null else cents, settings.toleranceCents)
        Spacer(Modifier.height(12.dp))
        PitchGraph(
            points = vm.history,
            nowSec = current?.timeSec ?: 0.0,
            lowMidi = animatedCenter - 7f,
            highMidi = animatedCenter + 7f,
            notation = settings.notation,
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)),
        )
    }
}
