package com.budsmapper.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.budsmapper.app.data.LastEventStore

/**
 * BudsAccessibilityService
 *
 * Responsibilities:
 * - Diagnostic interception of global Android KeyEvents via onKeyEvent()
 * - Verifies if Bluetooth AVRCP commands from Samsung Galaxy Buds Core are exposed as KeyEvents
 * - Records events to LastEventStore without blocking the main thread
 * - CRITICAL: Returns false to ensure normal media routing is not consumed or blocked
 */
class BudsAccessibilityService : AccessibilityService() {

    private lateinit var eventStore: LastEventStore

    override fun onCreate() {
        super.onCreate()
        eventStore = LastEventStore.getInstance(this)
        Log.i(TAG, "BudsAccessibilityService initialized")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "BudsAccessibilityService connected to accessibility subsystem")
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        val keyCode = event.keyCode
        val actionName = when (event.action) {
            KeyEvent.ACTION_DOWN -> "ACTION_DOWN"
            KeyEvent.ACTION_UP -> "ACTION_UP"
            KeyEvent.ACTION_MULTIPLE -> "ACTION_MULTIPLE"
            else -> "ACTION_UNKNOWN(${event.action})"
        }
        val keyName = KeyEvent.keyCodeToString(keyCode)

        Log.d(
            TAG,
            "onKeyEvent received: keyCode=$keyCode ($keyName), action=$actionName, deviceId=${event.deviceId}"
        )

        // Record the event for the diagnostic dashboard
        eventStore.saveEvent(event)

        // CRITICAL FOR DIAGNOSTIC PHASE:
        // Return false so normal event routing continues unobstructed through Android's audio/media system.
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: Diagnostic phase does not inspect UI hierarchy or window events
    }

    override fun onInterrupt() {
        Log.w(TAG, "BudsAccessibilityService interrupted by system")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "BudsAccessibilityService destroyed")
    }

    companion object {
        private const val TAG = "BudsAccessService"
    }
}

