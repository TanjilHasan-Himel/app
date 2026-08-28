package com.eplaytime.app.service

import com.eplaytime.app.R

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.eplaytime.app.data.repository.MusicRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.eplaytime.app.util.AudioEffectManager

@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    companion object {
        const val ACTION_PLAY_ALARM = "com.eplaytime.app.action.PLAY_ALARM"
        const val EXTRA_SONG_URI = "extra_song_uri"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_TARGET_VOLUME = "extra_target_volume"
        const val EXTRA_ALARM_SINGLE_SHOT = "extra_alarm_single_shot"
        private const val TAG = "MusicService"
    }

    @Inject
    lateinit var musicRepository: MusicRepository

    private var player: ExoPlayer? = null
    private var mediaSession: MediaLibrarySession? = null
    private var isAlarmSingleShotMode = false
    private var fadeCoroutine: kotlinx.coroutines.Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY_ALARM) {
            val uriString = intent.getStringExtra(EXTRA_SONG_URI)
            val volume = intent.getFloatExtra(EXTRA_TARGET_VOLUME, 1.0f)
            val singleShot = intent.getBooleanExtra(EXTRA_ALARM_SINGLE_SHOT, true)
            
            if (uriString != null) {
                // 1. Override the system media volume to perfectly match exactly what the user scheduled
                try {
                    val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                    val maxSystemVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    
                    val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                    val hasHeadphones = devices.any {
                        it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
                    }
                    
                    val safeVolume = if (hasHeadphones) volume.coerceAtMost(0.5f) else volume
                    val targetSystemVolume = (maxSystemVolume * safeVolume.coerceIn(0f, 1f)).toInt()
                    
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetSystemVolume, 0)
                    Log.d(TAG, "System STREAM_MUSIC volume overridden to exactly $targetSystemVolume / $maxSystemVolume (Headphones: $hasHeadphones)")
                } catch (e: Exception) {
                    Log.e(TAG, "Could not override system volume (might lack permission)", e)
                }

                // Start absolute FGS immediately to prevent Android 12+ 10-second timeout FGS crash
                try {
                    val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val channel = android.app.NotificationChannel("playtime_al", "Audia Player Scheduler", android.app.NotificationManager.IMPORTANCE_LOW)
                        nm.createNotificationChannel(channel)
                    }
                    val notification = androidx.core.app.NotificationCompat.Builder(this, "playtime_al")
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setContentTitle("Scheduled Playback")
                        .setContentText("Starting...")
                        .build()
                    startForeground(12345, notification)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to startForeground", e)
                }

                val songTitle = intent.getStringExtra("extra_song_title") ?: "Alarm Playback"
                val metadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(songTitle)
                    .setArtist("Audia Player Scheduler")
                    .build()

                val item = MediaItem.Builder()
                    .setUri(uriString)
                    .setMediaMetadata(metadata)
                    .build()
                player?.let { exo ->
                    isAlarmSingleShotMode = singleShot
                    exo.stop()
                    exo.clearMediaItems()
                    exo.repeatMode = Player.REPEAT_MODE_OFF
                    exo.shuffleModeEnabled = false
                    exo.setMediaItem(item)
                    
                    // 2. Start internally at completely silent (0f) for the fade-in effect
                    exo.volume = 0f
                    exo.prepare()
                    exo.play()

                    // 3. Launch a smooth fade-in coroutine gracefully turning it up over 10 seconds
                    fadeCoroutine?.cancel()
                    fadeCoroutine = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val fadeDuration = 10_000L // 10 seconds
                        val fadeInterval = 100L    // update every 100ms
                        val steps = (fadeDuration / fadeInterval).toInt()
                        
                        // We fade internal volume from 0 to 1.0 (since the system-level volume is now perfect)
                        val targetInternalVolume = 1.0f
                        val volumeStep = targetInternalVolume / steps

                        for (i in 1..steps) {
                            kotlinx.coroutines.delay(fadeInterval)
                            if (player == null || player?.playWhenReady == false) break
                            exo.volume = (exo.volume + volumeStep).coerceAtMost(targetInternalVolume)
                        }
                        
                        exo.volume = targetInternalVolume
                        Log.d(TAG, "Fade-in playback complete and reached max level")
                    }

                    Log.d(TAG, "Playback scheduler started. singleShot=$singleShot uri=$uriString")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    // Callback for MediaSession events
    @OptIn(UnstableApi::class)
    private val sessionCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("root")
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

// ...

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            // Return empty for now, as UI drives playback via controller.setMediaItem
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            return try {
                val action = command.customAction
                if (action == "ACTION_GET_AUDIO_SESSION_ID") {
                    val extras = Bundle()
                    extras.putInt("AUDIO_SESSION_ID", player?.audioSessionId ?: 0)
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, extras))
                } else {
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling custom command", e)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_UNKNOWN))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (isAlarmSingleShotMode && playbackState == Player.STATE_ENDED) {
                    player?.pause()
                    player?.seekTo(0L)
                    isAlarmSingleShotMode = false
                    Log.d(TAG, "Alarm single-shot completed, stopping service")
                    stopSelf()
                }
            }
        })
    }

    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL) // Keep CPU awake
            .build()

        // Intent to open app when clicking notification
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaLibrarySession.Builder(this, player!!, sessionCallback)
            .setId("MusicService")
            .setSessionActivity(pendingIntent!!)
            .build()
            
        player?.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioEffectManager.initialize(audioSessionId)
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onDestroy() {
        AudioEffectManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Stop service if task removed, but keep notification active if playing?
        // Usually handled by MediaSessionService logic.
        // For simplicity:
        if (player?.playWhenReady == false || player?.mediaItemCount == 0) {
            isAlarmSingleShotMode = false
            stopSelf()
        }
    }
}
