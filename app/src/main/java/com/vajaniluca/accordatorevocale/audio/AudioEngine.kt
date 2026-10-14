package com.vajaniluca.accordatorevocale.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import com.vajaniluca.accordatorevocale.music.Notes
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Legge il microfono su un thread dedicato e pubblica circa 43 analisi al secondo.
 * Il tempo dei frame si basa sui campioni letti, quindi è preciso e non dipende dal carico del telefono.
 */
class AudioEngine {
    companion object {
        const val SAMPLE_RATE = 44100
        const val FRAME_SIZE = 4096
        const val HOP_SIZE = 1024
        /** Regolarità minima del suono (1 = periodico perfetto): sotto questa soglia non è canto. */
        const val MIN_CONFIDENCE = 0.8
    }

    private val _frames = MutableSharedFlow<PitchFrame>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val frames: SharedFlow<PitchFrame> = _frames.asSharedFlow()

    @Volatile var a4: Double = 440.0
    @Volatile var noiseMarginDb: Double = 14.0
    @Volatile private var samplesRead = 0L
    @Volatile private var running = false
    private var thread: Thread? = null
    var generation = 0
        private set

    fun currentTimeSec(): Double = samplesRead.toDouble() / SAMPLE_RATE

    @Synchronized
    fun start(): Boolean {
        if (running) return true
        val record = createRecord() ?: return false
        generation++
        samplesRead = 0
        running = true
        val gen = generation
        thread = Thread({ loop(record, gen) }, "rilevamento-intonazione").also { it.start() }
        return true
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }

    @SuppressLint("MissingPermission")
    private fun createRecord(): AudioRecord? {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) return null
        val bufferBytes = max(minBuffer, FRAME_SIZE * 2)
        // VOICE_RECOGNITION di norma disattiva controllo automatico del volume e riduzione del rumore.
        for (source in listOf(MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC)) {
            val record = try {
                AudioRecord(source, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferBytes)
            } catch (e: Exception) {
                null
            }
            if (record?.state == AudioRecord.STATE_INITIALIZED) return record
            record?.release()
        }
        return null
    }

    private fun loop(record: AudioRecord, gen: Int) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val detector = YinPitchDetector(SAMPLE_RATE, FRAME_SIZE)
        val tracker = PitchTracker()
        val gate = NoiseGate()
        val hopSec = HOP_SIZE.toDouble() / SAMPLE_RATE
        var pitched = false
        val hop = ShortArray(HOP_SIZE)
        val frame = FloatArray(FRAME_SIZE)
        var filled = 0
        try {
            record.startRecording()
            while (running) {
                var got = 0
                while (got < HOP_SIZE && running) {
                    val n = record.read(hop, got, HOP_SIZE - got)
                    if (n < 0) {
                        running = false
                        break
                    }
                    got += n
                }
                if (got < HOP_SIZE) break

                System.arraycopy(frame, HOP_SIZE, frame, 0, FRAME_SIZE - HOP_SIZE)
                for (i in 0 until HOP_SIZE) frame[FRAME_SIZE - HOP_SIZE + i] = hop[i] / 32768f
                samplesRead += HOP_SIZE
                filled = min(FRAME_SIZE, filled + HOP_SIZE)
                if (filled < FRAME_SIZE) continue

                val level = levelDb(frame)
                gate.marginDb = noiseMarginDb
                val voiced = gate.process(level, hopSec, pitched)
                val result = if (voiced) detector.detect(frame)?.takeIf { it.confidence >= MIN_CONFIDENCE } else null
                val reference = a4
                val midi = tracker.process(result?.let { Notes.freqToMidi(it.frequency, reference) })
                pitched = midi != null
                val time = (samplesRead - FRAME_SIZE / 2).toDouble() / SAMPLE_RATE
                _frames.tryEmit(
                    PitchFrame(gen, time, midi?.let { Notes.midiToFreq(it, reference) }, midi, level, gate.thresholdDb),
                )
            }
        } catch (e: Exception) {
            running = false
        } finally {
            try {
                record.stop()
            } catch (_: Exception) {
            }
            record.release()
        }
    }

    private fun levelDb(buffer: FloatArray): Double {
        var sum = 0.0
        // Solo la parte più recente, così il livello reagisce subito quando smetti di cantare.
        val from = buffer.size - HOP_SIZE * 2
        for (i in from until buffer.size) sum += buffer[i] * buffer[i]
        val rms = sqrt(sum / (buffer.size - from))
        return if (rms <= 0.0) -120.0 else 20 * log10(rms)
    }
}
