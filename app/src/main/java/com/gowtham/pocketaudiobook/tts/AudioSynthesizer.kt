package com.gowtham.pocketaudiobook.tts

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.gowtham.pocketaudiobook.book.BookRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID

sealed class SynthesisResult {
    data class Success(val bookId: String, val totalChunks: Int) : SynthesisResult()
    data class Cancelled(val bookId: String) : SynthesisResult()
    data class Error(val bookId: String, val message: String) : SynthesisResult()
}

class AudioSynthesizer(
    private val context: Context,
    private val bookRepository: BookRepository
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    @Volatile
    private var isCancelled = false

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        private const val CHUNK_SYNTHESIS_TIMEOUT_MS = 30000L
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized && tts != null) return@withContext true

        val deferred = CompletableDeferred<Boolean>()

        mainHandler.post {
            tryInitEngine(GOOGLE_TTS_PACKAGE) { success ->
                if (success) {
                    deferred.complete(true)
                } else {
                    tryInitEngine(null) { defaultSuccess ->
                        deferred.complete(defaultSuccess)
                    }
                }
            }
        }

        deferred.await()
    }

    private fun tryInitEngine(enginePackage: String?, callback: (Boolean) -> Unit) {
        var ttsInstance: TextToSpeech? = null
        var isCallbackInvoked = false

        fun postResult(success: Boolean) {
            if (!isCallbackInvoked) {
                isCallbackInvoked = true
                callback(success)
            }
        }

        val initListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val activeTts = ttsInstance
                if (activeTts == null) {
                    postResult(false)
                    return@OnInitListener
                }

                var langResult = activeTts.setLanguage(Locale.US)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    langResult = activeTts.setLanguage(Locale.getDefault())
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        try { activeTts.shutdown() } catch (_: Exception) {}
                        postResult(false)
                        return@OnInitListener
                    }
                }

                tts = activeTts
                isInitialized = true
                postResult(true)
            } else {
                try { ttsInstance?.shutdown() } catch (_: Exception) {}
                postResult(false)
            }
        }

        try {
            ttsInstance = if (isEngineAvailable(context, enginePackage)) {
                TextToSpeech(context.applicationContext, initListener, enginePackage)
            } else {
                TextToSpeech(context.applicationContext, initListener)
            }
        } catch (_: Exception) {
            postResult(false)
        }
    }

    private fun isEngineAvailable(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun synthesizeBook(
        bookId: String,
        text: String,
        voiceName: String? = null,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        onProgress: (completed: Int, total: Int, message: String) -> Unit
    ): SynthesisResult = withContext(Dispatchers.IO) {
        isCancelled = false

        if (!initialize()) {
            return@withContext SynthesisResult.Error(bookId, "Failed to initialize Text-to-Speech engine.")
        }

        val activeTts = tts ?: return@withContext SynthesisResult.Error(bookId, "TTS engine unavailable.")

        // Configure speech rate and pitch
        activeTts.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        activeTts.setPitch(pitch.coerceIn(0.5f, 1.5f))

        // Configure requested narrator voice
        if (!voiceName.isNullOrBlank()) {
            val matchingVoice = try {
                activeTts.voices?.firstOrNull { it.name == voiceName }
            } catch (_: Exception) {
                null
            }

            if (matchingVoice != null) {
                activeTts.voice = matchingVoice
            } else {
                return@withContext SynthesisResult.Error(
                    bookId,
                    "Selected voice '$voiceName' is not available on this device. Please select an available voice."
                )
            }
        }

        // Check if existing audio chunks were generated with different voice/rate/pitch settings
        val metadata = bookRepository.getBookMetadata(bookId)
        if (metadata != null) {
            val voiceChanged = metadata.voiceName != voiceName
            val rateChanged = kotlin.math.abs(metadata.speechRate - speechRate) > 0.05f
            val pitchChanged = kotlin.math.abs(metadata.pitch - pitch) > 0.05f

            if (voiceChanged || rateChanged || pitchChanged) {
                val oldFiles = bookRepository.getGeneratedAudioChunkFiles(bookId, metadata.totalChunks)
                for (file in oldFiles) {
                    try { file.delete() } catch (_: Exception) {}
                }
            }
        }

        val chunks = TextChunker.chunkText(text)
        if (chunks.isEmpty()) {
            return@withContext SynthesisResult.Error(bookId, "No speakable text found in book.")
        }

        val totalChunks = chunks.size
        val sessionId = UUID.randomUUID().toString()

        var pendingDeferred: CompletableDeferred<Boolean>? = null
        var pendingUtteranceId: String? = null

        activeTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == pendingUtteranceId) {
                    pendingDeferred?.complete(true)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId == pendingUtteranceId) {
                    pendingDeferred?.complete(false)
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId == pendingUtteranceId) {
                    pendingDeferred?.complete(false)
                }
            }
        })

        for (i in 0 until totalChunks) {
            if (isCancelled || !isActive) {
                activeTts.stop()
                return@withContext SynthesisResult.Cancelled(bookId)
            }

            val targetFile = bookRepository.getChunkAudioFile(bookId, i)

            // Resume logic: if valid non-empty audio chunk file already exists, skip synthesis
            if (targetFile.exists() && targetFile.length() > 1024) {
                onProgress(i + 1, totalChunks, "Reusing chunk ${i + 1} of $totalChunks")
                continue
            }

            val utteranceId = "${sessionId}_$i"
            pendingUtteranceId = utteranceId
            val chunkDeferred = CompletableDeferred<Boolean>()
            pendingDeferred = chunkDeferred

            onProgress(i, totalChunks, "Synthesizing chunk ${i + 1} of $totalChunks...")

            val params = Bundle()
            val result = activeTts.synthesizeToFile(chunks[i], params, targetFile, utteranceId)

            if (result == TextToSpeech.ERROR) {
                if (targetFile.exists()) targetFile.delete()
                return@withContext SynthesisResult.Error(bookId, "TTS failed to synthesize chunk ${i + 1}.")
            }

            val success = withTimeoutOrNull(CHUNK_SYNTHESIS_TIMEOUT_MS) {
                chunkDeferred.await()
            } ?: false

            if (!success || isCancelled || !isActive) {
                if (targetFile.exists()) targetFile.delete()
                if (isCancelled || !isActive) {
                    activeTts.stop()
                    return@withContext SynthesisResult.Cancelled(bookId)
                } else {
                    return@withContext SynthesisResult.Error(bookId, "Timeout or error synthesizing chunk ${i + 1}.")
                }
            }

            onProgress(i + 1, totalChunks, "Completed chunk ${i + 1} of $totalChunks")
        }

        SynthesisResult.Success(bookId, totalChunks)
    }

    fun cancel() {
        isCancelled = true
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun shutdown() {
        isCancelled = true
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
