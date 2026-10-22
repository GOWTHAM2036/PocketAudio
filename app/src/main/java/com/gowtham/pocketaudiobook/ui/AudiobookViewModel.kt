package com.gowtham.pocketaudiobook.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gowtham.pocketaudiobook.book.BookMetadata
import com.gowtham.pocketaudiobook.book.BookRepository
import com.gowtham.pocketaudiobook.book.GenerationStatus
import com.gowtham.pocketaudiobook.pdf.PdfExtractionResult
import com.gowtham.pocketaudiobook.pdf.PdfExtractor
import com.gowtham.pocketaudiobook.player.AudioPlayerManager
import com.gowtham.pocketaudiobook.player.AudioPlayerState
import com.gowtham.pocketaudiobook.service.AudioPlaybackService
import com.gowtham.pocketaudiobook.tts.AudioSynthesizer
import com.gowtham.pocketaudiobook.tts.SynthesisResult
import com.gowtham.pocketaudiobook.tts.TextChunker
import com.gowtham.pocketaudiobook.tts.TtsManager
import com.gowtham.pocketaudiobook.tts.TtsPlaybackState
import com.gowtham.pocketaudiobook.tts.VoiceManager
import com.gowtham.pocketaudiobook.tts.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class AudiobookViewModel(application: Application) : AndroidViewModel(application) {

    private val bookRepository = BookRepository(application)
    private val ttsManager = TtsManager(application)
    private val audioSynthesizer = AudioSynthesizer(application, bookRepository)
    private val audioPlayerManager = AudioPlayerManager(application)
    private val voiceManager = VoiceManager(application)

    private var synthesisJob: Job? = null

    @Volatile
    private var activeBookSessionToken: String = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(AudiobookUiState())
    val uiState: StateFlow<AudiobookUiState> = _uiState.asStateFlow()

    init {
        loadLibrary()
        loadAvailableVoices()

        AudioPlaybackService.onMediaActionReceived = { action ->
            when (action) {
                AudioPlaybackService.ACTION_PLAY_PAUSE -> {
                    if (_uiState.value.activePlaybackMode == PlaybackMode.SAVED_AUDIO) {
                        toggleSavedAudio()
                    } else {
                        toggleQuickListen()
                    }
                }
                AudioPlaybackService.ACTION_PREVIOUS -> {
                    if (_uiState.value.activePlaybackMode == PlaybackMode.SAVED_AUDIO) {
                        skipSavedAudioPrev()
                    } else {
                        skipQuickListenPrev()
                    }
                }
                AudioPlaybackService.ACTION_NEXT -> {
                    if (_uiState.value.activePlaybackMode == PlaybackMode.SAVED_AUDIO) {
                        skipSavedAudioNext()
                    } else {
                        skipQuickListenNext()
                    }
                }
            }
        }

        // Quick Listen TTS Manager Callbacks
        ttsManager.onStateChanged = { state ->
            val total = calculateTotalChunks(_uiState.value.fullText)
            val chunkIdx = when (state) {
                is TtsPlaybackState.Playing -> state.currentChunk - 1
                is TtsPlaybackState.Paused -> state.currentChunk - 1
                else -> ttsManager.activeChunkIndex
            }

            val isPlaying = state is TtsPlaybackState.Playing

            _uiState.update {
                it.copy(
                    quickListenState = state,
                    currentChunkIndex = chunkIdx,
                    totalChunks = total
                )
            }

            val title = _uiState.value.bookTitle.ifBlank { "PocketAudiobook" }
            val selectedVoice = _uiState.value.availableVoices.find { it.id == _uiState.value.selectedVoiceName }
            val voiceLabel = selectedVoice?.localeDisplayName ?: "Narrator Voice"

            if (isPlaying) {
                val sub = "Quick Listening: Chunk ${chunkIdx + 1}/$total · $voiceLabel"
                AudioPlaybackService.startService(getApplication(), title, sub, true)
            } else if (state is TtsPlaybackState.Paused) {
                val sub = "Quick Listen Paused (Chunk ${chunkIdx + 1}/$total)"
                AudioPlaybackService.startService(getApplication(), title, sub, false)
            } else if (state is TtsPlaybackState.Stopped) {
                AudioPlaybackService.stopService(getApplication())
            }

            saveCurrentBookMetadata()
        }

        // ExoPlayer Saved Audio Manager Callbacks
        audioPlayerManager.onStateChanged = { pState ->
            _uiState.update { it.copy(playerState = pState) }
            savePlaybackPosition(pState)

            val title = _uiState.value.bookTitle.ifBlank { "PocketAudiobook" }
            val segCount = pState.totalSegments.coerceAtLeast(1)
            val currentSeg = pState.currentSegmentIndex + 1

            if (pState.isPlaying) {
                val sub = "Saved Audio: Segment $currentSeg/$segCount"
                AudioPlaybackService.startService(getApplication(), title, sub, true)
            } else if (pState.isReady) {
                val sub = "Saved Audio Paused (Segment $currentSeg/$segCount)"
                AudioPlaybackService.startService(getApplication(), title, sub, false)
            } else if (pState.isEnded) {
                AudioPlaybackService.stopService(getApplication())
            }
        }
    }

    fun stopAllPlayback(exceptMode: PlaybackMode? = null) {
        if (exceptMode != PlaybackMode.QUICK_LISTEN) {
            ttsManager.stopPlayback()
        }
        if (exceptMode != PlaybackMode.SAVED_AUDIO) {
            audioPlayerManager.stop()
        }
        voiceManager.stopPreview()
        _uiState.update { it.copy(isPreviewPlaying = false, previewVoiceName = null, previewMessage = null) }
        AudioPlaybackService.stopService(getApplication())
    }

    fun loadLibrary() {
        viewModelScope.launch {
            val list = bookRepository.getLibrary()
            _uiState.update { it.copy(libraryList = list) }
        }
    }

    fun loadAvailableVoices() {
        viewModelScope.launch {
            val voices = voiceManager.getAvailableVoices()
            _uiState.update { state ->
                val defaultVoice = voices.firstOrNull { !it.isNetworkRequired } ?: voices.firstOrNull()
                val currentSelected = state.selectedVoiceName ?: defaultVoice?.id
                val currentLocale = state.selectedVoiceLocaleTag ?: defaultVoice?.localeTag

                state.copy(
                    availableVoices = voices,
                    selectedVoiceName = currentSelected,
                    selectedVoiceLocaleTag = currentLocale
                )
            }
        }
    }

    fun openVoiceSelectionDialog() {
        stopAllPlayback()
        loadAvailableVoices()
        _uiState.update { it.copy(showVoiceSelectionDialog = true, previewMessage = null) }
    }

    fun closeVoiceSelectionDialog() {
        voiceManager.stopPreview()
        _uiState.update {
            it.copy(
                showVoiceSelectionDialog = false,
                isPreviewPlaying = false,
                previewVoiceName = null,
                previewMessage = null
            )
        }
    }

    fun openBookInfoDialog() {
        _uiState.update { it.copy(showBookInfoDialog = true) }
    }

    fun closeBookInfoDialog() {
        _uiState.update { it.copy(showBookInfoDialog = false) }
    }

    fun onSpeechRateChange(newRate: Float) {
        _uiState.update { it.copy(speechRate = newRate) }
        saveCurrentBookMetadata()
    }

    fun onPitchChange(newPitch: Float) {
        _uiState.update { it.copy(pitch = newPitch) }
        saveCurrentBookMetadata()
    }

    fun resetVoiceSettings() {
        _uiState.update {
            it.copy(
                speechRate = 1.0f,
                pitch = 1.0f
            )
        }
        saveCurrentBookMetadata()
    }

    fun previewVoice(voice: VoiceModel) {
        stopAllPlayback()
        _uiState.update {
            it.copy(
                isPreviewPlaying = true,
                previewVoiceName = voice.id,
                previewMessage = "Playing sample for ${voice.id}..."
            )
        }

        voiceManager.previewVoice(
            voiceId = voice.id,
            speechRate = _uiState.value.speechRate,
            pitch = _uiState.value.pitch
        ) { isPlaying, voiceId, error ->
            _uiState.update {
                it.copy(
                    isPreviewPlaying = isPlaying,
                    previewVoiceName = if (isPlaying) voiceId else null,
                    previewMessage = error ?: if (isPlaying) "Playing sample..." else "Sample playback finished."
                )
            }
        }
    }

    fun stopVoicePreview() {
        voiceManager.stopPreview()
        _uiState.update {
            it.copy(
                isPreviewPlaying = false,
                previewVoiceName = null,
                previewMessage = null
            )
        }
    }

    fun selectVoiceForBook(voice: VoiceModel) {
        val voiceChanged = _uiState.value.selectedVoiceName != voice.id

        _uiState.update {
            it.copy(
                selectedVoiceName = voice.id,
                selectedVoiceLocaleTag = voice.localeTag,
                statusMessage = if (voiceChanged && it.generationStatus == GenerationStatus.READY) {
                    "Narrator changed to ${voice.id}. Tap 'Regenerate Offline Audio' in top menu to update offline files."
                } else {
                    "Selected narrator: ${voice.id}"
                }
            )
        }

        saveCurrentBookMetadata()
    }

    fun selectPlaybackMode(mode: PlaybackMode) {
        stopAllPlayback()
        _uiState.update { it.copy(activePlaybackMode = mode) }
    }

    // Quick Listen Actions
    fun toggleQuickListen() {
        val isPlaying = _uiState.value.quickListenState is TtsPlaybackState.Playing
        if (isPlaying) {
            pauseQuickListen()
        } else {
            playQuickListen()
        }
    }

    fun playQuickListen() {
        stopAllPlayback(exceptMode = PlaybackMode.QUICK_LISTEN)
        _uiState.update { it.copy(activePlaybackMode = PlaybackMode.QUICK_LISTEN) }

        val text = _uiState.value.fullText
        if (text.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Please paste or import text first.") }
            return
        }

        val startChunk = _uiState.value.currentChunkIndex
        val voice = _uiState.value.selectedVoiceName
        val rate = _uiState.value.speechRate
        val p = _uiState.value.pitch

        ttsManager.playText(text, startChunk, voice, rate, p)
    }

    fun pauseQuickListen() {
        ttsManager.pausePlayback()
    }

    fun stopQuickListen() {
        ttsManager.stopPlayback()
    }

    fun restartQuickListen() {
        stopAllPlayback(exceptMode = PlaybackMode.QUICK_LISTEN)
        _uiState.update { it.copy(activePlaybackMode = PlaybackMode.QUICK_LISTEN, currentChunkIndex = 0) }

        val text = _uiState.value.fullText
        val voice = _uiState.value.selectedVoiceName
        val rate = _uiState.value.speechRate
        val p = _uiState.value.pitch

        ttsManager.restartPlayback(text, voice, rate, p)
    }

    fun seekQuickListenChunk(targetChunk: Int) {
        val text = _uiState.value.fullText
        if (text.isBlank()) return
        val voice = _uiState.value.selectedVoiceName
        val rate = _uiState.value.speechRate
        val p = _uiState.value.pitch

        ttsManager.seekToChunk(text, targetChunk, voice, rate, p)
        _uiState.update { it.copy(currentChunkIndex = targetChunk) }
        saveCurrentBookMetadata()
    }

    fun skipQuickListenPrev() {
        val text = _uiState.value.fullText
        if (text.isBlank()) return
        val voice = _uiState.value.selectedVoiceName
        val rate = _uiState.value.speechRate
        val p = _uiState.value.pitch

        ttsManager.skipChunks(text, -1, voice, rate, p)
        _uiState.update { it.copy(currentChunkIndex = ttsManager.activeChunkIndex) }
        saveCurrentBookMetadata()
    }

    fun skipQuickListenNext() {
        val text = _uiState.value.fullText
        if (text.isBlank()) return
        val voice = _uiState.value.selectedVoiceName
        val rate = _uiState.value.speechRate
        val p = _uiState.value.pitch

        ttsManager.skipChunks(text, 1, voice, rate, p)
        _uiState.update { it.copy(currentChunkIndex = ttsManager.activeChunkIndex) }
        saveCurrentBookMetadata()
    }

    // ExoPlayer Saved Audio Actions
    fun toggleSavedAudio() {
        val isPlaying = _uiState.value.playerState.isPlaying
        if (isPlaying) {
            pauseSavedAudio()
        } else {
            playSavedAudio()
        }
    }

    fun playSavedAudio() {
        stopAllPlayback(exceptMode = PlaybackMode.SAVED_AUDIO)
        _uiState.update { it.copy(activePlaybackMode = PlaybackMode.SAVED_AUDIO) }
        audioPlayerManager.play()
    }

    fun pauseSavedAudio() {
        audioPlayerManager.pause()
    }

    fun stopSavedAudio() {
        audioPlayerManager.stop()
    }

    fun restartSavedAudio() {
        audioPlayerManager.seekToSegment(0, 0L)
        audioPlayerManager.play()
    }

    fun seekSavedAudioProgress(fraction: Float) {
        audioPlayerManager.seekToTotalProgress(fraction)
    }

    fun seekSavedAudioByOffset(offsetMs: Long) {
        audioPlayerManager.seekByOffsetMs(offsetMs)
    }

    fun skipSavedAudioPrev() {
        audioPlayerManager.skipToPreviousSegment()
    }

    fun skipSavedAudioNext() {
        audioPlayerManager.skipToNextSegment()
    }

    fun onTextChange(newText: String) {
        activeBookSessionToken = UUID.randomUUID().toString()
        stopAllPlayback()

        viewModelScope.launch(Dispatchers.Default) {
            val cCount = newText.length
            val wCount = if (newText.isBlank()) 0 else newText.trim().split(Regex("\\s+")).size
            val preview = createTextPreview(newText)
            val total = calculateTotalChunks(newText)

            val currentBookId = _uiState.value.bookId.ifBlank { UUID.randomUUID().toString() }

            _uiState.update {
                it.copy(
                    bookId = currentBookId,
                    fullText = newText,
                    textPreview = preview,
                    charCount = cCount,
                    wordCount = wCount,
                    currentChunkIndex = 0,
                    totalChunks = total,
                    generationStatus = GenerationStatus.NOT_STARTED,
                    activePlaybackMode = PlaybackMode.QUICK_LISTEN
                )
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(bookTitle = newTitle) }
        saveCurrentBookMetadata()
    }

    fun importPdf(uri: Uri) {
        activeBookSessionToken = UUID.randomUUID().toString()
        stopAllPlayback()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtractingPdf = true,
                    statusMessage = "Extracting text from PDF..."
                )
            }

            when (val result = PdfExtractor.extractTextFromUri(getApplication(), uri)) {
                is PdfExtractionResult.Success -> {
                    processExtractedText(
                        title = result.title,
                        text = result.text,
                        sourceUri = uri.toString(),
                        pageCount = result.pageCount
                    )
                }
                is PdfExtractionResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isExtractingPdf = false,
                            statusMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private suspend fun processExtractedText(
        title: String,
        text: String,
        sourceUri: String,
        pageCount: Int
    ) = withContext(Dispatchers.Default) {
        val newBookId = UUID.randomUUID().toString()
        val cCount = text.length
        val wCount = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
        val preview = createTextPreview(text)
        val total = calculateTotalChunks(text)

        bookRepository.saveBookText(newBookId, text)

        val metadata = BookMetadata(
            id = newBookId,
            title = title,
            sourceUri = sourceUri,
            status = GenerationStatus.NOT_STARTED,
            totalChunks = total,
            completedChunks = 0,
            lastSegmentIndex = 0,
            lastPositionMs = 0L,
            lastOpenedTimestamp = System.currentTimeMillis(),
            voiceName = _uiState.value.selectedVoiceName,
            voiceLocaleTag = _uiState.value.selectedVoiceLocaleTag,
            speechRate = _uiState.value.speechRate,
            pitch = _uiState.value.pitch
        )
        bookRepository.saveBookMetadata(metadata)

        _uiState.update {
            it.copy(
                bookId = newBookId,
                bookTitle = title,
                fullText = text,
                textPreview = preview,
                charCount = cCount,
                wordCount = wCount,
                sourceUriString = sourceUri,
                isExtractingPdf = false,
                currentChunkIndex = 0,
                totalChunks = total,
                generationStatus = GenerationStatus.NOT_STARTED,
                activePlaybackMode = PlaybackMode.QUICK_LISTEN,
                statusMessage = "Successfully imported \"$title\" ($pageCount pages, $wCount words)."
            )
        }
        loadLibrary()
    }

    fun generateAudiobook() {
        stopAllPlayback()

        val text = _uiState.value.fullText
        val title = _uiState.value.bookTitle.ifBlank { "Untitled Document" }
        var currentId = _uiState.value.bookId
        val voiceName = _uiState.value.selectedVoiceName
        val rate = _uiState.value.speechRate
        val p = _uiState.value.pitch

        if (text.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Please paste or import text before preparing offline audio.") }
            return
        }

        if (currentId.isBlank()) {
            currentId = UUID.randomUUID().toString()
            _uiState.update { it.copy(bookId = currentId) }
        }

        val sessionToken = activeBookSessionToken

        synthesisJob?.cancel()

        synthesisJob = viewModelScope.launch {
            bookRepository.saveBookText(currentId, text)

            _uiState.update {
                it.copy(
                    generationStatus = GenerationStatus.GENERATING,
                    generationProgress = 0f,
                    generationMessage = "Initializing Text-to-Speech engine...",
                    statusMessage = null
                )
            }

            val result = audioSynthesizer.synthesizeBook(
                bookId = currentId,
                text = text,
                voiceName = voiceName,
                speechRate = rate,
                pitch = p
            ) { completed, total, message ->
                if (sessionToken != activeBookSessionToken) return@synthesizeBook
                val progress = if (total > 0) completed.toFloat() / total else 0f
                _uiState.update {
                    it.copy(
                        generationCompletedChunks = completed,
                        generationTotalChunks = total,
                        generationProgress = progress,
                        generationMessage = message
                    )
                }

                viewModelScope.launch {
                    val meta = BookMetadata(
                        id = currentId,
                        title = title,
                        sourceUri = _uiState.value.sourceUriString,
                        status = GenerationStatus.GENERATING,
                        totalChunks = total,
                        completedChunks = completed,
                        lastSegmentIndex = 0,
                        lastPositionMs = 0L,
                        lastOpenedTimestamp = System.currentTimeMillis(),
                        voiceName = voiceName,
                        voiceLocaleTag = _uiState.value.selectedVoiceLocaleTag,
                        speechRate = rate,
                        pitch = p
                    )
                    bookRepository.saveBookMetadata(meta)
                }
            }

            if (sessionToken != activeBookSessionToken) return@launch

            when (result) {
                is SynthesisResult.Success -> {
                    val meta = BookMetadata(
                        id = currentId,
                        title = title,
                        sourceUri = _uiState.value.sourceUriString,
                        status = GenerationStatus.READY,
                        totalChunks = result.totalChunks,
                        completedChunks = result.totalChunks,
                        lastSegmentIndex = 0,
                        lastPositionMs = 0L,
                        lastOpenedTimestamp = System.currentTimeMillis(),
                        voiceName = voiceName,
                        voiceLocaleTag = _uiState.value.selectedVoiceLocaleTag,
                        speechRate = rate,
                        pitch = p
                    )
                    bookRepository.saveBookMetadata(meta)

                    _uiState.update {
                        it.copy(
                            generationStatus = GenerationStatus.READY,
                            generationProgress = 1f,
                            statusMessage = "Offline audio ready! You can now switch to Saved Offline Audio."
                        )
                    }

                    loadLibrary()
                    prepareAudioPlayerForBook(meta)
                }
                is SynthesisResult.Cancelled -> {
                    val currentCompleted = _uiState.value.generationCompletedChunks
                    val currentTotal = _uiState.value.generationTotalChunks
                    val meta = BookMetadata(
                        id = currentId,
                        title = title,
                        sourceUri = _uiState.value.sourceUriString,
                        status = GenerationStatus.CANCELLED,
                        totalChunks = currentTotal,
                        completedChunks = currentTotal,
                        lastSegmentIndex = 0,
                        lastPositionMs = 0L,
                        lastOpenedTimestamp = System.currentTimeMillis(),
                        voiceName = voiceName,
                        voiceLocaleTag = _uiState.value.selectedVoiceLocaleTag,
                        speechRate = rate,
                        pitch = p
                    )
                    bookRepository.saveBookMetadata(meta)

                    _uiState.update {
                        it.copy(
                            generationStatus = GenerationStatus.CANCELLED,
                            statusMessage = "Offline generation cancelled."
                        )
                    }
                    loadLibrary()
                }
                is SynthesisResult.Error -> {
                    val meta = BookMetadata(
                        id = currentId,
                        title = title,
                        sourceUri = _uiState.value.sourceUriString,
                        status = GenerationStatus.FAILED,
                        totalChunks = _uiState.value.generationTotalChunks,
                        completedChunks = _uiState.value.generationCompletedChunks,
                        lastSegmentIndex = 0,
                        lastPositionMs = 0L,
                        lastOpenedTimestamp = System.currentTimeMillis(),
                        errorMessage = result.message,
                        voiceName = voiceName,
                        voiceLocaleTag = _uiState.value.selectedVoiceLocaleTag,
                        speechRate = rate,
                        pitch = p
                    )
                    bookRepository.saveBookMetadata(meta)

                    _uiState.update {
                        it.copy(
                            generationStatus = GenerationStatus.FAILED,
                            statusMessage = "Generation failed: ${result.message}"
                        )
                    }
                    loadLibrary()
                }
            }
        }
    }

    fun cancelGeneration() {
        audioSynthesizer.cancel()
        synthesisJob?.cancel()
    }

    fun prepareAudioPlayerForBook(metadata: BookMetadata, autoPlay: Boolean = false) {
        viewModelScope.launch {
            val audioFiles = bookRepository.getGeneratedAudioChunkFiles(metadata.id, metadata.totalChunks)
            if (audioFiles.isNotEmpty()) {
                audioPlayerManager.loadAudioChunks(
                    files = audioFiles,
                    startSegmentIndex = metadata.lastSegmentIndex,
                    startPositionMs = metadata.lastPositionMs,
                    autoPlay = autoPlay
                )
            } else {
                _uiState.update { it.copy(statusMessage = "Saved audio files not found.") }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        stopAllPlayback()
        _uiState.update { it.copy(activeTab = tabIndex) }
        if (tabIndex == 1) {
            loadLibrary()
        }
    }

    fun openBookFromLibrary(metadata: BookMetadata) {
        activeBookSessionToken = UUID.randomUUID().toString()
        stopAllPlayback()

        viewModelScope.launch {
            val text = bookRepository.loadBookText(metadata.id) ?: ""
            val preview = createTextPreview(text)
            val cCount = text.length
            val wCount = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
            val total = calculateTotalChunks(text)
            val restoredChunk = metadata.lastSegmentIndex.coerceIn(0, (total - 1).coerceAtLeast(0))

            _uiState.update {
                it.copy(
                    activeTab = 0,
                    bookId = metadata.id,
                    bookTitle = metadata.title,
                    fullText = text,
                    textPreview = preview,
                    charCount = cCount,
                    wordCount = wCount,
                    currentChunkIndex = restoredChunk,
                    totalChunks = total,
                    sourceUriString = metadata.sourceUri,
                    generationStatus = metadata.status,
                    generationCompletedChunks = metadata.completedChunks,
                    generationTotalChunks = metadata.totalChunks,
                    selectedVoiceName = metadata.voiceName,
                    selectedVoiceLocaleTag = metadata.voiceLocaleTag,
                    speechRate = metadata.speechRate,
                    pitch = metadata.pitch,
                    activePlaybackMode = if (metadata.status == GenerationStatus.READY) PlaybackMode.SAVED_AUDIO else PlaybackMode.QUICK_LISTEN,
                    statusMessage = "Loaded \"${metadata.title}\" from library."
                )
            }

            if (metadata.status == GenerationStatus.READY) {
                prepareAudioPlayerForBook(metadata, autoPlay = false)
            }
        }
    }

    fun deleteBookFromLibrary(metadata: BookMetadata) {
        viewModelScope.launch {
            if (_uiState.value.bookId == metadata.id) {
                activeBookSessionToken = UUID.randomUUID().toString()
                stopAllPlayback()
                _uiState.update {
                    it.copy(
                        bookId = "",
                        bookTitle = "Untitled Document",
                        fullText = "",
                        textPreview = "",
                        charCount = 0,
                        wordCount = 0,
                        currentChunkIndex = 0,
                        totalChunks = 1,
                        generationStatus = GenerationStatus.NOT_STARTED,
                        activePlaybackMode = PlaybackMode.QUICK_LISTEN,
                        playerState = AudioPlayerState()
                    )
                }
            }
            bookRepository.deleteBook(metadata.id)
            loadLibrary()
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun calculateTotalChunks(text: String): Int {
        val chunks = TextChunker.chunkText(text)
        return if (chunks.isEmpty()) 1 else chunks.size
    }

    private fun createTextPreview(text: String): String {
        if (text.length <= 5000) return text
        return text.substring(0, 5000) + "\n\n[... Truncated for display performance. Full document text (${text.length} characters) will be spoken ...]"
    }

    private fun saveCurrentBookMetadata() {
        val currentId = _uiState.value.bookId
        if (currentId.isBlank()) return

        viewModelScope.launch {
            val existing = bookRepository.getBookMetadata(currentId)
            val currentChunk = _uiState.value.currentChunkIndex
            val total = calculateTotalChunks(_uiState.value.fullText)

            val updated = if (existing != null) {
                existing.copy(
                    title = _uiState.value.bookTitle,
                    voiceName = _uiState.value.selectedVoiceName,
                    voiceLocaleTag = _uiState.value.selectedVoiceLocaleTag,
                    speechRate = _uiState.value.speechRate,
                    pitch = _uiState.value.pitch,
                    lastSegmentIndex = currentChunk,
                    totalChunks = total,
                    lastOpenedTimestamp = System.currentTimeMillis()
                )
            } else {
                BookMetadata(
                    id = currentId,
                    title = _uiState.value.bookTitle.ifBlank { "Untitled Document" },
                    sourceUri = _uiState.value.sourceUriString,
                    status = _uiState.value.generationStatus,
                    totalChunks = total,
                    completedChunks = _uiState.value.generationCompletedChunks,
                    lastSegmentIndex = currentChunk,
                    lastPositionMs = 0L,
                    lastOpenedTimestamp = System.currentTimeMillis(),
                    voiceName = _uiState.value.selectedVoiceName,
                    voiceLocaleTag = _uiState.value.selectedVoiceLocaleTag,
                    speechRate = _uiState.value.speechRate,
                    pitch = _uiState.value.pitch
                )
            }
            bookRepository.saveBookMetadata(updated)
            loadLibrary()
        }
    }

    private fun savePlaybackPosition(pState: AudioPlayerState) {
        val currentId = _uiState.value.bookId
        if (currentId.isBlank() || _uiState.value.generationStatus != GenerationStatus.READY) return

        viewModelScope.launch {
            val existing = bookRepository.getBookMetadata(currentId) ?: return@launch
            val updated = existing.copy(
                lastSegmentIndex = pState.currentSegmentIndex,
                lastPositionMs = pState.segmentPositionMs,
                lastOpenedTimestamp = System.currentTimeMillis()
            )
            bookRepository.saveBookMetadata(updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAllPlayback()
        ttsManager.shutdown()
        voiceManager.shutdown()
        audioSynthesizer.shutdown()
        audioPlayerManager.release()
    }
}
