package com.vajaniluca.accordatorevocale.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vajaniluca.accordatorevocale.AppViewModel
import com.vajaniluca.accordatorevocale.exercise.Exercise
import com.vajaniluca.accordatorevocale.exercise.ExerciseNote
import com.vajaniluca.accordatorevocale.music.Notation
import com.vajaniluca.accordatorevocale.music.Notes
import com.vajaniluca.accordatorevocale.ui.components.SimpleTopBar
import java.util.UUID

private val DURATIONS = listOf(0.5, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0)

@Composable
fun ExerciseEditorScreen(
    vm: AppViewModel,
    exerciseId: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val original = remember(exerciseId) { exerciseId?.let { id -> vm.exercises.value.find { it.id == id } } }
    var name by remember { mutableStateOf(original?.name ?: "Nuovo esercizio") }
    var bpm by remember { mutableIntStateOf(original?.bpm ?: 80) }
    val notes = remember {
        mutableStateListOf<ExerciseNote>().apply {
            addAll(original?.notes ?: listOf(ExerciseNote(60, 1.0), ExerciseNote(62, 1.0), ExerciseNote(64, 1.0)))
        }
    }
    val canSave = name.isNotBlank() && notes.any { it.midi != null }

    Column(modifier.fillMaxSize()) {
        SimpleTopBar(
            title = if (original == null) "Nuovo esercizio" else "Modifica esercizio",
            onBack = onDone,
            actions = {
                TextButton(
                    enabled = canSave,
                    onClick = {
                        vm.saveExercise(
                            Exercise(
                                id = original?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                bpm = bpm,
                                notes = notes.toList(),
                            ),
                        )
                        onDone()
                    },
                ) { Text("Salva") }
            },
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tempo", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    FilledTonalIconButton(onClick = { bpm = (bpm - 4).coerceAtLeast(30) }) { Text("−") }
                    OutlinedTextField(
                        value = bpm.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { bpm = it.coerceIn(30, 240) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("BPM") },
                        modifier = Modifier.width(120.dp).padding(horizontal = 8.dp),
                    )
                    FilledTonalIconButton(onClick = { bpm = (bpm + 4).coerceAtMost(240) }) { Text("+") }
                }
                Text(
                    "Durate in movimenti: a $bpm BPM un movimento dura ${"%.2f".format(60.0 / bpm)} s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
            itemsIndexed(notes) { index, note ->
                NoteRow(
                    index = index,
                    note = note,
                    notation = settings.notation,
                    onChange = { notes[index] = it },
                    onDuplicate = { notes.add(index + 1, note) },
                    onDelete = { notes.removeAt(index) },
                    onMoveUp = if (index > 0) ({ notes.add(index - 1, notes.removeAt(index)) }) else null,
                )
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val last = notes.lastOrNull { it.midi != null }
                            notes.add(ExerciseNote(last?.midi ?: 60, notes.lastOrNull()?.beats ?: 1.0))
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("+ Nota") }
                    OutlinedButton(onClick = { notes.add(ExerciseNote(null, 1.0)) }, modifier = Modifier.weight(1f)) {
                        Text("+ Pausa")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteRow(
    index: Int,
    note: ExerciseNote,
    notation: Notation,
    onChange: (ExerciseNote) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Ricorda l'ultima nota, così trasformare una pausa di nuovo in nota non la perde.
    var lastMidi by remember { mutableIntStateOf(note.midi ?: 60) }
    LaunchedEffect(note.midi) { note.midi?.let { lastMidi = it } }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("${index + 1}", Modifier.width(28.dp), style = MaterialTheme.typography.labelMedium)
        IconButton(enabled = note.midi != null, onClick = { note.midi?.let { onChange(note.copy(midi = it - 1)) } }) { Text("−") }
        Text(
            note.midi?.let { Notes.name(it, notation) } ?: "Pausa",
            Modifier.width(64.dp),
            textAlign = TextAlign.Center,
            fontWeight = if (note.midi != null) FontWeight.Bold else FontWeight.Normal,
            color = if (note.midi != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(enabled = note.midi != null, onClick = { note.midi?.let { onChange(note.copy(midi = it + 1)) } }) { Text("+") }
        AssistChip(
            onClick = {
                val i = DURATIONS.indexOf(note.beats)
                onChange(note.copy(beats = DURATIONS[(i + 1) % DURATIONS.size]))
            },
            label = { Text("${formatBeats(note.beats)} mov.") },
        )
        Box(Modifier.weight(1f))
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Altre azioni") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (note.midi == null) "Trasforma in nota" else "Trasforma in pausa") },
                    onClick = {
                        onChange(note.copy(midi = if (note.midi == null) lastMidi else null))
                        menuOpen = false
                    },
                )
                if (note.midi != null) {
                    DropdownMenuItem(text = { Text("Ottava sopra") }, onClick = {
                        onChange(note.copy(midi = note.midi + 12))
                        menuOpen = false
                    })
                    DropdownMenuItem(text = { Text("Ottava sotto") }, onClick = {
                        onChange(note.copy(midi = note.midi - 12))
                        menuOpen = false
                    })
                }
                if (onMoveUp != null) {
                    DropdownMenuItem(text = { Text("Sposta su") }, onClick = {
                        onMoveUp()
                        menuOpen = false
                    })
                }
                DropdownMenuItem(text = { Text("Duplica") }, onClick = {
                    onDuplicate()
                    menuOpen = false
                })
                DropdownMenuItem(text = { Text("Elimina") }, onClick = {
                    onDelete()
                    menuOpen = false
                })
            }
        }
    }
}
