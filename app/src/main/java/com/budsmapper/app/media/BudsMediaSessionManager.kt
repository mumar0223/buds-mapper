package com.budsmapper.app.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.media.AudioTrack
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.budsmapper.app.accessibility.BudsAccessibilityService
import com.budsmapper.app.data.AppProfileStore
import com.budsmapper.app.data.BudsMediaCommand
import com.budsmapper.app.data.LastEventStore

class BudsMediaSessionManager(private val context: Context) {

    private val eventStore = LastEventStore.getInstance(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaSession: MediaSession? = null
    private var silentAudioTrack: AudioTrack? = null
    private var lastPulseTimestamp: Long = 0L

    // Callback to detect when YouTube/Spotify starts audio so we can reclaim mediaButtonSession priority
    private val audioPlaybackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            super.onPlaybackConfigChanged(configs)
            if (!isSessionActive()) return

            val mediaConfigs = configs.filter { config ->
                config.audioAttributes.usage == AudioAttributes.USAGE_MEDIA
            }

            // Only pulse if another media app is active alongside our silent track, with a 3000ms cooldown
            val now = SystemClock.uptimeMillis()
            if (mediaConfigs.size > 1 && (now - lastPulseTimestamp) >= 3000L) {
                lastPulseTimestamp = now
                Log.d(TAG, "External media audio detected (${mediaConfigs.size} sessions). Pulsing silent AudioTrack.")
                pulseSilentAudio()
            }
        }
    }

    @Synchronized
    fun setCaptureEnabled(enabled: Boolean) {
        if (enabled) {
            if (mediaSession == null) {
                initializeSession()
            }
            startSilentAudio()
            mediaSession?.isActive = true
            registerPlaybackCallback()
            Log.i(TAG, "MediaSession & Silent AudioTrack activated for priority capture")
        } else {
            unregisterPlaybackCallback()
            stopSilentAudio()
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
            Log.i(TAG, "MediaSession & Silent AudioTrack deactivated and released")
        }
    }

    @Synchronized
    fun isSessionActive(): Boolean {
        return mediaSession?.isActive == true
    }

    @Synchronized
    fun getSessionToken(): MediaSession.Token? {
        if (mediaSession == null) {
            initializeSession()
        }
        return mediaSession?.sessionToken
    }

    @Synchronized
    fun updateMetadata(title: String, artist: String, album: String, artwork: Bitmap) {
        if (mediaSession == null) {
            initializeSession()
        }
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
            .putBitmap(MediaMetadata.METADATA_KEY_ART, artwork)
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun startSilentAudio() {
        if (silentAudioTrack != null) return

        try {
            val sampleRate = 8000
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val silence = ByteArray(bufferSize) // Zero-filled PCM = complete silence
            track.write(silence, 0, silence.size)
            track.setLoopPoints(0, bufferSize / 2, -1) // Loop infinitely
            track.play()
            silentAudioTrack = track
            Log.i(TAG, "Silent AudioTrack started on USAGE_MEDIA to satisfy AudioPlayerStateMonitor")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start silent AudioTrack", e)
        }
    }

    private fun stopSilentAudio() {
        try {
            silentAudioTrack?.stop()
            silentAudioTrack?.release()
            silentAudioTrack = null
            Log.i(TAG, "Silent AudioTrack stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping silent AudioTrack", e)
        }
    }

    private fun pulseSilentAudio() {
        // Briefly restart our AudioTrack to become the most recently started USAGE_MEDIA stream
        mainHandler.post {
            try {
                silentAudioTrack?.pause()
                silentAudioTrack?.play()
                mediaSession?.setPlaybackState(buildPlayingState())
                Log.d(TAG, "Silent AudioTrack pulsed successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Could not pulse silent AudioTrack", e)
            }
        }
    }

    private fun registerPlaybackCallback() {
        try {
            audioManager?.registerAudioPlaybackCallback(audioPlaybackCallback, mainHandler)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register audio playback callback", e)
        }
    }

    private fun unregisterPlaybackCallback() {
        try {
            audioManager?.unregisterAudioPlaybackCallback(audioPlaybackCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister audio playback callback", e)
        }
    }

    private fun buildPlayingState(): PlaybackState {
        return PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_FAST_FORWARD or
                PlaybackState.ACTION_REWIND
            )
            .setState(PlaybackState.STATE_PLAYING, 0L, 1.0f)
            .build()
    }

    private fun initializeSession() {
        val session = MediaSession(context, SESSION_TAG)

        @Suppress("DEPRECATION")
        session.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
            MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        session.setPlaybackState(buildPlayingState())

        session.setCallback(object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                }

                if (keyEvent != null) {
                    var wasActionConsumed = false

                    // Only process ACTION_DOWN to avoid double-triggering gestures
                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        val keyName = KeyEvent.keyCodeToString(keyEvent.keyCode)
                        val currentPkg = eventStore.currentForegroundPackage.value
                        val currentLabel = eventStore.currentForegroundLabel.value
                        val profileStore = AppProfileStore.getInstance(context)

                        if (profileStore.isProfileEnabled(currentPkg)) {
                            val command = when (keyEvent.keyCode) {
                                KeyEvent.KEYCODE_MEDIA_PLAY -> BudsMediaCommand.MEDIA_PLAY
                                KeyEvent.KEYCODE_MEDIA_PAUSE -> BudsMediaCommand.MEDIA_PAUSE
                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> BudsMediaCommand.MEDIA_PLAY_PAUSE
                                KeyEvent.KEYCODE_MEDIA_NEXT -> BudsMediaCommand.MEDIA_NEXT
                                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> BudsMediaCommand.MEDIA_PREVIOUS
                                else -> null
                            }
                            if (command != null) {
                                val action = profileStore.getMapping(currentPkg, command)
                                val desc = "${action.displayName} ($currentLabel)"
                                Log.i(
                                    TAG,
                                    "Profile active for $currentLabel ($currentPkg): ${command.key} -> ${action.displayName}"
                                )
                                eventStore.saveEvent(keyEvent, source = "MediaSession", mappedAction = desc)

                                // Dispatch physical gesture on screen via AccessibilityService!
                                wasActionConsumed = BudsAccessibilityService.instance?.executeAction(action) ?: false
                            }
                        } else {
                            Log.d(TAG, "Profile disabled for foreground app: $currentLabel ($currentPkg)")
                            eventStore.saveEvent(
                                keyEvent,
                                source = "MediaSession",
                                mappedAction = "Pass-through ($currentLabel)"
                            )
                        }
                    }

                    // Return true if our mapped gesture consumed the event;
                    // otherwise return false so native Android media handling passes through!
                    if (wasActionConsumed) {
                        return true
                    }
                    return false
                }

                return super.onMediaButtonEvent(mediaButtonIntent)
            }
        })

        mediaSession = session
    }

    companion object {
        private const val TAG = "BudsMediaSession"
        private const val SESSION_TAG = "BudsMapperMediaSession"
    }
}
