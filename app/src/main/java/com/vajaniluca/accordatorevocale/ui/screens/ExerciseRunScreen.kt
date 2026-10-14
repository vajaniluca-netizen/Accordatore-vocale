package com.vajaniluca.accordatorevocale.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vajaniluca.accordatorevocale.AppViewModel
import com.vajaniluca.accordatorevocale.music.Notes
import com.vajaniluca.accordatorevocale.ui.components.DeviationLabel
import com.vajaniluca.accordatorevocale.ui.components.GraphTarget
import com.vajaniluca.accordatorevocale.ui.components.PitchGraph
import com.vajaniluca.accordatorevocale.ui.components.SimpleTopBar
import com.vajaniluca.accordatorevocale.ui.components.signed
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun ExerciseRunScreen(
    vm: AppViewModel,
    exerciseId: String,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val frame by vm.current.collectAsStateWithLifecycle()

    val exercise = exercises.find { it.id == exerciseId }
    if (exercise == null) {
        Column(modifier) {
            SimpleTopBar("Esercizio", onBack = onBack)
            Text("Esercizio non trovato.", Modifier.padding(16.dp))
        }
        return
    }

    val active = session?.takeIf { it.exercise.id == exercise.id }
    LaunchedEffect(active?.result) {
        if (active?.result != null) {
            vm.cancelExercise()
            onFinished()
        }
    }
    val running = active != null && active.result == null
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    val transpose = vm.transposition(exercise.id)
    val shown = active?.exercise ?: exercise.transposed(transpose)
    val timeline = remember(shown) { shown.timeline() }
    val now = if (running) frame?.timeSec ?: 0.0 else 0.0
    val offset = active?.startSec ?: 0.4
    val relative = now - offset
    val targets = remember(timeline, offset) {
        timeline.mapNotNull { n -> n.midi?.let { GraphTarget(it, n.startSec + offset, n.endSec + offset) } }
    }
    val range = shown.midiRange() ?: 60..72
    var low = range.first - 3
    var high = range.last + 3
    if (high - low < 12) {
        val extra = 12 - (high - low)
        low -= extra / 2
        high += extra - extra / 2
    }

    Column(modifier.fillMaxSize()) {
        SimpleTopBar(exercise.name, onBack = {
            vm.cancelExercise()
            onBack()
        })
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Card(Modifier.fillMaxWidth().heightIn(min = 110.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val current = timeline.firstOrNull { relative >= it.startSec && relative < it.endSec }
                    val nextSung = timeline.firstOrNull { it.midi != null && it.startSec > relative }
                    val target = current?.midi
                    when {
                        !running -> {
                            Text("Premi Avvia e canta seguendo le barre.", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Le note scorrono da destra verso la linea verticale; la tua voce appare come una linea colorata: verde se intonata, arancio se vicina, rossa se lontana.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        relative < 0 -> {
                            Text("Si parte tra ${ceil(-relative).toInt()}…", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            nextSung?.midi?.let { Text("Prima nota: ${Notes.name(it, settings.notation)}") }
                        }
                        target == null -> {
                            Text("Pausa", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            nextSung?.midi?.let { Text("Prossima: ${Notes.name(it, settings.notation)}") }
                        }
                        else -> {
                            val sung = frame?.midi
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Nota", style = MaterialTheme.typography.labelMedium)
                                    Text(Notes.name(target, settings.notation), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Tu", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        (frame?.note ?: sung?.roundToInt())?.let { Notes.name(it, settings.notation) } ?: "–",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            DeviationLabel(sung?.let { Notes.cents(it, target) }, settings.toleranceCents)
                        }
                    }
                }
            }

            PitchGraph(
                points = if (running) vm.history else emptyList(),
                nowSec = now,
                lowMidi = low - 0.5f,
                highMidi = high + 0.5f,
                notation = settings.notation,
                windowSec = 7.0,
                playheadFraction = 0.3f,
                targets = targets,
                toleranceCents = settings.toleranceCents,
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp).clip(RoundedCornerShape(12.dp)),
            )

            if (!running) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Tonalità ${signed(transpose)}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(onClick = { vm.setTransposition(exercise.id, transpose - 12) }) { Text("−8ª") }
                    OutlinedButton(onClick = { vm.setTransposition(exercise.id, transpose - 1) }) { Text("−") }
                    OutlinedButton(onClick = { vm.setTransposition(exercise.id, transpose + 1) }) { Text("+") }
                    OutlinedButton(onClick = { vm.setTransposition(exercise.id, transpose + 12) }) { Text("+8ª") }
                }
                Button(
                    onClick = { vm.startExercise(exercise) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(52.dp),
                ) { Text("Avvia") }
            } else {
                OutlinedButton(
                    onClick = { vm.cancelExercise() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(52.dp),
                ) { Text("Interrompi") }
            }
        }
    }
}
