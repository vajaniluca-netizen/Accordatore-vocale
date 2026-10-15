package com.vajaniluca.accordatorevocale.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vajaniluca.accordatorevocale.AppViewModel
import com.vajaniluca.accordatorevocale.data.NoiseFilter
import com.vajaniluca.accordatorevocale.data.Responsiveness
import com.vajaniluca.accordatorevocale.music.Notation
import com.vajaniluca.accordatorevocale.ui.components.SimpleTopBar
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var restored by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        SimpleTopBar("Impostazioni")
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Nomi delle note", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.notation == Notation.ITALIAN,
                    onClick = { vm.updateSettings { it.copy(notation = Notation.ITALIAN) } },
                    label = { Text("Do Re Mi") },
                )
                FilterChip(
                    selected = settings.notation == Notation.ENGLISH,
                    onClick = { vm.updateSettings { it.copy(notation = Notation.ENGLISH) } },
                    label = { Text("C D E") },
                )
            }
            Hint("Ottave in notazione scientifica: Do4 è il Do centrale, La4 è il La del diapason.")
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text("Reattività dell'indicatore", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in Responsiveness.entries) {
                    FilterChip(
                        selected = settings.responsiveness == r,
                        onClick = { vm.updateSettings { it.copy(responsiveness = r) } },
                        label = { Text(r.label) },
                    )
                }
            }
            Hint("Stabile fa la media su quasi un secondo e mostra solo l'intonazione di fondo; Normale attenua vibrato e tremolii; Rapida segue di più le variazioni. Il punteggio degli esercizi non cambia.")
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text("Filtro rumore di fondo", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (f in NoiseFilter.entries) {
                    FilterChip(
                        selected = settings.noiseFilter == f,
                        onClick = { vm.updateSettings { it.copy(noiseFilter = f) } },
                        label = { Text(f.label) },
                    )
                }
            }
            Hint("L'app misura il rumore dell'ambiente (TV, voci lontane) e ascolta solo i suoni più forti di quel livello. Con Forte devi cantare più vicino o più forte; con Leggero senti anche le voci deboli ma entra più rumore. Nell'accordatore la barra del livello mostra dove si trova la soglia.")
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text("La di riferimento: ${settings.a4} Hz", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = settings.a4.toFloat(),
                onValueChange = { v -> vm.updateSettings { it.copy(a4 = v.roundToInt()) } },
                valueRange = 430f..450f,
                steps = 19,
            )
            Hint("Di solito 440 Hz. Cambialo solo se canti con uno strumento accordato diversamente (per esempio 442).")
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text("Tolleranza: ±${settings.toleranceCents} cent", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = settings.toleranceCents.toFloat(),
                onValueChange = { v -> vm.updateSettings { it.copy(toleranceCents = (v / 5).roundToInt() * 5) } },
                valueRange = 5f..50f,
                steps = 8,
            )
            Hint("Entro questo scostamento la nota conta come intonata. 100 cent = un semitono; 20 è un buon punto di partenza, 10 è esigente.")
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text("Movimenti di attesa prima dell'esercizio: ${settings.leadInBeats}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = settings.leadInBeats.toFloat(),
                onValueChange = { v -> vm.updateSettings { it.copy(leadInBeats = v.roundToInt()) } },
                valueRange = 0f..8f,
                steps = 7,
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            OutlinedButton(onClick = {
                vm.restorePresets()
                restored = true
            }) { Text("Ripristina esercizi di esempio") }
            if (restored) Hint("Esercizi di esempio ripristinati. I tuoi esercizi non sono stati toccati.")

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Consigli", style = MaterialTheme.typography.titleSmall)
            Hint("Canta a 20/40 cm dal telefono, in un ambiente silenzioso, senza coprire il microfono (di solito in basso).")
            Hint("Il tracciato colorato mostra la tua voce: sopra la barra sei crescente, sotto sei calante.")
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
