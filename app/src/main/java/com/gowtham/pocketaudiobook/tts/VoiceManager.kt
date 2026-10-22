package com.gowtham.pocketaudiobook.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class VoiceManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    @Volatile
    private var activePreviewRequestId: String = ""

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        const val DEFAULT_SAMPLE_TEXT = "Hello! This is a sample of the selected narrator voice for your audiobook."
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

    suspend fun getAvailableVoices(): List<VoiceModel> = withContext(Dispatchers.IO) {
        if (!initialize()) return@withContext emptyList()
        val activeTts = tts ?: return@withContext emptyList()

        try {
            val rawVoices = activeTts.voices ?: emptySet()
            rawVoices
                .map { VoiceModel.fromAndroidVoice(it) }
                .filter { it.isInstalled }
                .sortedWith(
                    compareBy<VoiceModel> { it.isNetworkRequired } // Offline voices first
                        .thenBy { it.localeDisplayName }
                        .thenBy { it.displayName }
                )
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun findVoiceById(voiceId: String?): Voice? {
        if (voiceId.isNullOrBlank()) return null
        val activeTts = tts ?: return null
        return try {
            activeTts.voices?.firstOrNull { it.name == voiceId }
        } catch (_: Exception) {
            null
        }
    }

    fun previewVoice(
        voiceId: String,
        sampleText: String = DEFAULT_SAMPLE_TEXT,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        onProgress: (isPlaying: Boolean, voiceId: String?, error: String?) -> Unit
    ) {
        mainHandler.post {
            val activeTts = tts
            if (activeTts == null || !isInitialized) {
                onProgress(false, voiceId, "TTS engine not initialized.")
                return@post
            }

            try {
                stopPreviewInternal()

                val newRequestId = UUID.randomUUID().toString()
                activePreviewRequestId = newRequestId

                val targetVoice = activeTts.voices?.firstOrNull { it.name == voiceId }
                if (targetVoice == null) {
                    onProgress(false, voiceId, "Selected voice '$voiceId' is not available on this device.")
                    return@post
                }

                activeTts.voice = targetVoice
                activeTts.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
                activeTts.setPitch(pitch.coerceIn(0.5f, 1.5f))

                val utteranceId = "${newRequestId}_$voiceId"

                activeTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {
                        if (id == utteranceId) {
                            mainHandler.post {
                                if (newRequestId == activePreviewRequestId) {
                                    onProgress(true, voiceId, null)
                                }
                            }
                        }
                    }

                    override fun onDone(id: String?) {
                        if (id == utteranceId) {
                            mainHandler.post {
                                if (newRequestId == activePreviewRequestId) {
                                    activePreviewRequestId = ""
                                    onProgress(false, voiceId, null)
                                }
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        if (id == utteranceId) {
                            mainHandler.post {
                                if (newRequestId == activePreviewRequestId) {
                                    activePreviewRequestId = ""
                                    onProgress(false, voiceId, "Preview failed.")
                                }
                            }
                        }
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        if (id == utteranceId) {
                            mainHandler.post {
                                if (newRequestId == activePreviewRequestId) {
                                    activePreviewRequestId = ""
                                    onProgress(false, voiceId, "Preview error code: $errorCode")
                                }
                            }
                        }
                    }
                })

                val result = activeTts.speak(sampleText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    activePreviewRequestId = ""
                    onProgress(false, voiceId, "Failed to start voice preview.")
                }
            } catch (e: Exception) {
                activePreviewRequestId = ""
                onProgress(false, voiceId, "Error previewing voice: ${e.localizedMessage}")
            }
        }
    }

    fun stopPreview() {
        mainHandler.post {
            stopPreviewInternal()
        }
    }

    private fun stopPreviewInternal() {
        activePreviewRequestId = ""
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun shutdown() {
        activePreviewRequestId = ""
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
