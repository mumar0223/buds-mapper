package com.budsmapper.app.media

import android.content.Context
import android.content.Intent
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import com.budsmapper.app.data.LastEventStore

class BudsMediaSessionManager(private val context: Context) {

    private val eventStore = LastEventStore.getInstance(context)
    private var mediaSession: MediaSession? = null

    @Synchronized
    fun setCaptureEnabled(enabled: Boolean) {
        if (enabled) {
            if (mediaSession == null) {
                initializeSession()
            }
            mediaSession?.isActive = true
            Log.i(TAG, "MediaSession activated for diagnostic capture")
        } else {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
            Log.i(TAG, "MediaSession deactivated and released")
        }
    }

    @Synchronized
    fun isSessionActive(): Boolean {
        return mediaSession?.isActive == true
    }

    private fun initializeSession() {
        val session = MediaSession(context, SESSION_TAG)
        
        session.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
            MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        val state = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_FAST_FORWARD or
                PlaybackState.ACTION_REWIND
            )
            .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        session.setPlaybackState(state)

        session.setCallback(object : MediaSession.Callback() {
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                val keyEvent: KeyEvent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                }

                if (keyEvent != null) {
                    val keyName = KeyEvent.keyCodeToString(keyEvent.keyCode)
                    Log.i(
                        TAG,
                        "onMediaButtonEvent intercepted! keyCode=${keyEvent.keyCode} ($keyName), action=${keyEvent.action}, deviceId=${keyEvent.deviceId}"
                    )

                    // Record event to LastEventStore with source tag "MediaSession"
                    eventStore.saveEvent(keyEvent, source = "MediaSession")

                    // Return true to consume the event and confirm our session owns media-button routing
                    return true
                }

                return super.onMediaButtonEvent(mediaButtonIntent)
            }

            override fun onPlay() {
                Log.d(TAG, "MediaSession.Callback.onPlay()")
            }

            override fun onPause() {
                Log.d(TAG, "MediaSession.Callback.onPause()")
            }

            override fun onSkipToNext() {
                Log.d(TAG, "MediaSession.Callback.onSkipToNext()")
            }

            override fun onSkipToPrevious() {
                Log.d(TAG, "MediaSession.Callback.onSkipToPrevious()")
            }
        })

        mediaSession = session
    }

    companion object {
        private const val TAG = "BudsMediaSession"
        private const val SESSION_TAG = "BudsMapperMediaSession"
    }
}

