package com.gowtham.pocketaudiobook.ui

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gowtham.pocketaudiobook.book.GenerationStatus
import com.gowtham.pocketaudiobook.tts.TtsPlaybackState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookScreen() {
    val context = LocalContext.current
    val viewModel: AudiobookViewModel = remember(context) {
        AudiobookViewModel(context.applicationContext as Application)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importPdf(it) }
    }

    if (uiState.showVoiceSelectionDialog) {
        val currentSelectedId = uiState.selectedVoiceName
        VoiceSelectionDialog(
            availableVoices = uiState.availableVoices,
            selectedVoiceId = currentSelectedId,
            previewVoiceId = uiState.previewVoiceName,
            speechRate = uiState.speechRate,
            pitch = uiState.pitch,
            isPreviewPlaying = uiState.isPreviewPlaying,
            previewMessage = uiState.previewMessage,
            onSpeechRateChange = { viewModel.onSpeechRateChange(it) },
            onPitchChange = { viewModel.onPitchChange(it) },
            onResetVoiceSettings = { viewModel.resetVoiceSettings() },
            onPreviewVoice = { viewModel.previewVoice(it) },
            onStopPreview = { viewModel.stopVoicePreview() },
            onSelectVoice = { viewModel.selectVoiceForBook(it) },
            onDismiss = { viewModel.closeVoiceSelectionDialog() }
        )
    }

    if (uiState.showBookInfoDialog) {
        BookInfoDialog(
            uiState = uiState,
            onDismiss = { viewModel.closeBookInfoDialog() }
        )
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.Book,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PocketAudiobook",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = AppIcons.MoreVert,
                                    contentDescription = "More Options"
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                when (uiState.generationStatus) {
                                    GenerationStatus.GENERATING -> {
                                        DropdownMenuItem(
                                            text = { Text("Cancel Generation") },
                                            onClick = {
                                                showMenu = false
                                                viewModel.cancelGeneration()
                                            },
                                            leadingIcon = {
                                                Icon(imageVector = AppIcons.Close, contentDescription = null)
                                            }
                                        )
                                    }
                                    GenerationStatus.READY -> {
                                        DropdownMenuItem(
                                            text = { Text("Regenerate Offline Audio") },
                                            onClick = {
                                                showMenu = false
                                                viewModel.generateAudiobook()
                                            },
                                            leadingIcon = {
                                                Icon(imageVector = AppIcons.Generate, contentDescription = null)
                                            }
                                        )
                                    }
                                    else -> {
                                        DropdownMenuItem(
                                            text = { Text("Prepare for Offline Listening") },
                                            onClick = {
                                                showMenu = false
                                                viewModel.generateAudiobook()
                                            },
                                            enabled = uiState.fullText.isNotBlank(),
                                            leadingIcon = {
                                                Icon(imageVector = AppIcons.Generate, contentDescription = null)
                                            }
                                        )
                                    }
                                }

                                DropdownMenuItem(
                                    text = { Text("Voice Settings") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.openVoiceSelectionDialog()
                                    },
                                    leadingIcon = {
                                        Icon(imageVector = AppIcons.Book, contentDescription = null)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Book Information") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.openBookInfoDialog()
                                    },
                                    leadingIcon = {
                                        Icon(imageVector = AppIcons.Info, contentDescription = null)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )

                PrimaryTabRow(selectedTabIndex = uiState.activeTab) {
                    Tab(
                        selected = uiState.activeTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = { Text("Reader") },
                        icon = { Icon(imageVector = AppIcons.Book, contentDescription = null) }
                    )
                    Tab(
                        selected = uiState.activeTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = { Text("My Library") },
                        icon = { Icon(imageVector = AppIcons.Library, contentDescription = null) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.activeTab == 1) {
                LibraryView(
                    libraryList = uiState.libraryList,
                    onOpenBook = { viewModel.openBookFromLibrary(it) },
                    onDeleteBook = { viewModel.deleteBookFromLibrary(it) }
                )
            } else {
                ReaderView(
                    uiState = uiState,
                    onTitleChange = { viewModel.onTitleChange(it) },
                    onTextChange = { viewModel.onTextChange(it) },
                    onImportPdf = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                    onOpenVoiceDialog = { viewModel.openVoiceSelectionDialog() },
                    onSelectPlaybackMode = { viewModel.selectPlaybackMode(it) },
                    onToggleQuickListen = { viewModel.toggleQuickListen() },
                    onSeekQuickListen = { viewModel.seekQuickListenChunk(it) },
                    onSkipQuickListenPrev = { viewModel.skipQuickListenPrev() },
                    onSkipQuickListenNext = { viewModel.skipQuickListenNext() },
                    onToggleSavedAudio = { viewModel.toggleSavedAudio() },
                    onSeekSavedAudio = { viewModel.seekSavedAudioProgress(it) },
                    onSeekSavedAudioByOffset = { viewModel.seekSavedAudioByOffset(it) },
                    onSkipSavedPrev = { viewModel.skipSavedAudioPrev() },
                    onSkipSavedNext = { viewModel.skipSavedAudioNext() },
                    onCancelGeneration = { viewModel.cancelGeneration() },
                    onClearStatus = { viewModel.clearStatusMessage() }
                )
            }
        }
    }
}

@Composable
private fun ReaderView(
    uiState: AudiobookUiState,
    onTitleChange: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onImportPdf: () -> Unit,
    onOpenVoiceDialog: () -> Unit,
    onSelectPlaybackMode: (PlaybackMode) -> Unit,
    onToggleQuickListen: () -> Unit,
    onSeekQuickListen: (Int) -> Unit,
    onSkipQuickListenPrev: () -> Unit,
    onSkipQuickListenNext: () -> Unit,
    onToggleSavedAudio: () -> Unit,
    onSeekSavedAudio: (Float) -> Unit,
    onSeekSavedAudioByOffset: (Long) -> Unit,
    onSkipSavedPrev: () -> Unit,
    onSkipSavedNext: () -> Unit,
    onCancelGeneration: () -> Unit,
    onClearStatus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section 1: Header Card (Title, Import PDF, Narrator Selector)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "BOOK TITLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.bookTitle,
                        onValueChange = onTitleChange,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onImportPdf,
                        enabled = !uiState.isExtractingPdf && uiState.generationStatus != GenerationStatus.GENERATING,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.FileUpload,
                            contentDescription = "Import PDF",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Narrator Voice Selector Row
                val selectedVoice = uiState.availableVoices.find { it.id == uiState.selectedVoiceName }
                val localeLabel = selectedVoice?.localeDisplayName ?: "English (United States)"
                val networkLabel = if (selectedVoice?.isNetworkRequired == true) "Network voice" else "Offline voice"

                Surface(
                    onClick = onOpenVoiceDialog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = localeLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Narrator · $networkLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = AppIcons.ChevronRight,
                            contentDescription = "Select Narrator",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // PDF Extraction Loader
        if (uiState.isExtractingPdf) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Notification Banner
        uiState.statusMessage?.let { status ->
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClearStatus,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Section 2: Book Content Card (Constrained & Scrollable)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Book Content",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val wordStr = if (uiState.wordCount > 0) "${String.format(Locale.US, "%,d", uiState.wordCount)} words" else "0 words"
                    Text(
                        text = wordStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = uiState.textPreview,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = { Text("Your book text appears here. Import a PDF or paste content to begin listening...") },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section 3: Dedicated Listening Panel (Always Visible)
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val isOfflineAudioReady = uiState.generationStatus == GenerationStatus.READY
                val isSavedMode = isOfflineAudioReady && uiState.activePlaybackMode == PlaybackMode.SAVED_AUDIO

                // Mode Selector Chips (when offline audio is ready)
                if (isOfflineAudioReady) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.activePlaybackMode == PlaybackMode.QUICK_LISTEN,
                            onClick = { onSelectPlaybackMode(PlaybackMode.QUICK_LISTEN) },
                            label = { Text("Quick Listen (Live)") }
                        )
                        FilterChip(
                            selected = uiState.activePlaybackMode == PlaybackMode.SAVED_AUDIO,
                            onClick = { onSelectPlaybackMode(PlaybackMode.SAVED_AUDIO) },
                            label = { Text("Saved Offline Audio") }
                        )
                    }
                }

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val headerTitle = if (isSavedMode) {
                            val pState = uiState.playerState
                            if (pState.isPlaying) "Playing Saved Audio"
                            else if (pState.isEnded) "Audiobook Finished"
                            else "Saved Audio Ready"
                        } else {
                            when (uiState.quickListenState) {
                                is TtsPlaybackState.Playing -> "Quick Listening: Chunk ${uiState.currentChunkIndex + 1} of ${uiState.totalChunks}"
                                is TtsPlaybackState.Paused -> "Quick Listen Paused"
                                else -> "Ready for Quick Listen"
                            }
                        }

                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val subtitleStr = if (isSavedMode) {
                            "Pre-rendered audio files · ExoPlayer"
                        } else {
                            if (uiState.quickListenState is TtsPlaybackState.Playing) {
                                "Live TTS speech · Speaking chunk ${uiState.currentChunkIndex + 1}/${uiState.totalChunks}"
                            } else {
                                "Tap Play for instant live speech"
                            }
                        }

                        Text(
                            text = subtitleStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = AppIcons.Headset,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.generationStatus == GenerationStatus.GENERATING) {
                    LinearProgressIndicator(
                        progress = { uiState.generationProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val pct = (uiState.generationProgress * 100).toInt()
                        Text(
                            text = "Synthesizing offline chunk ${uiState.generationCompletedChunks} of ${uiState.generationTotalChunks} ($pct%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onCancelGeneration) {
                            Text("Cancel")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Audio Timeline / Seek Slider
                if (isSavedMode) {
                    val pState = uiState.playerState
                    val totalDurationMs = pState.totalBookDurationMs
                    val elapsedMs = pState.totalElapsedMs
                    val progressFraction = if (totalDurationMs > 0L) {
                        (elapsedMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                    } else 0f

                    var savedSliderPos by remember(progressFraction) { mutableFloatStateOf(progressFraction) }
                    var isDraggingSavedSlider by remember { mutableStateOf(false) }

                    Slider(
                        value = if (isDraggingSavedSlider) savedSliderPos else progressFraction,
                        onValueChange = {
                            isDraggingSavedSlider = true
                            savedSliderPos = it
                        },
                        onValueChangeFinished = {
                            isDraggingSavedSlider = false
                            onSeekSavedAudio(savedSliderPos)
                        },
                        enabled = totalDurationMs > 0L,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimeMs(elapsedMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (totalDurationMs > 0L) formatTimeMs(totalDurationMs) else "--:--",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Quick Listen Chunk Seek Slider with smooth Drag handling
                    val totalChunks = uiState.totalChunks.coerceAtLeast(1)
                    val currentChunk = uiState.currentChunkIndex.coerceIn(0, totalChunks - 1)

                    var sliderChunkPos by remember(currentChunk) { mutableFloatStateOf(currentChunk.toFloat()) }
                    var isDraggingSlider by remember { mutableStateOf(false) }

                    Slider(
                        value = if (isDraggingSlider) sliderChunkPos else currentChunk.toFloat(),
                        onValueChange = {
                            isDraggingSlider = true
                            sliderChunkPos = it
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            onSeekQuickListen(sliderChunkPos.toInt())
                        },
                        valueRange = 0f..(totalChunks - 1).toFloat(),
                        steps = (totalChunks - 2).coerceAtLeast(0),
                        enabled = uiState.fullText.isNotBlank() && totalChunks > 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val displayChunk = if (isDraggingSlider) (sliderChunkPos.toInt() + 1) else (currentChunk + 1)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Chunk $displayChunk",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total $totalChunks chunks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Media Controls Row
                val isPlaying = if (isSavedMode) uiState.playerState.isPlaying else uiState.quickListenState is TtsPlaybackState.Playing

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip Previous Segment
                    IconButton(
                        onClick = {
                            if (isSavedMode) onSkipSavedPrev() else onSkipQuickListenPrev()
                        },
                        enabled = uiState.fullText.isNotBlank(),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.SkipPrevious,
                            contentDescription = "Previous Segment"
                        )
                    }

                    // Skip Back (5s for Saved Audio / 1 Chunk for Quick Listen)
                    IconButton(
                        onClick = {
                            if (isSavedMode) onSeekSavedAudioByOffset(-5000L) else onSkipQuickListenPrev()
                        },
                        enabled = uiState.fullText.isNotBlank(),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.SkipBack15,
                            contentDescription = if (isSavedMode) "Rewind 5s" else "Previous Chunk"
                        )
                    }

                    // Primary Central Play/Pause Button
                    FilledIconButton(
                        onClick = {
                            if (isSavedMode) onToggleSavedAudio() else onToggleQuickListen()
                        },
                        enabled = uiState.fullText.isNotBlank(),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) AppIcons.Pause else AppIcons.Play,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Skip Forward (5s for Saved Audio / 1 Chunk for Quick Listen)
                    IconButton(
                        onClick = {
                            if (isSavedMode) onSeekSavedAudioByOffset(5000L) else onSkipQuickListenNext()
                        },
                        enabled = uiState.fullText.isNotBlank(),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.SkipForward30,
                            contentDescription = if (isSavedMode) "Forward 5s" else "Next Chunk"
                        )
                    }

                    // Skip Next Segment
                    IconButton(
                        onClick = {
                            if (isSavedMode) onSkipSavedNext() else onSkipQuickListenNext()
                        },
                        enabled = uiState.fullText.isNotBlank(),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.SkipNext,
                            contentDescription = "Next Segment"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Caption
                val captionStr = when {
                    isSavedMode -> "Playing pre-rendered offline audio. Skip controls seek ±5 seconds."
                    isOfflineAudioReady -> "Offline audio ready. Toggle mode above or tap Play for Quick Listen."
                    else -> "Quick Listen plays live speech immediately. Prepare offline audio in top 3-dot menu."
                }

                Text(
                    text = captionStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
