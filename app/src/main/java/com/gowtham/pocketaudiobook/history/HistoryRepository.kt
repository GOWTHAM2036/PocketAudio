package com.gowtham.pocketaudiobook.history

import android.content.Context
import android.net.Uri
import com.gowtham.pocketaudiobook.pdf.PdfExtractionResult
import com.gowtham.pocketaudiobook.pdf.PdfExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class HistoryRepository(private val context: Context) {

    private val indexFile = File(context.filesDir, "history_index.json")
    private val booksDir = File(context.filesDir, "cached_books").apply { mkdirs() }

    suspend fun getHistory(): List<HistoryItem> = withContext(Dispatchers.IO) {
        if (!indexFile.exists()) return@withContext emptyList()
        try {
            val jsonStr = indexFile.readText()
            HistoryItem.listFromJson(jsonStr).sortedByDescending { it.lastOpenedTimestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveOrUpdateHistory(
        title: String,
        text: String,
        sourceUri: Uri?,
        lastChunkIndex: Int,
        totalChunks: Int
    ): HistoryItem = withContext(Dispatchers.IO) {
        val currentHistory = getHistory().toMutableList()
        val uriString = sourceUri?.toString()

        // Check if book already exists by sourceUri or title
        val existingIndex = currentHistory.indexOfFirst {
            (uriString != null && it.sourceUri == uriString) || (it.title.equals(title, ignoreCase = true))
        }

        val item: HistoryItem
        if (existingIndex != -1) {
            val existing = currentHistory[existingIndex]
            item = existing.copy(
                title = title,
                lastChunkIndex = lastChunkIndex,
                totalChunks = totalChunks,
                lastOpenedTimestamp = System.currentTimeMillis()
            )
            currentHistory[existingIndex] = item
        } else {
            val newId = UUID.randomUUID().toString()
            item = HistoryItem(
                id = newId,
                title = title.ifBlank { "Untitled Document" },
                sourceUri = uriString,
                lastChunkIndex = lastChunkIndex,
                totalChunks = totalChunks,
                lastOpenedTimestamp = System.currentTimeMillis()
            )
            currentHistory.add(0, item)
        }

        // Write text content to cached book file
        try {
            val bookFile = File(booksDir, "${item.id}.txt")
            bookFile.writeText(text)
        } catch (_: Exception) {}

        // Save updated index file
        saveIndexFile(currentHistory)
        item
    }

    suspend fun loadBookContent(item: HistoryItem): BookLoadResult = withContext(Dispatchers.IO) {
        var textContent: String? = null
        var isFromCache = false

        // 1. Try reading from original PDF if URI is available
        if (!item.sourceUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(item.sourceUri)
                when (val result = PdfExtractor.extractTextFromUri(context, uri)) {
                    is PdfExtractionResult.Success -> {
                        textContent = result.text
                    }
                    is PdfExtractionResult.Error -> {
                        // PDF failed or permission revoked, fallback to cached text
                    }
                }
            } catch (_: Exception) {
                // Ignore and try cache
            }
        }

        // 2. If PDF was not available, try reading cached text file
        if (textContent.isNullOrBlank()) {
            val cacheFile = File(booksDir, "${item.id}.txt")
            if (cacheFile.exists()) {
                try {
                    textContent = cacheFile.readText()
                    isFromCache = true
                } catch (_: Exception) {}
            }
        }

        if (!textContent.isNullOrBlank()) {
            BookLoadResult.Success(
                title = item.title,
                text = textContent,
                lastChunkIndex = item.lastChunkIndex,
                totalChunks = item.totalChunks,
                isFromCache = isFromCache
            )
        } else {
            BookLoadResult.Error("Could not access original document or cached copy.")
        }
    }

    suspend fun deleteHistoryItem(id: String) = withContext(Dispatchers.IO) {
        val current = getHistory().filterNot { it.id == id }
        saveIndexFile(current)
        try {
            val file = File(booksDir, "$id.txt")
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    private fun saveIndexFile(list: List<HistoryItem>) {
        try {
            // Keep at most top 30 history entries
            val trimmedList = list.take(30)
            val jsonStr = HistoryItem.listToJson(trimmedList)
            indexFile.writeText(jsonStr)
        } catch (_: Exception) {}
    }
}
