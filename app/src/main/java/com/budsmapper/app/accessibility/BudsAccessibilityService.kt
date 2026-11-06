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
 * - Diagnostic interception of global Android KeyEvents via onKeyEvent()
 * - Hosts the native MediaSession diagnostic receiver to ensure background longevity
 * - Records events to LastEventStore without blocking the main thread
 * - CRITICAL: Returns false from onKeyEvent() so normal system key events are not blocked
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
        serviceInfo = info

        // Check if capture mode was already enabled
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

        // CRITICAL: Return false so normal event routing continues unobstructed
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op for diagnostic phase
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
