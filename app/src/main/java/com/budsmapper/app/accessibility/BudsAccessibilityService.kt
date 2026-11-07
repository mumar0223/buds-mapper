package com.budsmapper.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.media.BudsMediaSessionManager
import com.budsmapper.app.notification.StatusNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class BudsAccessibilityService : AccessibilityService() {

    private lateinit var eventStore: LastEventStore
    private lateinit var mediaSessionManager: BudsMediaSessionManager
    private lateinit var statusNotificationManager: StatusNotificationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastRecordedPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        eventStore = LastEventStore.getInstance(this)
        mediaSessionManager = BudsMediaSessionManager(this)
        statusNotificationManager = StatusNotificationManager(this)

        // Sync MediaSession lifecycle with user preference
        serviceScope.launch {
            eventStore.mediaSessionCaptureEnabled.collect { enabled ->
                Log.i(TAG, "Syncing MediaSession capture state: enabled=$enabled")
                mediaSessionManager.setCaptureEnabled(enabled)
            }
        }

        // Reactively sync notification whenever toggle OR foreground app changes
        serviceScope.launch {
            combine(
                eventStore.statusNotificationEnabled,
                eventStore.currentForegroundPackage,
                eventStore.currentForegroundLabel
            ) { enabled, pkg, label ->
                Triple(enabled, pkg, label)
            }.collect { (enabled, pkg, label) ->
                if (enabled) {
                    statusNotificationManager.updateNotification(label, pkg)
                } else {
                    statusNotificationManager.cancelNotification()
                }
            }
        }

        Log.i(TAG, "BudsAccessibilityService initialized")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "BudsAccessibilityService connected to accessibility subsystem")

        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        // Listen strictly to TYPE_WINDOW_STATE_CHANGED for 0.0% idle CPU
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        serviceInfo = info

        if (eventStore.isMediaSessionCaptureEnabled()) {
            mediaSessionManager.setCaptureEnabled(true)
        }

        // Seed initial foreground package if available, then immediately post notification
        val currentRootPkg = rootInActiveWindow?.packageName?.toString()
        if (!currentRootPkg.isNullOrEmpty() && !isIgnoredPackage(currentRootPkg)) {
            val label = resolveAppLabel(currentRootPkg)
            lastRecordedPackage = currentRootPkg
            eventStore.setCurrentForegroundPackage(currentRootPkg, label)
        } else if (eventStore.isStatusNotificationEnabled()) {
            statusNotificationManager.updateNotification(
                eventStore.currentForegroundLabel.value,
                eventStore.currentForegroundPackage.value
            )
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

        eventStore.saveEvent(event, source = "AccessibilityService")
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val pkg = event.packageName?.toString() ?: return
        if (isIgnoredPackage(pkg)) return

        if (pkg != lastRecordedPackage) {
            lastRecordedPackage = pkg
            val appLabel = resolveAppLabel(pkg)

            Log.i(TAG, "Foreground application switched to: $appLabel ($pkg)")
            eventStore.setCurrentForegroundPackage(pkg, appLabel)
        }
    }

    private fun isIgnoredPackage(pkg: String): Boolean {
        if (pkg.isEmpty()) return true
        return pkg == "com.android.systemui" ||
            pkg.contains("inputmethod") ||
            pkg.contains("honeyboard") || // Samsung Keyboard on Galaxy A35 5G
            pkg == "com.samsung.android.app.cocktailbarservice" || // Samsung Edge Panel
            pkg == "com.sec.android.app.SmartClipEdgeService" ||
            pkg == "android"
    }

    private fun resolveAppLabel(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        } catch (_: Exception) {
            packageName
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "BudsAccessibilityService interrupted by system")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaSessionManager.setCaptureEnabled(false)
        statusNotificationManager.release()
        Log.i(TAG, "BudsAccessibilityService destroyed, MediaSession and Notification released")
    }

    companion object {
        private const val TAG = "BudsAccessService"
    }
}
