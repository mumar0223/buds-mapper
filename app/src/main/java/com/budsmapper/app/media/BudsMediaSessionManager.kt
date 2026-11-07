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
import com.budsmapper.app.data.MappedAction

class BudsMediaSessionManager(private val context: Context) {

    private val eventStore = LastEventStore.getInstance(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaSession: MediaSession? = null

    // Dual lightweight AudioTracks for instantaneous, timer-free handoff:
    // Alternating tracks provides fresh Player Interface IDs (piid) so AudioPlayerStateMonitor
    // registers a genuine STARTED event without coalescing or delay.
    private var trackA: AudioTrack? = null
    private var trackB: AudioTrack? = null
    private var isTrackAActive: Boolean = true
    private var lastSelfSwapUptime: Long = 0L

    // Event-driven callback: fires whenever audio playback states change in the system
    private val audioPlaybackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            super.onPlaybackConfigChanged(configs)
            if (!isSessionActive()) return

            // Ignore the immediate reflection from our own track handoffs (< 100ms)
            val now = SystemClock.uptimeMillis()
            if (now - lastSelfSwapUptime < 100L) return

            val currentPkg = eventStore.currentForegroundPackage.value
            if (!AppProfileStore.getInstance(context).isProfileEnabled(currentPkg)) return

            val mediaConfigs = configs.count { config ->
                config.audioAttributes.usage == AudioAttributes.USAGE_MEDIA
            }

            // If another media app (e.g. YouTube) starts outputting audio alongside us,
            // immediately hand off to the standby track so we become the newest active media stream.
            // Zero ms timers, zero polling!
            if (mediaConfigs > 1) {
                Log.d(TAG, "External media playback detected in enabled app ($currentPkg). Swapping sub-audible track.")
                swapActiveAudioTrack()
            }
        }
    }

    @Synchronized
    fun setCaptureEnabled(enabled: Boolean) {
        if (enabled) {
            if (mediaSession == null) {
                initializeSession()
            }
            startSubAudibleAudio()
            mediaSession?.isActive = true
            registerPlaybackCallback()
            Log.i(TAG, "MediaSession & Sub-Audible Dual AudioTracks activated for priority capture")
        } else {
            unregisterPlaybackCallback()
            stopSubAudibleAudio()
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
            Log.i(TAG, "MediaSession & Sub-Audible Dual AudioTracks deactivated and released")
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

    /**
     * Creates a lightweight MODE_STATIC AudioTrack filled with alternating +1 and -1 16-bit PCM samples (-90.3 dBFS).
     *
     * Why non-zero PCM?
     * Samsung One UI 6.1 AudioFlinger monitors active USAGE_MEDIA streams for digital zero frames (0x0000).
     * If pure zeros are detected for 3-5 seconds, Samsung flags the stream as silent/idle and demotes priority.
     * Alternating +1 (0x0001) and -1 (0xFFFF) creates continuous waveform activity with zero DC offset,
     * remaining 100% inaudible below the hardware DAC thermal noise floor while preventing AudioFlinger silence detection!
     */
    private fun createNonZeroSubAudibleTrack(): AudioTrack {
        val sampleRate = 8000
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // Ensure bufferSize is a multiple of 4 bytes (2 samples of 16-bit PCM)
        val bufferSize = if (minBufferSize % 4 == 0) minBufferSize else minBufferSize + (4 - (minBufferSize % 4))

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

        val pcm = ByteArray(bufferSize)
        var i = 0
        while (i < bufferSize) {
            // Little-endian 16-bit PCM:
            // Sample +1 = 0x0001 -> byte 0x01, byte 0x00
            pcm[i] = 0x01
            pcm[i + 1] = 0x00
            // Sample -1 = 0xFFFF -> byte 0xFF, byte 0xFF
            pcm[i + 2] = 0xFF.toByte()
            pcm[i + 3] = 0xFF.toByte()
            i += 4
        }

        track.write(pcm, 0, pcm.size)
        val frameCount = bufferSize / 2
        track.setLoopPoints(0, frameCount, -1) // Loop infinitely
        track.setVolume(0.01f) // Extra safeguard: volume reduced to 1%
        return track
    }

    @Synchronized
    private fun startSubAudibleAudio() {
        if (trackA != null && trackB != null) return

        try {
            val a = createNonZeroSubAudibleTrack()
            val b = createNonZeroSubAudibleTrack()
            lastSelfSwapUptime = SystemClock.uptimeMillis()
            a.play()
            trackA = a
            trackB = b
            isTrackAActive = true
            Log.i(TAG, "Sub-audible dual AudioTracks started. TrackA active on USAGE_MEDIA.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start sub-audible AudioTracks", e)
        }
    }

    @Synchronized
    private fun stopSubAudibleAudio() {
        try {
            trackA?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop()
                it.release()
            }
            trackB?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping sub-audible AudioTracks", e)
        } finally {
            trackA = null
            trackB = null
            Log.i(TAG, "Sub-audible AudioTracks stopped and released")
        }
    }

    /**
     * Atomically hands off priority from active track to standby track.
     * This registers a fresh AudioPlaybackConfiguration STARTED event with a new PIID,
     * immediately claiming index 0 in the MediaSessionStack without timers or delay.
     */
    @Synchronized
    fun swapActiveAudioTrack() {
        val a = trackA ?: return
        val b = trackB ?: return

        try {
            lastSelfSwapUptime = SystemClock.uptimeMillis()
            if (isTrackAActive) {
                b.reloadStaticData()
                b.setLoopPoints(0, b.bufferSizeInFrames, -1)
                b.play()
                if (a.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    a.stop()
                }
                isTrackAActive = false
            } else {
                a.reloadStaticData()
                a.setLoopPoints(0, a.bufferSizeInFrames, -1)
                a.play()
                if (b.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    b.stop()
                }
                isTrackAActive = true
            }
            mediaSession?.setPlaybackState(buildPlayingState())
            Log.d(TAG, "Instant dual-track handoff completed (isTrackAActive=$isTrackAActive)")
        } catch (e: Exception) {
            Log.w(TAG, "Could not swap sub-audible AudioTrack", e)
        }
    }

    fun reclaimPriority() {
        if (!isSessionActive()) return
        val currentPkg = eventStore.currentForegroundPackage.value
        val isEnabled = AppProfileStore.getInstance(context).isProfileEnabled(currentPkg)
        if (!isEnabled) return

        swapActiveAudioTrack()
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
                    val currentPkg = eventStore.currentForegroundPackage.value
                    val currentLabel = eventStore.currentForegroundLabel.value
                    val profileStore = AppProfileStore.getInstance(context)

                    if (!profileStore.isProfileEnabled(currentPkg)) {
                        return false
                    }

                    val command = when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY -> BudsMediaCommand.MEDIA_PLAY
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> BudsMediaCommand.MEDIA_PAUSE
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> BudsMediaCommand.MEDIA_PLAY_PAUSE
                        KeyEvent.KEYCODE_MEDIA_NEXT -> BudsMediaCommand.MEDIA_NEXT
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> BudsMediaCommand.MEDIA_PREVIOUS
                        else -> null
                    } ?: return false

                    val action = profileStore.getMapping(currentPkg, command)
                    if (action == MappedAction.DEFAULT) {
                        return false
                    }

                    if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                        val desc = "${action.displayName} ($currentLabel)"
                        Log.i(
                            TAG,
                            "Profile active for $currentLabel ($currentPkg): ${command.key} -> ${action.displayName}"
                        )
                        eventStore.saveEvent(keyEvent, source = "MediaSession", mappedAction = desc)

                        // Dispatch physical gesture on screen via AccessibilityService!
                        BudsAccessibilityService.instance?.executeAction(action)
                    }

                    // Consume BOTH ACTION_DOWN and ACTION_UP for mapped actions so key release
                    // never leaks through to the media player
                    return true
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
