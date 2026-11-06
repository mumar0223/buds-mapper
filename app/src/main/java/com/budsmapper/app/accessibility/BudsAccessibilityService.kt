package com.budsmapper.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.media.BudsMediaSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * BudsAccessibilityService
 *
 * Responsibilities:
 * - Intercepts physical system hardware buttons (Volume Up/Down) via onKeyEvent()
 * - Hosts BudsMediaSessionManager to keep MediaSession & Silent AudioTrack alive in background
 * - Observes foreground app package changes via TYPE_WINDOW_STATE_CHANGED
 * - CRITICAL: Returns false from onKeyEvent() so normal volume routing is not blocked
 */
class BudsAccessibilityService : AccessibilityService() {

    private lateinit var eventStore: LastEventStore
    private lateinit var mediaSessionManager: BudsMediaSessionManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        eventStore = LastEventStore.getInstance(this)
        mediaSessionManager = BudsMediaSessionManager(this)

        // Sync MediaSession lifecycle with user preference in background
        serviceScope.launch {
            eventStore.mediaSessionCaptureEnabled.collect { enabled ->
                Log.i(TAG, "Syncing MediaSession capture state: enabled=$enabled")
                mediaSessionManager.setCaptureEnabled(enabled)
            }
        }

        Log.i(TAG, "BudsAccessibilityService initialized with MediaSession host")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "BudsAccessibilityService connected to accessibility subsystem")
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        serviceInfo = info

        if (eventStore.isMediaSessionCaptureEnabled()) {
            mediaSessionManager.setCaptureEnabled(true)
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        val keyCode = event.keyCode
        val actionName = when (event.action) {
            KeyEvent.ACTION_DOWN -> "ACTION_DOWN"
            KeyEvent.ACTION_UP -> "ACTION_UP"
            else -> "ACTION(${event.action})"
        }
        val keyName = KeyEvent.keyCodeToString(keyCode)

        Log.d(
            TAG,
            "onKeyEvent received: keyCode=$keyCode ($keyName), action=$actionName, deviceId=${event.deviceId}"
        )

        // Record the event with source tag "AccessibilityService"
        eventStore.saveEvent(event, source = "AccessibilityService")

        // Return false so normal volume routing continues unobstructed
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return
            // Ignore system UI overlays like volume sliders, status bar or keyboards
            if (pkg != "com.android.systemui" && !pkg.contains("inputmethod")) {
                Log.d(TAG, "Foreground app changed: $pkg")
                eventStore.setCurrentForegroundPackage(pkg)
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "BudsAccessibilityService interrupted by system")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaSessionManager.setCaptureEnabled(false)
        Log.i(TAG, "BudsAccessibilityService destroyed and MediaSession released")
    }

    companion object {
        private const val TAG = "BudsAccessService"
    }
}
