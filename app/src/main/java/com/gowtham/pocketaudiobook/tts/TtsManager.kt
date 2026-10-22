package com.gowtham.pocketaudiobook.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

sealed class TtsPlaybackState {
    object Idle : TtsPlaybackState()
    object Initializing : TtsPlaybackState()
    data class Playing(val currentChunk: Int, val totalChunks: Int) : TtsPlaybackState()
    data class Paused(val currentChunk: Int, val totalChunks: Int) : TtsPlaybackState()
    object Stopped : TtsPlaybackState()
    data class Error(val message: String) : TtsPlaybackState()
}

class TtsManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    @Volatile
    private var activeSessionId: String = ""

    @Volatile
    private var isSpeakingRequested: Boolean = false

    private var currentChunks: List<String> = emptyList()

    var activeChunkIndex: Int = 0
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    var onStateChanged: ((TtsPlaybackState) -> Unit)? = null

    companion object {
        private const val TAG = "PocketAudiobook"
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        private const val SILENT_LEAD_IN_MS = 120L

        @Volatile
        var activeInstance: TtsManager? = null

        fun stopCurrentSpeechImmediately() {
            val t0 = SystemClock.elapsedRealtime()
            Log.d(TAG, "[TtsManager] stopCurrentSpeechImmediately invoked at t=$t0 ms")
            activeInstance?.stopPlaybackImmediatelyInternal()
        }
    }

    init {
        activeInstance = this
        audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun initialize(onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (isInitialized && tts != null) {
            onComplete?.invoke(true, null)
            return
        }

        updateState(TtsPlaybackState.Initializing)

        tryInitEngine(GOOGLE_TTS_PACKAGE) { success, _ ->
            if (success) {
                onComplete?.invoke(true, null)
            } else {
                tryInitEngine(null) { defaultSuccess, defaultError ->
                    if (defaultSuccess) {
                        onComplete?.invoke(true, null)
                    } else {
                        val errMsg = defaultError ?: "Failed to initialize Text-to-Speech engine."
                        updateState(TtsPlaybackState.Error(errMsg))
                        onComplete?.invoke(false, errMsg)
                    }
                }
            }
        }
    }

    private fun tryInitEngine(enginePackage: String?, callback: (Boolean, String?) -> Unit) {
        var ttsInstance: TextToSpeech? = null
        var isCallbackInvoked = false

        fun postResult(success: Boolean, error: String?) {
            if (!isCallbackInvoked) {
                isCallbackInvoked = true
                mainHandler.post { callback(success, error) }
            }
        }

        val initListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val activeTts = ttsInstance
                if (activeTts == null) {
                    postResult(false, "TTS engine instance was null.")
                    return@OnInitListener
                }

                var langResult = activeTts.setLanguage(Locale.US)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    langResult = activeTts.setLanguage(Locale.getDefault())
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        try { activeTts.shutdown() } catch (_: Exception) {}
                        postResult(false, "English voice data is missing or not supported on this device.")
                        return@OnInitListener
                    }
                }

                tts = activeTts
                isInitialized = true
                setupUtteranceListener(activeTts)
                Log.d(TAG, "[TtsManager] Engine initialized successfully: ${enginePackage ?: "default"}")
                postResult(true, null)
            } else {
                try { ttsInstance?.shutdown() } catch (_: Exception) {}
                postResult(false, "Failed to initialize engine ${enginePackage ?: "default"}.")
            }
        }

        try {
            ttsInstance = if (isEngineAvailable(context, enginePackage)) {
                TextToSpeech(context.applicationContext, initListener, enginePackage)
            } else {
                TextToSpeech(context.applicationContext, initListener)
            }
        } catch (e: Exception) {
            postResult(false, "Error launching TTS engine: ${e.localizedMessage}")
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

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        Log.d(TAG, "[TtsManager] Audio focus lost -> pausing")
                        pausePlayback()
                    }
                }
                .build()
            audioFocusRequest = focusRequest
            am.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        Log.d(TAG, "[TtsManager] Audio focus lost -> pausing")
                        pausePlayback()
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    private fun setupUtteranceListener(ttsEngine: TextToSpeech) {
        ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == null || utteranceId.endsWith("_silence")) return
                val (sessionId, chunkIdx) = parseUtteranceId(utteranceId) ?: return

                Log.d(TAG, "[TtsManager] onStart t=${SystemClock.elapsedRealtime()} ms utteranceId='$utteranceId' (chunkIdx=$chunkIdx)")

                mainHandler.post {
                    if (sessionId == activeSessionId && isSpeakingRequested) {
                        activeChunkIndex = chunkIdx
                        updateState(TtsPlaybackState.Playing(chunkIdx + 1, currentChunks.size))
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == null || utteranceId.endsWith("_silence")) return
                val (sessionId, chunkIdx) = parseUtteranceId(utteranceId) ?: return

                Log.d(TAG, "[TtsManager] onDone t=${SystemClock.elapsedRealtime()} ms utteranceId='$utteranceId' (chunkIdx=$chunkIdx)")

                // Queue next chunk immediately on TTS background thread to prevent audio track underrun
                if (sessionId == activeSessionId && isSpeakingRequested) {
                    val nextToQueue = chunkIdx + 2
                    if (nextToQueue < currentChunks.size) {
                        val nextUtteranceId = "${sessionId}_$nextToQueue"
                        tts?.speak(currentChunks[nextToQueue], TextToSpeech.QUEUE_ADD, null, nextUtteranceId)
                    }

                    if (chunkIdx == currentChunks.size - 1) {
                        isSpeakingRequested = false
                        activeSessionId = ""
                        abandonAudioFocus()
                        mainHandler.post { updateState(TtsPlaybackState.Stopped) }
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId != null && utteranceId.endsWith("_silence")) return
                handleError(utteranceId, "An error occurred during speech playback.")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId != null && utteranceId.endsWith("_silence")) return
                handleError(utteranceId, "TTS error code: $errorCode")
            }

            private fun handleError(utteranceId: String?, errorMsg: String) {
                if (utteranceId == null) return
                val (sessionId, _) = parseUtteranceId(utteranceId) ?: return

                // Ignore errors from stale / superseded sessions
                if (sessionId.isBlank() || sessionId != activeSessionId) return

                Log.e(TAG, "[TtsManager] onError t=${SystemClock.elapsedRealtime()} ms utteranceId='$utteranceId', msg='$errorMsg'")

                mainHandler.post {
                    if (sessionId == activeSessionId && isSpeakingRequested) {
                        isSpeakingRequested = false
                        activeSessionId = ""
                        abandonAudioFocus()
                        updateState(TtsPlaybackState.Error(errorMsg))
                    }
                }
            }
        })
    }

    fun playText(
        text: String,
        startFromChunkIndex: Int = 0,
        voiceName: String? = null,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        if (text.isBlank()) {
            updateState(TtsPlaybackState.Error("Text area is empty."))
            return
        }

        Log.d(TAG, "[TtsManager] playText requested t=${SystemClock.elapsedRealtime()} ms: startChunk=$startFromChunkIndex")
        isSpeakingRequested = true

        if (!isInitialized || tts == null) {
            initialize { success, error ->
                if (success) {
                    playTextInternal(text, startFromChunkIndex, voiceName, speechRate, pitch)
                } else {
                    isSpeakingRequested = false
                    updateState(TtsPlaybackState.Error(error ?: "TTS initialization failed."))
                }
            }
        } else {
            playTextInternal(text, startFromChunkIndex, voiceName, speechRate, pitch)
        }
    }

    private fun playTextInternal(
        text: String,
        startFromChunkIndex: Int,
        voiceName: String?,
        speechRate: Float,
        pitch: Float
    ) {
        val currentTts = tts ?: return

        stopPlaybackImmediatelyInternal()
        requestAudioFocus()

        isSpeakingRequested = true

        currentTts.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        currentTts.setPitch(pitch.coerceIn(0.5f, 1.5f))

        if (!voiceName.isNullOrBlank()) {
            val matchingVoice = try {
                currentTts.voices?.firstOrNull { it.name == voiceName }
            } catch (_: Exception) {
                null
            }

            if (matchingVoice != null) {
                currentTts.voice = matchingVoice
            }
        }

        val chunks = TextChunker.chunkText(text)
        if (chunks.isEmpty()) {
            isSpeakingRequested = false
            updateState(TtsPlaybackState.Error("No speakable text found."))
            return
        }

        currentChunks = chunks
        val validStartIndex = startFromChunkIndex.coerceIn(0, chunks.size - 1)
        activeChunkIndex = validStartIndex

        val newSessionId = UUID.randomUUID().toString()
        activeSessionId = newSessionId

        Log.d(TAG, "[TtsManager] playTextInternal starting new session t=${SystemClock.elapsedRealtime()} ms: id='$newSessionId', chunk=$validStartIndex of ${chunks.size}")

        // Lead-in silence to un-mute DAC & hardware audio track
        val silenceUtteranceId = "${newSessionId}_silence"
        currentTts.playSilentUtterance(SILENT_LEAD_IN_MS, TextToSpeech.QUEUE_FLUSH, silenceUtteranceId)

        // Queue first chunk with QUEUE_ADD
        val utteranceId0 = "${newSessionId}_$validStartIndex"
        currentTts.speak(chunks[validStartIndex], TextToSpeech.QUEUE_ADD, null, utteranceId0)

        // Queue second chunk with QUEUE_ADD for seamless gapless transition
        if (validStartIndex + 1 < chunks.size) {
            val utteranceId1 = "${newSessionId}_${validStartIndex + 1}"
            currentTts.speak(chunks[validStartIndex + 1], TextToSpeech.QUEUE_ADD, null, utteranceId1)
        }
    }

    fun seekToChunk(
        text: String,
        targetChunkIndex: Int,
        voiceName: String? = null,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        if (text.isBlank()) return
        val chunks = TextChunker.chunkText(text)
        if (chunks.isEmpty()) return

        val validIndex = targetChunkIndex.coerceIn(0, chunks.size - 1)
        val wasSpeaking = isSpeakingRequested

        Log.d(TAG, "[TtsManager] seekToChunk target=$validIndex, wasSpeaking=$wasSpeaking")

        stopPlaybackImmediatelyInternal()

        if (wasSpeaking) {
            playTextInternal(text, validIndex, voiceName, speechRate, pitch)
        } else {
            isSpeakingRequested = false
            currentChunks = chunks
            activeChunkIndex = validIndex
            updateState(TtsPlaybackState.Paused(validIndex + 1, chunks.size))
        }
    }

    fun skipChunks(
        text: String,
        deltaChunks: Int,
        voiceName: String? = null,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        if (text.isBlank()) return
        val chunks = TextChunker.chunkText(text)
        if (chunks.isEmpty()) return
        val target = (activeChunkIndex + deltaChunks).coerceIn(0, chunks.size - 1)
        seekToChunk(text, target, voiceName, speechRate, pitch)
    }

    fun stopPlayback() {
        Log.d(TAG, "[TtsManager] stopPlayback requested t=${SystemClock.elapsedRealtime()} ms")
        stopPlaybackImmediatelyInternal()
        updateState(TtsPlaybackState.Stopped)
    }

    fun stopPlaybackImmediatelyInternal() {
        val t0 = SystemClock.elapsedRealtime()
        Log.d(TAG, "[TtsManager] Synchronous stop starting at t=$t0 ms")
        isSpeakingRequested = false
        activeSessionId = ""
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "[TtsManager] Error calling tts.stop(): ${e.localizedMessage}")
        }
        abandonAudioFocus()
        val t1 = SystemClock.elapsedRealtime()
        Log.d(TAG, "[TtsManager] Synchronous stop completed at t=$t1 ms (elapsed=${t1 - t0} ms)")
    }

    fun pausePlayback() {
        val pausedChunk = activeChunkIndex
        Log.d(TAG, "[TtsManager] pausePlayback requested t=${SystemClock.elapsedRealtime()} ms: pausedChunk=$pausedChunk")
        stopPlaybackImmediatelyInternal()
        updateState(TtsPlaybackState.Paused(pausedChunk + 1, currentChunks.size.coerceAtLeast(1)))
    }

    fun restartPlayback(
        text: String,
        voiceName: String? = null,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f
    ) {
        playText(text, startFromChunkIndex = 0, voiceName, speechRate, pitch)
    }

    fun shutdown() {
        isSpeakingRequested = false
        activeSessionId = ""
        abandonAudioFocus()
        onStateChanged = null
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    private fun updateState(state: TtsPlaybackState) {
        Log.d(TAG, "[TtsManager] State transition t=${SystemClock.elapsedRealtime()} ms -> $state")
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onStateChanged?.invoke(state)
        } else {
            mainHandler.post { onStateChanged?.invoke(state) }
        }
    }

    private fun parseUtteranceId(utteranceId: String): Pair<String, Int>? {
        val lastUnderscore = utteranceId.lastIndexOf('_')
        if (lastUnderscore == -1) return null
        val sessionId = utteranceId.substring(0, lastUnderscore)
        val chunkIdx = utteranceId.substring(lastUnderscore + 1).toIntOrNull() ?: return null
        return Pair(sessionId, chunkIdx)
    }
}
