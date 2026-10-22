package com.gowtham.pocketaudiobook.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val isReady: Boolean = false,
    val isEnded: Boolean = false,
    val currentSegmentIndex: Int = 0,
    val totalSegments: Int = 0,
    val segmentPositionMs: Long = 0L,
    val segmentDurationMs: Long = 0L,
    val totalElapsedMs: Long = 0L,
    val totalBookDurationMs: Long = 0L,
    val errorMessage: String? = null
)

class AudioPlayerManager(private val context: Context) {

    private var player: ExoPlayer? = null
    private var chunkFiles: List<File> = emptyList()
    private val segmentDurationsMs = mutableMapOf<Int, Long>()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    var onStateChanged: ((AudioPlayerState) -> Unit)? = null

    fun loadAudioChunks(
        files: List<File>,
        startSegmentIndex: Int = 0,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = false
    ) {
        release()

        if (files.isEmpty()) {
            emitState(AudioPlayerState(errorMessage = "No audio files available for playback."))
            return
        }

        chunkFiles = files
        segmentDurationsMs.clear()

        try {
            val exoPlayer = ExoPlayer.Builder(context.applicationContext).build()
            player = exoPlayer

            val mediaItems = files.map { file ->
                MediaItem.fromUri(Uri.fromFile(file))
            }

            exoPlayer.setMediaItems(mediaItems)

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    notifyStateChange()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    notifyStateChange()
                    if (isPlaying) {
                        startProgressPolling()
                    } else {
                        stopProgressPolling()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    notifyStateChange()
                }
            })

            val validIndex = startSegmentIndex.coerceIn(0, files.size - 1)
            exoPlayer.seekTo(validIndex, startPositionMs.coerceAtLeast(0L))
            exoPlayer.prepare()

            if (autoPlay) {
                exoPlayer.play()
            } else {
                notifyStateChange()
            }
        } catch (e: Exception) {
            emitState(AudioPlayerState(errorMessage = "Failed to initialize audio player: ${e.localizedMessage}"))
        }
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun stop() {
        player?.stop()
        notifyStateChange()
    }

    fun seekToSegment(segmentIndex: Int, positionMs: Long = 0L) {
        val p = player ?: return
        if (chunkFiles.isEmpty()) return
        val validIndex = segmentIndex.coerceIn(0, chunkFiles.size - 1)
        p.seekTo(validIndex, positionMs.coerceAtLeast(0L))
        notifyStateChange()
    }

    fun seekToTotalProgress(fraction: Float) {
        val p = player ?: return
        val totalMs = calculateTotalBookDurationMs()
        if (totalMs <= 0L) return

        val targetMs = (fraction.coerceIn(0f, 1f) * totalMs).toLong()
        
        var accumulated = 0L
        for (i in chunkFiles.indices) {
            val segDuration = getSegmentDuration(i)
            if (accumulated + segDuration >= targetMs || i == chunkFiles.size - 1) {
                val offsetInsideSeg = targetMs - accumulated
                p.seekTo(i, offsetInsideSeg.coerceAtLeast(0L))
                break
            }
            accumulated += segDuration
        }
        notifyStateChange()
    }

    fun seekByOffsetMs(offsetMs: Long) {
        val p = player ?: return
        val totalMs = calculateTotalBookDurationMs()
        if (totalMs <= 0L) return

        val segIdx = p.currentMediaItemIndex.coerceAtLeast(0)
        val segPosMs = p.currentPosition.coerceAtLeast(0L)

        var accumulatedMs = 0L
        for (i in 0 until segIdx) {
            accumulatedMs += getSegmentDuration(i)
        }
        val currentTotalElapsedMs = accumulatedMs + segPosMs
        val targetTotalMs = (currentTotalElapsedMs + offsetMs).coerceIn(0L, totalMs)

        seekToTotalProgress(targetTotalMs.toFloat() / totalMs)
    }

    fun skipToPreviousSegment() {
        val p = player ?: return
        if (p.hasPreviousMediaItem()) {
            p.seekToPreviousMediaItem()
        } else {
            p.seekTo(0, 0L)
        }
        notifyStateChange()
    }

    fun skipToNextSegment() {
        val p = player ?: return
        if (p.hasNextMediaItem()) {
            p.seekToNextMediaItem()
        }
        notifyStateChange()
    }

    fun release() {
        stopProgressPolling()
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {}
        player = null
        chunkFiles = emptyList()
        segmentDurationsMs.clear()
    }

    private fun startProgressPolling() {
        stopProgressPolling()
        updateRunnable = object : Runnable {
            override fun run() {
                notifyStateChange()
                mainHandler.postDelayed(this, 300L)
            }
        }
        updateRunnable?.let { mainHandler.post(it) }
    }

    private fun stopProgressPolling() {
        updateRunnable?.let { mainHandler.removeCallbacks(it) }
        updateRunnable = null
    }

    private fun notifyStateChange() {
        val p = player ?: run {
            emitState(AudioPlayerState())
            return
        }

        val segIdx = p.currentMediaItemIndex.coerceAtLeast(0)
        val segPosMs = p.currentPosition.coerceAtLeast(0L)
        val segDurMs = p.duration.coerceAtLeast(0L)

        if (segDurMs > 0L) {
            segmentDurationsMs[segIdx] = segDurMs
        }

        val totalBookDurMs = calculateTotalBookDurationMs()
        var accumulatedMs = 0L
        for (i in 0 until segIdx) {
            accumulatedMs += getSegmentDuration(i)
        }
        val totalElapsedMs = accumulatedMs + segPosMs

        val state = AudioPlayerState(
            isPlaying = p.isPlaying,
            isReady = p.playbackState == Player.STATE_READY,
            isEnded = p.playbackState == Player.STATE_ENDED,
            currentSegmentIndex = segIdx,
            totalSegments = chunkFiles.size,
            segmentPositionMs = segPosMs,
            segmentDurationMs = segDurMs,
            totalElapsedMs = totalElapsedMs,
            totalBookDurationMs = totalBookDurMs.coerceAtLeast(totalElapsedMs)
        )

        emitState(state)
    }

    private fun getSegmentDuration(index: Int): Long {
        return segmentDurationsMs[index] ?: 15000L
    }

    private fun calculateTotalBookDurationMs(): Long {
        var total = 0L
        for (i in chunkFiles.indices) {
            total += getSegmentDuration(i)
        }
        return total
    }

    private fun emitState(state: AudioPlayerState) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onStateChanged?.invoke(state)
        } else {
            mainHandler.post { onStateChanged?.invoke(state) }
        }
    }
}
