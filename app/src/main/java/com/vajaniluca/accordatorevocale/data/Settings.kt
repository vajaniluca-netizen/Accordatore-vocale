package com.vajaniluca.accordatorevocale.data

import android.content.Context
import com.vajaniluca.accordatorevocale.music.Notation

/** Quanto l'indicatore segue in fretta la voce: costante di tempo della media, in secondi. */
enum class Responsiveness(val label: String, val timeConstantSec: Double) {
    STABLE("Stabile", 0.4),
    NORMAL("Normale", 0.2),
    FAST("Rapida", 0.07),
}

/** Margine in dB sopra il rumore di fondo richiesto per considerare un suono come voce. */
enum class NoiseFilter(val label: String, val marginDb: Double) {
    LIGHT("Leggero", 8.0),
    NORMAL("Normale", 14.0),
    STRONG("Forte", 20.0),
}

data class Settings(
    val a4: Int = 440,
    val notation: Notation = Notation.ITALIAN,
    val toleranceCents: Int = 20,
    val leadInBeats: Int = 4,
    val responsiveness: Responsiveness = Responsiveness.NORMAL,
    val noiseFilter: NoiseFilter = NoiseFilter.NORMAL,
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("impostazioni", Context.MODE_PRIVATE)

    fun load(): Settings = Settings(
        a4 = prefs.getInt("a4", 440),
        notation = runCatching { Notation.valueOf(prefs.getString("notation", null) ?: "") }.getOrDefault(Notation.ITALIAN),
        toleranceCents = prefs.getInt("tolerance", 20),
        leadInBeats = prefs.getInt("leadIn", 4),
        responsiveness = runCatching { Responsiveness.valueOf(prefs.getString("responsiveness", null) ?: "") }
            .getOrDefault(Responsiveness.NORMAL),
        noiseFilter = runCatching { NoiseFilter.valueOf(prefs.getString("noiseFilter", null) ?: "") }
            .getOrDefault(NoiseFilter.NORMAL),
    )

    fun save(settings: Settings) {
        prefs.edit()
            .putInt("a4", settings.a4)
            .putString("notation", settings.notation.name)
            .putInt("tolerance", settings.toleranceCents)
            .putInt("leadIn", settings.leadInBeats)
            .putString("responsiveness", settings.responsiveness.name)
            .putString("noiseFilter", settings.noiseFilter.name)
            .apply()
    }
}
