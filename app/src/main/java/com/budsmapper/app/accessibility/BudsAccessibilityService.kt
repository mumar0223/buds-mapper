package com.budsmapper.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.pm.PackageManager
import android.graphics.Path
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.budsmapper.app.data.AppProfileStore
import com.budsmapper.app.data.LastEventStore
import com.budsmapper.app.data.MappedAction
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
        instance = this
        eventStore = LastEventStore.getInstance(this)
        mediaSessionManager = BudsMediaSessionManager(this)
        statusNotificationManager = StatusNotificationManager(this)

        val profileStore = AppProfileStore.getInstance(this)

        // Smart Auto-Engagement:
        // Dynamically activates MediaSession capture ONLY when Master Switch is ON AND
        // an application with an ENABLED profile is in the foreground.
        // Automatically releases immediately when Master is OFF or switching to unmapped apps!
        serviceScope.launch {
            combine(
                eventStore.isMasterEnabled,
                eventStore.currentForegroundPackage,
                profileStore.enabledPackages
            ) { isMasterEnabled, pkg, enabledSet ->
                isMasterEnabled && enabledSet.contains(pkg)
            }.collect { isEnabledForForegroundApp ->
                Log.i(
                    TAG,
                    "Dynamic capture state changed: isEnabled=$isEnabledForForegroundApp (master=${eventStore.isMasterEnabled.value}) for foreground pkg: ${eventStore.currentForegroundPackage.value}"
                )
                mediaSessionManager.setCaptureEnabled(isEnabledForForegroundApp)
                eventStore.setMediaSessionCaptureEnabled(isEnabledForForegroundApp)
            }
        }

        // Reactively sync notification whenever toggle, foreground app, or app profile changes
        serviceScope.launch {
            combine(
                eventStore.statusNotificationEnabled,
                eventStore.currentForegroundPackage,
                eventStore.currentForegroundLabel,
                profileStore.enabledPackages
            ) { enabled, pkg, label, _ ->
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

    /**
     * Executes the mapped touch action on the active application screen.
     * Returns true if the gesture was consumed, or false if it should pass through (e.g. DEFAULT).
     */
    fun executeAction(action: MappedAction): Boolean {
        if (!eventStore.isMasterEnabled.value) {
            Log.i(TAG, "Master service is disabled, ignoring action ${action.displayName}")
            return false
        }
        Log.i(TAG, "executeAction requested: ${action.displayName} (${action.id})")
        return when (action) {
            MappedAction.SWIPE_UP -> {
                performSwipeUp()
                true
            }
            MappedAction.SWIPE_DOWN -> {
                performSwipeDown()
                true
            }
            MappedAction.SCROLL_DOWN -> {
                performScrollDown()
                true
            }
            MappedAction.SCROLL_UP -> {
                performScrollUp()
                true
            }
            MappedAction.PLAY_PAUSE -> {
                performCenterTap()
                true
            }
            MappedAction.NONE -> {
                true // Consumed silently
            }
            MappedAction.DEFAULT -> {
                false // Pass-through to native media player
            }
        }
    }

    fun performSwipeUp(durationMs: Long = 200L) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        val centerX = width / 2f
        val startY = height * 0.72f
        val endY = height * 0.28f

        dispatchSwipe(centerX, startY, centerX, endY, durationMs, "Swipe Up (Next Video)")
    }

    fun performSwipeDown(durationMs: Long = 200L) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        val centerX = width / 2f
        val startY = height * 0.28f
        val endY = height * 0.72f

        dispatchSwipe(centerX, startY, centerX, endY, durationMs, "Swipe Down (Previous Video)")
    }

    fun performScrollDown(durationMs: Long = 250L) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        val centerX = width / 2f
        val startY = height * 0.60f
        val endY = height * 0.40f

        dispatchSwipe(centerX, startY, centerX, endY, durationMs, "Scroll Down")
    }

    fun performScrollUp(durationMs: Long = 250L) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        val centerX = width / 2f
        val startY = height * 0.40f
        val endY = height * 0.60f

        dispatchSwipe(centerX, startY, centerX, endY, durationMs, "Scroll Up")
    }

    fun performCenterTap() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        val centerX = width / 2f
        val centerY = height / 2f

        dispatchTap(centerX, centerY)
    }

    private fun dispatchSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long,
        gestureName: String
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        val result = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.i(TAG, "Touch gesture $gestureName completed successfully on screen")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Touch gesture $gestureName cancelled by system")
            }
        }, null)

        Log.d(TAG, "dispatchGesture($gestureName): from ($startX, $startY) to ($endX, $endY), dispatched=$result")
    }

    private fun dispatchTap(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke)
            .build()
        dispatchGesture(gesture, null, null)
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
        instance = null
        serviceScope.cancel()
        mediaSessionManager.setCaptureEnabled(false)
        statusNotificationManager.release()
        Log.i(TAG, "BudsAccessibilityService destroyed, MediaSession and Notification released")
    }

    companion object {
        private const val TAG = "BudsAccessService"

        @Volatile
        var instance: BudsAccessibilityService? = null
            private set
    }
}
