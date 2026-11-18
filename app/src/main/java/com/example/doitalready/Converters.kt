package com.example.doitalready.data

import androidx.room.TypeConverter
import org.json.JSONArray

// Pengubah List<String> <-> teks JSON, supaya tags & attachmentList bisa disimpan di satu kolom Room tanpa library tambahan
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        val array = JSONArray()
        value.orEmpty().forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optString(i, "")
                    if (item.isNotBlank()) add(item)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}