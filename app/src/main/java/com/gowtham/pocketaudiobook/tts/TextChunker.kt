package com.gowtham.pocketaudiobook.tts

object TextChunker {
    private const val MAX_CHUNK_LENGTH = 1000

    fun chunkText(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        if (trimmed.length <= MAX_CHUNK_LENGTH) {
            return listOf(trimmed)
        }

        val chunks = mutableListOf<String>()
        val paragraphs = trimmed.split(Regex("(\r?\n)+"))
        var currentChunk = StringBuilder()

        for (paragraph in paragraphs) {
            val p = paragraph.trim()
            if (p.isEmpty()) continue

            if (p.length > MAX_CHUNK_LENGTH) {
                // Split by sentence boundaries (. ! ? followed by space)
                val sentences = p.split(Regex("(?<=[.!?])\\s+"))
                for (sentence in sentences) {
                    val s = sentence.trim()
                    if (s.isEmpty()) continue

                    if (s.length > MAX_CHUNK_LENGTH) {
                        // Split by word boundaries if single sentence is > MAX_CHUNK_LENGTH
                        val words = s.split(Regex("\\s+"))
                        for (word in words) {
                            if (currentChunk.length + word.length + 1 > MAX_CHUNK_LENGTH) {
                                if (currentChunk.isNotEmpty()) {
                                    chunks.add(currentChunk.toString().trim())
                                    currentChunk = StringBuilder()
                                }
                            }
                            if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                            currentChunk.append(word)
                        }
                    } else {
                        if (currentChunk.length + s.length + 1 > MAX_CHUNK_LENGTH) {
                            if (currentChunk.isNotEmpty()) {
                                chunks.add(currentChunk.toString().trim())
                                currentChunk = StringBuilder()
                            }
                        }
                        if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                        currentChunk.append(s)
                    }
                }
            } else {
                if (currentChunk.length + p.length + 2 > MAX_CHUNK_LENGTH) {
                    if (currentChunk.isNotEmpty()) {
                        chunks.add(currentChunk.toString().trim())
                        currentChunk = StringBuilder()
                    }
                }
                if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
                currentChunk.append(p)
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks.filter { it.isNotBlank() }
    }
}
