package com.budsmapper.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.budsmapper.app.MainActivity
import com.budsmapper.app.R
import com.budsmapper.app.data.LastEventStore

/**
 * StatusNotificationManager
 *
 * Implements a modern, dark, Spotify-inspired dashboard notification.
 * Strictly classified as an ORDINARY STATUS NOTIFICATION (never MediaStyle).
 *
 * - Uses NotificationCompat.DecoratedCustomViewStyle()
 * - Collapsed: Compact dashboard showing active app & gesture status
 * - Expanded: Detailed status cards, service badges, and quick open button
 * - Zero media player interference (no Play/Pause buttons, no audio focus conflict)
 */
class StatusNotificationManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val eventStore = LastEventStore.getInstance(appContext)

    @Volatile
    private var lastAppLabel: String = "Buds Mapper"

    @Volatile
    private var lastPackageName: String = appContext.packageName

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NOTIFICATION_DISMISSED) {
                if (eventStore.isStatusNotificationEnabled()) {
                    Log.d(TAG, "Notification dismissed; re-pinning status dashboard")
                    updateNotification(lastAppLabel, lastPackageName)
                }
            }
        }
    }

    init {
        createNotificationChannel()
        registerDismissReceiver()
    }

    private fun createNotificationChannel() {
        // Clean up any legacy media channel IDs to remove lingering media controls in SystemUI
        try {
            notificationManager.deleteNotificationChannel("buds_mapper_active_status")
            notificationManager.deleteNotificationChannel("buds_mapper_active_status_v2")
            notificationManager.deleteNotificationChannel("buds_mapper_active_status_v3")
            notificationManager.deleteNotificationChannel("buds_mapper_active_status_v4")
            notificationManager.deleteNotificationChannel("buds_mapper_command_center_v1")
        } catch (_: Exception) {}

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Displays Buds Mapper status dashboard in a polished dark card."
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun registerDismissReceiver() {
        val filter = IntentFilter(ACTION_NOTIFICATION_DISMISSED)
        ContextCompat.registerReceiver(
            appContext,
            dismissReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun updateNotification(appLabel: String, packageName: String) {
        lastAppLabel = appLabel
        lastPackageName = packageName

        val isCaptureActive = eventStore.isMediaSessionCaptureEnabled()
        val appIconBitmap = resolveAppIcon(packageName)

        // 1. Build Intent to open Buds Mapper Dashboard
        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Build Collapsed RemoteViews (~64dp compact card)
        val collapsedViews = RemoteViews(appContext.packageName, R.layout.notification_dashboard_collapsed).apply {
            setTextViewText(R.id.dash_header_title, "Buds Mapper")
            setTextViewText(R.id.dash_badge_active, "ᛒ ACTIVE")
            setTextViewText(R.id.dash_text_current_app, "Currently using $appLabel")
            setTextViewText(
                R.id.dash_text_sub,
                if (isCaptureActive) "Bluetooth gestures are ready" else "Gesture capture paused"
            )
        }

        // 3. Build Expanded RemoteViews (rich dashboard overview)
        val expandedViews = RemoteViews(appContext.packageName, R.layout.notification_dashboard_expanded).apply {
            setTextViewText(R.id.dash_exp_title, "Buds Mapper")
            setTextViewText(R.id.dash_exp_badge, "ᛒ ACTIVE")
            setTextViewText(R.id.dash_exp_app_name, appLabel)
            setTextViewText(R.id.dash_exp_app_sub, "Active in foreground")

            if (appIconBitmap != null) {
                setImageViewBitmap(R.id.dash_exp_app_icon, appIconBitmap)
            } else {
                setImageViewResource(R.id.dash_exp_app_icon, R.drawable.ic_buds_mapper)
            }

            setTextViewText(
                R.id.dash_chip_gestures,
                if (isCaptureActive) "Gestures: Ready" else "Gestures: Paused"
            )
            setTextViewText(R.id.dash_chip_service, "Service: Running")
            setOnClickPendingIntent(R.id.dash_btn_open, contentPendingIntent)
        }

        // 4. Delete Intent for self-healing persistence across One UI "Clear All"
        val deleteIntent = Intent(ACTION_NOTIFICATION_DISMISSED).apply {
            setPackage(appContext.packageName)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            appContext,
            1,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 5. Construct Ordinary Notification with DecoratedCustomViewStyle (STRICTLY NON-MEDIA)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_buds_mapper)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setContentTitle("Buds Mapper")
            .setContentText("Currently using $appLabel")
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Dashboard notification updated for app: $appLabel ($packageName)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post dashboard notification", e)
        }
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        Log.i(TAG, "Dashboard notification cancelled")
    }

    fun release() {
        try {
            appContext.unregisterReceiver(dismissReceiver)
        } catch (_: IllegalArgumentException) {}
        cancelNotification()
    }

    private fun resolveAppIcon(packageName: String): Bitmap? {
        return try {
            val drawable: Drawable = appContext.packageManager.getApplicationIcon(packageName)
            drawableToRoundedBitmap(drawable)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve icon for $packageName", e)
            null
        }
    }

    private fun drawableToRoundedBitmap(drawable: Drawable): Bitmap {
        val size = 96
        val srcBitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val raw = drawable.bitmap
            // Convert HARDWARE bitmaps to ARGB_8888 for safe software Canvas rendering on One UI
            val softwareBmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && raw.config == Bitmap.Config.HARDWARE) {
                raw.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                raw
            }
            Bitmap.createScaledBitmap(softwareBmp, size, size, true)
        } else {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bmp
        }

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val cornerRadius = 20f

        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(srcBitmap, 0f, 0f, paint)

        return output
    }

    companion object {
        private const val TAG = "StatusNotifMgr"
        private const val CHANNEL_ID = "buds_mapper_status_channel_v1"
        private const val CHANNEL_NAME = "Buds Mapper Status"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_NOTIFICATION_DISMISSED = "com.budsmapper.app.ACTION_NOTIFICATION_DISMISSED"
    }
}
