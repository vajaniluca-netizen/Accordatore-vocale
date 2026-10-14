package com.vajaniluca.accordatorevocale.data

import android.content.Context
import com.vajaniluca.accordatorevocale.exercise.Exercise
import com.vajaniluca.accordatorevocale.exercise.ExerciseNote
import com.vajaniluca.accordatorevocale.exercise.Presets
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Salva gli esercizi in un file JSON nella memoria privata dell'app. */
class ExerciseRepository(context: Context) {
    private val file = File(context.filesDir, "esercizi.json")

    fun load(): List<Exercise> {
        if (!file.exists()) return Presets.all().also(::save)
        return try {
            parse(file.readText())
        } catch (e: Exception) {
            Presets.all()
        }
    }

    fun save(exercises: List<Exercise>) {
        val array = JSONArray()
        for (e in exercises) {
            val notes = JSONArray()
            for (n in e.notes) {
                notes.put(JSONObject().put("m", n.midi ?: JSONObject.NULL).put("b", n.beats))
            }
            array.put(JSONObject().put("id", e.id).put("name", e.name).put("bpm", e.bpm).put("notes", notes))
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(array.toString())
        tmp.renameTo(file)
    }

    private fun parse(text: String): List<Exercise> {
        val array = JSONArray(text)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val notes = o.getJSONArray("notes")
            Exercise(
                id = o.getString("id"),
                name = o.getString("name"),
                bpm = o.getInt("bpm"),
                notes = (0 until notes.length()).map { j ->
                    val n = notes.getJSONObject(j)
                    ExerciseNote(if (n.isNull("m")) null else n.getInt("m"), n.getDouble("b"))
                },
            )
        }
    }
}
