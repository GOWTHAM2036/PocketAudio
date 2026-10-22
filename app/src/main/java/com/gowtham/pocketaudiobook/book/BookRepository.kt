package com.gowtham.pocketaudiobook.book

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BookRepository(private val context: Context) {

    private val indexFile = File(context.filesDir, "library_index.json")
    private val booksBaseDir = File(context.filesDir, "audiobooks").apply { mkdirs() }

    fun getBookDirectory(bookId: String): File {
        return File(booksBaseDir, bookId).apply { mkdirs() }
    }

    fun getChunkAudioFile(bookId: String, chunkIndex: Int): File {
        return File(getBookDirectory(bookId), "chunk_$chunkIndex.wav")
    }

    fun getGeneratedAudioChunkFiles(bookId: String, totalChunks: Int): List<File> {
        val list = mutableListOf<File>()
        for (i in 0 until totalChunks) {
            val file = getChunkAudioFile(bookId, i)
            if (file.exists() && file.length() > 0) {
                list.add(file)
            } else {
                break
            }
        }
        return list
    }

    suspend fun getLibrary(): List<BookMetadata> = withContext(Dispatchers.IO) {
        if (!indexFile.exists()) return@withContext emptyList()
        try {
            val jsonStr = indexFile.readText()
            BookMetadata.listFromJson(jsonStr).sortedByDescending { it.lastOpenedTimestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveBookMetadata(metadata: BookMetadata) = withContext(Dispatchers.IO) {
        val current = getLibrary().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == metadata.id }

        if (existingIndex != -1) {
            current[existingIndex] = metadata
        } else {
            current.add(0, metadata)
        }

        saveIndexFile(current)
    }

    suspend fun getBookMetadata(bookId: String): BookMetadata? = withContext(Dispatchers.IO) {
        getLibrary().firstOrNull { it.id == bookId }
    }

    suspend fun saveBookText(bookId: String, text: String) = withContext(Dispatchers.IO) {
        try {
            val textFile = File(getBookDirectory(bookId), "text.txt")
            textFile.writeText(text)
        } catch (_: Exception) {}
    }

    suspend fun loadBookText(bookId: String): String? = withContext(Dispatchers.IO) {
        try {
            val textFile = File(getBookDirectory(bookId), "text.txt")
            if (textFile.exists()) textFile.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        val current = getLibrary().filterNot { it.id == bookId }
        saveIndexFile(current)

        try {
            val dir = File(booksBaseDir, bookId)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (_: Exception) {}
    }

    private fun saveIndexFile(list: List<BookMetadata>) {
        try {
            val jsonStr = BookMetadata.listToJson(list)
            indexFile.writeText(jsonStr)
        } catch (_: Exception) {}
    }
}
