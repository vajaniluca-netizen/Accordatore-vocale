package com.vajaniluca.accordatorevocale.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vajaniluca.accordatorevocale.AppViewModel
import com.vajaniluca.accordatorevocale.exercise.Exercise
import com.vajaniluca.accordatorevocale.music.Notes
import com.vajaniluca.accordatorevocale.ui.components.SimpleTopBar

@Composable
fun ExerciseListScreen(
    vm: AppViewModel,
    onStart: (String) -> Unit,
    onEdit: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Exercise?>(null) }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SimpleTopBar("Esercizi")
            if (exercises.isEmpty()) {
                Text(
                    "Nessun esercizio. Creane uno nuovo oppure ripristina quelli di esempio dalle impostazioni.",
                    Modifier.padding(16.dp),
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                            val sung = exercise.notes.count { it.midi != null }
                            Text(
                                "$sung note · ${exercise.bpm} BPM · ${formatDuration(exercise.durationSec)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                exercise.notes.mapNotNull { it.midi }.take(12).joinToString(" ") { Notes.name(it, settings.notation) },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { onStart(exercise.id) }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Text("Inizia", Modifier.padding(start = 4.dp))
                                }
                                Box(Modifier.weight(1f))
                                IconButton(onClick = { onEdit(exercise.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Modifica") }
                                IconButton(onClick = { toDelete = exercise }) { Icon(Icons.Filled.Delete, contentDescription = "Elimina") }
                            }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { onEdit(null) },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nuovo") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    toDelete?.let { exercise ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminare l'esercizio?") },
            text = { Text("\"${exercise.name}\" verrà eliminato definitivamente.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteExercise(exercise.id)
                    toDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annulla") } },
        )
    }
}
