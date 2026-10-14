package com.vajaniluca.accordatorevocale

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vajaniluca.accordatorevocale.audio.AudioEngine
import com.vajaniluca.accordatorevocale.audio.NoteHysteresis
import com.vajaniluca.accordatorevocale.audio.PitchFrame
import com.vajaniluca.accordatorevocale.audio.PitchPoint
import com.vajaniluca.accordatorevocale.audio.PitchSmoother
import com.vajaniluca.accordatorevocale.data.ExerciseRepository
import com.vajaniluca.accordatorevocale.data.Settings
import com.vajaniluca.accordatorevocale.data.SettingsRepository
import com.vajaniluca.accordatorevocale.exercise.Exercise
import com.vajaniluca.accordatorevocale.exercise.ExerciseResult
import com.vajaniluca.accordatorevocale.exercise.ExerciseSession
import com.vajaniluca.accordatorevocale.exercise.Presets
import com.vajaniluca.accordatorevocale.exercise.Scoring
import com.vajaniluca.accordatorevocale.music.Notes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val HISTORY_SEC = 20.0
    }

    private val settingsRepository = SettingsRepository(application)
    private val exerciseRepository = ExerciseRepository(application)
    private val engine = AudioEngine()

    private val _settings = MutableStateFlow(settingsRepository.load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _exercises = MutableStateFlow(exerciseRepository.load())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _current = MutableStateFlow<PitchFrame?>(null)
    val current: StateFlow<PitchFrame?> = _current.asStateFlow()

    private val _audioError = MutableStateFlow(false)
    val audioError: StateFlow<Boolean> = _audioError.asStateFlow()

    /** Tracciato degli ultimi secondi, usato dai grafici. */
    val history = mutableStateListOf<PitchPoint>()

    private val _session = MutableStateFlow<ExerciseSession?>(null)
    val session: StateFlow<ExerciseSession?> = _session.asStateFlow()

    private val _lastResult = MutableStateFlow<ExerciseResult?>(null)
    val lastResult: StateFlow<ExerciseResult?> = _lastResult.asStateFlow()

    private val transpositions = mutableStateMapOf<String, Int>()

    // L'indicatore usa una media più lenta, il tracciato una più leggera; il punteggio usa i valori grezzi.
    private val displaySmoother = PitchSmoother(_settings.value.responsiveness.timeConstantSec)
    private val graphSmoother = PitchSmoother(_settings.value.responsiveness.timeConstantSec / 2)
    private val noteHysteresis = NoteHysteresis()

    init {
        engine.a4 = _settings.value.a4.toDouble()
        viewModelScope.launch { engine.frames.collect { onFrame(it) } }
    }

    fun startAudio() {
        history.clear()
        displaySmoother.reset()
        graphSmoother.reset()
        noteHysteresis.reset()
        _current.value = null
        _session.value = null
        _audioError.value = !engine.start()
    }

    fun stopAudio() {
        engine.stop()
        _session.value = null
    }

    override fun onCleared() {
        engine.stop()
    }

    private fun onFrame(frame: PitchFrame) {
        if (frame.generation != engine.generation) return
        val shown = displaySmoother.process(frame.timeSec, frame.midi)
        _current.value = frame.copy(
            midi = shown,
            frequency = shown?.let { Notes.midiToFreq(it, _settings.value.a4.toDouble()) },
            note = if (shown == null) null else noteHysteresis.process(shown),
        )
        history.add(PitchPoint(frame.timeSec, graphSmoother.process(frame.timeSec, frame.midi)))
        val cutoff = frame.timeSec - HISTORY_SEC
        var drop = 0
        while (drop < history.size && history[drop].timeSec < cutoff) drop++
        if (drop > 0) history.removeRange(0, drop)

        val s = _session.value ?: return
        if (s.result != null) return
        val relative = frame.timeSec - s.startSec
        if (relative >= 0) s.samples.add(PitchPoint(relative, frame.midi))
        if (relative > s.exercise.durationSec + 0.2) {
            val result = Scoring.score(s.exercise, s.samples, _settings.value.toleranceCents)
            _lastResult.value = result
            _session.value = s.copy(result = result)
        }
    }

    // Esercizi in esecuzione

    fun transposition(exerciseId: String): Int = transpositions[exerciseId] ?: 0

    fun setTransposition(exerciseId: String, semitones: Int) {
        transpositions[exerciseId] = semitones.coerceIn(-24, 24)
    }

    fun startExercise(exercise: Exercise) {
        val transposed = exercise.transposed(transposition(exercise.id))
        val leadIn = _settings.value.leadInBeats * transposed.secondsPerBeat
        _session.value = ExerciseSession(transposed, engine.currentTimeSec() + leadIn + 0.3)
    }

    fun cancelExercise() {
        _session.value = null
    }

    // Gestione esercizi

    fun saveExercise(exercise: Exercise) {
        val list = _exercises.value
        val updated = if (list.any { it.id == exercise.id }) list.map { if (it.id == exercise.id) exercise else it } else list + exercise
        _exercises.value = updated
        exerciseRepository.save(updated)
    }

    fun deleteExercise(id: String) {
        val updated = _exercises.value.filterNot { it.id == id }
        _exercises.value = updated
        exerciseRepository.save(updated)
    }

    fun restorePresets() {
        val presets = Presets.all()
        val ids = presets.map { it.id }.toSet()
        val updated = presets + _exercises.value.filterNot { it.id in ids }
        _exercises.value = updated
        exerciseRepository.save(updated)
    }

    // Impostazioni

    fun updateSettings(transform: (Settings) -> Settings) {
        val s = transform(_settings.value)
        _settings.value = s
        settingsRepository.save(s)
        engine.a4 = s.a4.toDouble()
        displaySmoother.timeConstantSec = s.responsiveness.timeConstantSec
        graphSmoother.timeConstantSec = s.responsiveness.timeConstantSec / 2
    }
}
