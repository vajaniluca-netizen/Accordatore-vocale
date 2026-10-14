package com.vajaniluca.accordatorevocale.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorBanner(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Text(message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

fun formatBeats(beats: Double): String =
    if (beats == beats.toLong().toDouble()) beats.toLong().toString() else beats.toString().replace('.', ',')

fun formatDuration(seconds: Double): String {
    val total = seconds.toInt()
    return if (total < 60) "$total s" else "${total / 60} min ${total % 60} s"
}
