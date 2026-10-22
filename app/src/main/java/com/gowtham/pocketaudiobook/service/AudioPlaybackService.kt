package com.gowtham.pocketaudiobook.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.gowtham.pocketaudiobook.MainActivity
import com.gowtham.pocketaudiobook.R
import com.gowtham.pocketaudiobook.tts.TtsManager

class AudioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var receiverRegistered = false

    companion object {
        private const val TAG = "PocketAudiobook"

        const val CHANNEL_ID = "pocket_audiobook_media_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_FOREGROUND = "com.gowtham.pocketaudiobook.START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "com.gowtham.pocketaudiobook.STOP_FOREGROUND"

        const val ACTION_PLAY_PAUSE = "com.gowtham.pocketaudiobook.ACTION_PLAY_PAUSE"
        const val ACTION_PREVIOUS = "com.gowtham.pocketaudiobook.ACTION_PREVIOUS"
        const val ACTION_NEXT = "com.gowtham.pocketaudiobook.ACTION_NEXT"

        // Base64 1-second silent WAV data URI to represent active TTS state in ExoPlayer/MediaSession
        private const val SILENT_WAV_URI = "data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA="

        @Volatile
        var onMediaActionReceived: ((action: String) -> Unit)? = null

        fun startService(context: Context, title: String, subtitle: String, isPlaying: Boolean) {
            try {
                Log.d(TAG, "[PlaybackService] startService: title='$title', subtitle='$subtitle', isPlaying=$isPlaying")
                val intent = Intent(context, AudioPlaybackService::class.java).apply {
                    action = ACTION_START_FOREGROUND
                    putExtra("title", title)
                    putExtra("subtitle", subtitle)
                    putExtra("isPlaying", isPlaying)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "[PlaybackService] Error starting service: ${e.localizedMessage}")
            }
        }

        fun stopService(context: Context) {
            try {
                Log.d(TAG, "[PlaybackService] stopService called")
                val intent = Intent(context, AudioPlaybackService::class.java).apply {
                    action = ACTION_STOP_FOREGROUND
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "[PlaybackService] Error stopping service: ${e.localizedMessage}")
            }
        }
    }

    private val mediaActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            Log.d(TAG, "[PlaybackService] BroadcastReceiver onReceive action: $action")
            onMediaActionReceived?.invoke(action)
        }
    }

    private val mediaSessionCallback = object : MediaSession.Callback {
        @Deprecated("Deprecated in Java")
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            Log.d(TAG, "[PlaybackService] MediaSession.Callback onPlayerCommandRequest: $playerCommand")
            when (playerCommand) {
                Player.COMMAND_PLAY_PAUSE -> {
                    onMediaActionReceived?.invoke(ACTION_PLAY_PAUSE)
                }
                Player.COMMAND_SEEK_TO_PREVIOUS -> {
                    onMediaActionReceived?.invoke(ACTION_PREVIOUS)
                }
                Player.COMMAND_SEEK_TO_NEXT -> {
                    onMediaActionReceived?.invoke(ACTION_NEXT)
                }
                Player.COMMAND_STOP -> {
                    onMediaActionReceived?.invoke(ACTION_STOP_FOREGROUND)
                }
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "[PlaybackService] onCreate")
        createNotificationChannel()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Prepare ExoPlayer ONCE with silent item to maintain STATE_READY without entering STATE_BUFFERING
        val player = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            setMediaItem(MediaItem.fromUri(Uri.parse(SILENT_WAV_URI)))
            prepare()
        }
        exoPlayer = player

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(mediaSessionCallback)
            .setSessionActivity(pendingIntent)
            .build()

        registerMediaReceiver()
    }

    private fun registerMediaReceiver() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_PLAY_PAUSE)
                addAction(ACTION_PREVIOUS)
                addAction(ACTION_NEXT)
                addAction(ACTION_STOP_FOREGROUND)
            }
            ContextCompat.registerReceiver(
                this,
                mediaActionReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
            Log.d(TAG, "[PlaybackService] BroadcastReceiver registered")
        }
    }

    private fun unregisterMediaReceiver() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(mediaActionReceiver)
            } catch (_: Exception) {}
            receiverRegistered = false
            Log.d(TAG, "[PlaybackService] BroadcastReceiver unregistered")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "[PlaybackService] onStartCommand action: $action")
        when (action) {
            ACTION_START_FOREGROUND -> {
                val title = intent.getStringExtra("title") ?: "PocketAudiobook"
                val subtitle = intent.getStringExtra("subtitle") ?: "Audiobook Playback"
                val isPlaying = intent.getBooleanExtra("isPlaying", false)
                
                updateServicePlaybackState(title, subtitle, isPlaying)
            }
            ACTION_PLAY_PAUSE, ACTION_PREVIOUS, ACTION_NEXT -> {
                onMediaActionReceived?.invoke(action)
            }
            ACTION_STOP_FOREGROUND -> {
                exoPlayer?.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val t0 = SystemClock.elapsedRealtime()
        Log.d(TAG, "[PlaybackService] onTaskRemoved received at t=$t0 ms")

        // 1. Immediately & synchronously stop TTS speech without waiting for ViewModel/UI thread
        TtsManager.stopCurrentSpeechImmediately()

        // 2. Immediately stop ExoPlayer
        try {
            exoPlayer?.stop()
            exoPlayer?.pause()
        } catch (_: Exception) {}

        // 3. Notify ViewModel & clean up service foreground status
        try {
            onMediaActionReceived?.invoke(ACTION_STOP_FOREGROUND)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (_: Exception) {}

        val t1 = SystemClock.elapsedRealtime()
        Log.d(TAG, "[PlaybackService] onTaskRemoved cleanup finished at t=$t1 ms (elapsed=${t1 - t0} ms)")

        super.onTaskRemoved(rootIntent)
    }

    private fun updateServicePlaybackState(title: String, subtitle: String, isPlaying: Boolean) {
        val p = exoPlayer
        if (p != null) {
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(subtitle)
                .setDisplayTitle(title)
                .setSubtitle(subtitle)
                .build()

            // Update metadata without replacing media item to avoid STATE_BUFFERING
            p.playlistMetadata = mediaMetadata

            if (isPlaying) {
                if (!p.isPlaying) {
                    p.play()
                }
            } else {
                if (p.isPlaying) {
                    p.pause()
                }
            }

            Log.d(TAG, "[PlaybackService] MediaSession state: isPlaying=$isPlaying (ExoPlayer.playbackState=${p.playbackState}, ExoPlayer.isPlaying=${p.isPlaying})")
        }

        startForegroundNotification(title, subtitle, isPlaying)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun startForegroundNotification(title: String, subtitle: String, isPlaying: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getBroadcast(
            this, 1, Intent(ACTION_PREVIOUS).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getBroadcast(
            this, 2, Intent(ACTION_PLAY_PAUSE).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getBroadcast(
            this, 3, Intent(ACTION_NEXT).setPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseText = if (isPlaying) "Pause" else "Play"

        val sessionToken = mediaSession?.platformToken?.let { token ->
            MediaSessionCompat.Token.fromToken(token)
        }

        val mediaStyle = MediaNotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)

        if (sessionToken != null) {
            mediaStyle.setMediaSession(sessionToken)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText("PocketAudiobook")
            .setContentIntent(pendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(playPauseIcon, playPauseText, playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .setStyle(mediaStyle)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
            Log.d(TAG, "[PlaybackService] Notification published: title='$title', actionIcon=${if (isPlaying) "PAUSE (Ⅱ)" else "PLAY (▶)"}")
        } catch (e: Exception) {
            Log.e(TAG, "[PlaybackService] startForeground error: ${e.localizedMessage}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PocketAudiobook Playback",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Active audiobook media controls"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "[PlaybackService] onDestroy")
        unregisterMediaReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        }
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        super.onDestroy()
    }
}
