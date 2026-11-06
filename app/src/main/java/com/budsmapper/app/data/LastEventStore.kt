package com.budsmapper.app.data

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecordedKeyEvent(
    val keyCode: Int,
    val keyName: String,
    val action: String,
    val timestamp: Long,
    val deviceId: Int,
    val source: String = "AccessibilityService" // "AccessibilityService" or "MediaSession"
)

data class CommandDetectionState(
    val mediaPlayDetected: Boolean = false,
    val mediaPlayTimestamp: Long = 0L,
    val mediaPauseDetected: Boolean = false,
    val mediaPauseTimestamp: Long = 0L,
    val mediaNextDetected: Boolean = false,
    val mediaNextTimestamp: Long = 0L,
    val mediaPreviousDetected: Boolean = false,
    val mediaPreviousTimestamp: Long = 0L
)

class LastEventStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _lastEventFlow = MutableStateFlow<RecordedKeyEvent?>(loadLastEvent())
    val lastEventFlow: StateFlow<RecordedKeyEvent?> = _lastEventFlow.asStateFlow()

    private val _detectionStateFlow = MutableStateFlow(loadDetectionState())
    val detectionStateFlow: StateFlow<CommandDetectionState> = _detectionStateFlow.asStateFlow()

    private val _mediaSessionCaptureEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_MEDIA_SESSION_CAPTURE_ENABLED, false)
    )
    val mediaSessionCaptureEnabled: StateFlow<Boolean> = _mediaSessionCaptureEnabled.asStateFlow()

    private val _currentForegroundPackage = MutableStateFlow(
        prefs.getString(KEY_FOREGROUND_PACKAGE, "com.budsmapper.app") ?: "com.budsmapper.app"
    )
    val currentForegroundPackage: StateFlow<String> = _currentForegroundPackage.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_MEDIA_SESSION_CAPTURE_ENABLED -> {
                _mediaSessionCaptureEnabled.value = prefs.getBoolean(KEY_MEDIA_SESSION_CAPTURE_ENABLED, false)
            }
            KEY_CODE, KEY_SOURCE, KEY_TIMESTAMP -> {
                _lastEventFlow.value = loadLastEvent()
                _detectionStateFlow.value = loadDetectionState()
            }
            KEY_CMD_PLAY_DETECTED, KEY_CMD_PAUSE_DETECTED, KEY_CMD_NEXT_DETECTED, KEY_CMD_PREV_DETECTED -> {
                _detectionStateFlow.value = loadDetectionState()
            }
            KEY_FOREGROUND_PACKAGE -> {
                _currentForegroundPackage.value = prefs.getString(KEY_FOREGROUND_PACKAGE, "com.budsmapper.app") ?: "com.budsmapper.app"
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setMediaSessionCaptureEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MEDIA_SESSION_CAPTURE_ENABLED, enabled).apply()
        _mediaSessionCaptureEnabled.value = enabled
    }

    fun isMediaSessionCaptureEnabled(): Boolean {
        return prefs.getBoolean(KEY_MEDIA_SESSION_CAPTURE_ENABLED, false)
    }

    fun setCurrentForegroundPackage(pkg: String) {
        prefs.edit().putString(KEY_FOREGROUND_PACKAGE, pkg).apply()
        _currentForegroundPackage.value = pkg
    }

    fun saveEvent(event: KeyEvent, source: String = "AccessibilityService") {
        val keyCode = event.keyCode
        val keyName = KeyEvent.keyCodeToString(keyCode)
        val action = when (event.action) {
            KeyEvent.ACTION_DOWN -> "ACTION_DOWN"
            KeyEvent.ACTION_UP -> "ACTION_UP"
            else -> "ACTION(${event.action})"
        }
        val timestamp = System.currentTimeMillis()
        val deviceId = event.deviceId

        val editor = prefs.edit()
            .putInt(KEY_CODE, keyCode)
            .putString(KEY_NAME, keyName)
            .putString(KEY_ACTION, action)
            .putLong(KEY_TIMESTAMP, timestamp)
            .putInt(KEY_DEVICE_ID, deviceId)
            .putString(KEY_SOURCE, source)

        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                editor.putBoolean(KEY_CMD_PLAY_DETECTED, true)
                editor.putLong(KEY_CMD_PLAY_TIME, timestamp)
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                editor.putBoolean(KEY_CMD_PAUSE_DETECTED, true)
                editor.putLong(KEY_CMD_PAUSE_TIME, timestamp)
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                editor.putBoolean(KEY_CMD_PLAY_DETECTED, true)
                editor.putLong(KEY_CMD_PLAY_TIME, timestamp)
                editor.putBoolean(KEY_CMD_PAUSE_DETECTED, true)
                editor.putLong(KEY_CMD_PAUSE_TIME, timestamp)
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                editor.putBoolean(KEY_CMD_NEXT_DETECTED, true)
                editor.putLong(KEY_CMD_NEXT_TIME, timestamp)
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                editor.putBoolean(KEY_CMD_PREV_DETECTED, true)
                editor.putLong(KEY_CMD_PREV_TIME, timestamp)
            }
        }
        editor.apply()
    }

    fun reload() {
        _lastEventFlow.value = loadLastEvent()
        _detectionStateFlow.value = loadDetectionState()
        _mediaSessionCaptureEnabled.value = prefs.getBoolean(KEY_MEDIA_SESSION_CAPTURE_ENABLED, false)
        _currentForegroundPackage.value = prefs.getString(KEY_FOREGROUND_PACKAGE, "com.budsmapper.app") ?: "com.budsmapper.app"
    }

    private fun loadLastEvent(): RecordedKeyEvent? {
        if (!prefs.contains(KEY_CODE)) return null
        return RecordedKeyEvent(
            keyCode = prefs.getInt(KEY_CODE, 0),
            keyName = prefs.getString(KEY_NAME, "UNKNOWN") ?: "UNKNOWN",
            action = prefs.getString(KEY_ACTION, "UNKNOWN") ?: "UNKNOWN",
            timestamp = prefs.getLong(KEY_TIMESTAMP, 0L),
            deviceId = prefs.getInt(KEY_DEVICE_ID, 0),
            source = prefs.getString(KEY_SOURCE, "AccessibilityService") ?: "AccessibilityService"
        )
    }

    private fun loadDetectionState(): CommandDetectionState {
        return CommandDetectionState(
            mediaPlayDetected = prefs.getBoolean(KEY_CMD_PLAY_DETECTED, false),
            mediaPlayTimestamp = prefs.getLong(KEY_CMD_PLAY_TIME, 0L),
            mediaPauseDetected = prefs.getBoolean(KEY_CMD_PAUSE_DETECTED, false),
            mediaPauseTimestamp = prefs.getLong(KEY_CMD_PAUSE_TIME, 0L),
            mediaNextDetected = prefs.getBoolean(KEY_CMD_NEXT_DETECTED, false),
            mediaNextTimestamp = prefs.getLong(KEY_CMD_NEXT_TIME, 0L),
            mediaPreviousDetected = prefs.getBoolean(KEY_CMD_PREV_DETECTED, false),
            mediaPreviousTimestamp = prefs.getLong(KEY_CMD_PREV_TIME, 0L)
        )
    }

    companion object {
        private const val PREFS_NAME = "buds_mapper_events"
        private const val KEY_CODE = "last_key_code"
        private const val KEY_NAME = "last_key_name"
        private const val KEY_ACTION = "last_action"
        private const val KEY_TIMESTAMP = "last_timestamp"
        private const val KEY_DEVICE_ID = "last_device_id"
        private const val KEY_SOURCE = "last_source"
        private const val KEY_MEDIA_SESSION_CAPTURE_ENABLED = "media_session_capture_enabled"
        private const val KEY_FOREGROUND_PACKAGE = "foreground_package"

        private const val KEY_CMD_PLAY_DETECTED = "cmd_play_detected"
        private const val KEY_CMD_PLAY_TIME = "cmd_play_time"
        private const val KEY_CMD_PAUSE_DETECTED = "cmd_pause_detected"
        private const val KEY_CMD_PAUSE_TIME = "cmd_pause_time"
        private const val KEY_CMD_NEXT_DETECTED = "cmd_next_detected"
        private const val KEY_CMD_NEXT_TIME = "cmd_next_time"
        private const val KEY_CMD_PREV_DETECTED = "cmd_prev_detected"
        private const val KEY_CMD_PREV_TIME = "cmd_prev_time"

        @Volatile
        private var instance: LastEventStore? = null

        fun getInstance(context: Context): LastEventStore {
            return instance ?: synchronized(this) {
                instance ?: LastEventStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
