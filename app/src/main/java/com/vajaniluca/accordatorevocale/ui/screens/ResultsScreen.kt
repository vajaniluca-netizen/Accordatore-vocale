package com.vajaniluca.accordatorevocale.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vajaniluca.accordatorevocale.AppViewModel
import com.vajaniluca.accordatorevocale.exercise.NoteResult
import com.vajaniluca.accordatorevocale.music.Notation
import com.vajaniluca.accordatorevocale.music.Notes
import com.vajaniluca.accordatorevocale.ui.components.SimpleTopBar
import com.vajaniluca.accordatorevocale.ui.components.signed
import com.vajaniluca.accordatorevocale.ui.theme.PitchColors
import kotlin.math.roundToInt

@Composable
fun ResultsScreen(
    vm: AppViewModel,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result by vm.lastResult.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val r = result

    Column(modifier.fillMaxSize()) {
        SimpleTopBar("Risultato", onBack = onClose)
        if (r == null) {
            Text("Nessun risultato disponibile.", Modifier.padding(16.dp))
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(r.exerciseName, style = MaterialTheme.typography.titleMedium)
                        val pct = (r.inTuneFraction * 100).roundToInt()
                        Text("$pct%", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        Text("del tempo entro ±${r.toleranceCents} cent dalla nota giusta")
                        val mean = r.meanCents
                        if (mean != null) {
                            val tendency = when {
                                mean > 5 -> "tendi a cantare crescente (troppo alto)"
                                mean < -5 -> "tendi a cantare calante (troppo basso)"
                                else -> "nessuna tendenza evidente verso l'alto o il basso"
                            }
                            Text(
                                "Scostamento medio ${signed(mean.roundToInt())} cent: $tendency.",
                                Modifier.padding(top = 8.dp),
                            )
                            r.meanAbsCents?.let { Text("Errore medio assoluto: ${it.roundToInt()} cent.") }
                        } else {
                            Text("Non ho rilevato la voce durante l'esercizio.", Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
            item {
                Text("Nota per nota", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            }
            items(r.notes) { NoteResultRow(it, r.toleranceCents, settings.notation) }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Esercizi") }
            Button(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Riprova") }
        }
    }
}

@Composable
private fun NoteResultRow(result: NoteResult, toleranceCents: Int, notation: Notation) {
    val midi = result.note.midi ?: return
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${result.note.index + 1}.", Modifier.width(32.dp), style = MaterialTheme.typography.labelMedium)
            Text(Notes.name(midi, notation), Modifier.width(64.dp), fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f)) {
                val cents = result.medianCents
                if (cents == null) {
                    Text("non cantata", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val r = cents.roundToInt()
                    val description = when {
                        kotlin.math.abs(cents) <= toleranceCents -> "✓ intonata"
                        cents > 0 -> "più alta di $r cent"
                        else -> "più bassa di ${-r} cent"
                    }
                    Text(
                        description + (result.spreadCents?.let { "  (oscillazione ±${it.roundToInt()})" } ?: ""),
                        color = PitchColors.forCents(cents, toleranceCents),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                LinearProgressIndicator(
                    progress = { result.inTuneFraction.toFloat() },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = PitchColors.InTune,
                )
            }
            Text("${(result.inTuneFraction * 100).roundToInt()}%", Modifier.padding(start = 12.dp))
        }
    }
}
