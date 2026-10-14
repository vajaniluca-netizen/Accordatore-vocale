package com.vajaniluca.accordatorevocale.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object PitchColors {
    val InTune = Color(0xFF43A047)
    val Near = Color(0xFFFFA000)
    val Off = Color(0xFFE53935)

    fun forCents(cents: Double, toleranceCents: Int): Color = when {
        kotlin.math.abs(cents) <= toleranceCents -> InTune
        kotlin.math.abs(cents) <= toleranceCents * 2 -> Near
        else -> Off
    }
}

@Composable
fun AccordatoreTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
