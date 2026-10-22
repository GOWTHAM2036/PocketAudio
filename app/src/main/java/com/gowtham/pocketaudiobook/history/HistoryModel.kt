package com.gowtham.pocketaudiobook.history

import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val id: String,
    val title: String,
    val sourceUri: String?,
    val lastChunkIndex: Int,
    val totalChunks: Int,
    val lastOpenedTimestamp: Long
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("sourceUri", sourceUri ?: JSONObject.NULL)
            put("lastChunkIndex", lastChunkIndex)
            put("totalChunks", totalChunks)
            put("lastOpenedTimestamp", lastOpenedTimestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): HistoryItem {
            return HistoryItem(
                id = json.getString("id"),
                title = json.getString("title"),
                sourceUri = if (json.isNull("sourceUri")) null else json.getString("sourceUri"),
                lastChunkIndex = json.optInt("lastChunkIndex", 0),
                totalChunks = json.optInt("totalChunks", 1),
                lastOpenedTimestamp = json.optLong("lastOpenedTimestamp", System.currentTimeMillis())
            )
        }

        fun listFromJson(jsonString: String): List<HistoryItem> {
            val list = mutableListOf<HistoryItem>()
            if (jsonString.isBlank()) return list
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(fromJson(obj))
                }
            } catch (_: Exception) {}
            return list
        }

        fun listToJson(list: List<HistoryItem>): String {
            val array = JSONArray()
            for (item in list) {
                array.put(item.toJson())
            }
            return array.toString()
        }
    }
}

sealed class BookLoadResult {
    data class Success(
        val title: String,
        val text: String,
        val lastChunkIndex: Int,
        val totalChunks: Int,
        val isFromCache: Boolean
    ) : BookLoadResult()

    data class Error(val message: String) : BookLoadResult()
}
