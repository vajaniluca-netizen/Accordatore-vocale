package com.vajaniluca.accordatorevocale.data

import android.content.Context
import com.vajaniluca.accordatorevocale.music.Notation

data class Settings(
    val a4: Int = 440,
    val notation: Notation = Notation.ITALIAN,
    val toleranceCents: Int = 20,
    val leadInBeats: Int = 4,
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("impostazioni", Context.MODE_PRIVATE)

    fun load(): Settings = Settings(
        a4 = prefs.getInt("a4", 440),
        notation = runCatching { Notation.valueOf(prefs.getString("notation", null) ?: "") }.getOrDefault(Notation.ITALIAN),
        toleranceCents = prefs.getInt("tolerance", 20),
        leadInBeats = prefs.getInt("leadIn", 4),
    )

    fun save(settings: Settings) {
        prefs.edit()
            .putInt("a4", settings.a4)
            .putString("notation", settings.notation.name)
            .putInt("tolerance", settings.toleranceCents)
            .putInt("leadIn", settings.leadInBeats)
            .apply()
    }
}
