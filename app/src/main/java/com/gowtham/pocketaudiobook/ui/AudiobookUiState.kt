package com.gowtham.pocketaudiobook.ui

import com.gowtham.pocketaudiobook.book.BookMetadata
import com.gowtham.pocketaudiobook.book.GenerationStatus
import com.gowtham.pocketaudiobook.player.AudioPlayerState
import com.gowtham.pocketaudiobook.tts.TtsPlaybackState
import com.gowtham.pocketaudiobook.tts.VoiceModel

enum class PlaybackMode {
    QUICK_LISTEN,
    SAVED_AUDIO
}

data class AudiobookUiState(
    val bookId: String = "",
    val bookTitle: String = "Untitled Document",
    val textPreview: String = "",
    val fullText: String = "",
    val charCount: Int = 0,
    val wordCount: Int = 0,
    val sourceUriString: String? = null,
    val isExtractingPdf: Boolean = false,
    val statusMessage: String? = null,
    val activePlaybackMode: PlaybackMode = PlaybackMode.QUICK_LISTEN,
    val quickListenState: TtsPlaybackState = TtsPlaybackState.Idle,
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 1,
    val generationStatus: GenerationStatus = GenerationStatus.NOT_STARTED,
    val generationCompletedChunks: Int = 0,
    val generationTotalChunks: Int = 0,
    val generationProgress: Float = 0f,
    val generationMessage: String? = null,
    val playerState: AudioPlayerState = AudioPlayerState(),
    val libraryList: List<BookMetadata> = emptyList(),
    val activeTab: Int = 0, // 0 = Reader, 1 = Library
    val availableVoices: List<VoiceModel> = emptyList(),
    val selectedVoiceName: String? = null,
    val selectedVoiceLocaleTag: String? = null,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val showVoiceSelectionDialog: Boolean = false,
    val showBookInfoDialog: Boolean = false,
    val isPreviewPlaying: Boolean = false,
    val previewVoiceName: String? = null,
    val previewMessage: String? = null
)
