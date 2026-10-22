package com.gowtham.pocketaudiobook.book

import org.json.JSONArray
import org.json.JSONObject

enum class GenerationStatus {
    NOT_STARTED,
    EXTRACTING,
    GENERATING,
    READY,
    FAILED,
    CANCELLED
}

data class BookMetadata(
    val id: String,
    val title: String,
    val sourceUri: String?,
    val status: GenerationStatus,
    val totalChunks: Int,
    val completedChunks: Int,
    val lastSegmentIndex: Int,
    val lastPositionMs: Long,
    val lastOpenedTimestamp: Long,
    val errorMessage: String? = null,
    val voiceName: String? = null,
    val voiceLocaleTag: String? = null,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("sourceUri", sourceUri ?: JSONObject.NULL)
            put("status", status.name)
            put("totalChunks", totalChunks)
            put("completedChunks", completedChunks)
            put("lastSegmentIndex", lastSegmentIndex)
            put("lastPositionMs", lastPositionMs)
            put("lastOpenedTimestamp", lastOpenedTimestamp)
            put("errorMessage", errorMessage ?: JSONObject.NULL)
            put("voiceName", voiceName ?: JSONObject.NULL)
            put("voiceLocaleTag", voiceLocaleTag ?: JSONObject.NULL)
            put("speechRate", speechRate.toDouble())
            put("pitch", pitch.toDouble())
        }
    }

    companion object {
        fun fromJson(json: JSONObject): BookMetadata {
            val statusStr = json.optString("status", GenerationStatus.NOT_STARTED.name)
            val status = try {
                GenerationStatus.valueOf(statusStr)
            } catch (_: Exception) {
                GenerationStatus.NOT_STARTED
            }

            return BookMetadata(
                id = json.getString("id"),
                title = json.getString("title"),
                sourceUri = if (json.isNull("sourceUri")) null else json.getString("sourceUri"),
                status = status,
                totalChunks = json.optInt("totalChunks", 0),
                completedChunks = json.optInt("completedChunks", 0),
                lastSegmentIndex = json.optInt("lastSegmentIndex", 0),
                lastPositionMs = json.optLong("lastPositionMs", 0L),
                lastOpenedTimestamp = json.optLong("lastOpenedTimestamp", System.currentTimeMillis()),
                errorMessage = if (json.isNull("errorMessage")) null else json.getString("errorMessage"),
                voiceName = if (json.isNull("voiceName")) null else json.getString("voiceName"),
                voiceLocaleTag = if (json.isNull("voiceLocaleTag")) null else json.getString("voiceLocaleTag"),
                speechRate = json.optDouble("speechRate", 1.0).toFloat(),
                pitch = json.optDouble("pitch", 1.0).toFloat()
            )
        }

        fun listFromJson(jsonStr: String): List<BookMetadata> {
            val list = mutableListOf<BookMetadata>()
            if (jsonStr.isBlank()) return list
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
            } catch (_: Exception) {}
            return list
        }

        fun listToJson(list: List<BookMetadata>): String {
            val array = JSONArray()
            for (item in list) {
                array.put(item.toJson())
            }
            return array.toString()
        }
    }
}
