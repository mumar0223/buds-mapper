package com.budsmapper.app.notification

import android.app.Notification
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
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.budsmapper.app.MainActivity
import com.budsmapper.app.R
import com.budsmapper.app.data.LastEventStore

class StatusNotificationManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val eventStore = LastEventStore.getInstance(appContext)
    private val backgroundArtwork: Bitmap = generateAestheticBackground()

    @Volatile
    private var lastAppLabel: String = "Buds Mapper"

    @Volatile
    private var lastPackageName: String = appContext.packageName

    // Event-driven self-healing receiver: if Android 14+ / One UI "Clear All" dismisses
    // the ongoing notification, immediately re-pin it with 0.0% idle CPU overhead.
    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NOTIFICATION_DISMISSED) {
                if (eventStore.isStatusNotificationEnabled()) {
                    Log.d(TAG, "Notification dismissed by system/user; re-pinning compact card")
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
        // Clean up legacy channel IDs that may have cached error states in One UI
        try {
            notificationManager.deleteNotificationChannel("buds_mapper_active_status")
            notificationManager.deleteNotificationChannel("buds_mapper_active_status_v2")
            notificationManager.deleteNotificationChannel("buds_mapper_active_status_v3")
        } catch (_: Exception) {}

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Displays the currently active foreground application in a compact Spotify-styled card."
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
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

        val appIconBitmap = resolveAppIcon(packageName)

        val remoteViews = RemoteViews(appContext.packageName, R.layout.notification_compact_spotify).apply {
            setImageViewBitmap(R.id.notification_bg, backgroundArtwork)
            setTextViewText(R.id.notification_title, "Buds Mapper")
            setTextViewText(R.id.notification_subtitle, appLabel)
            if (appIconBitmap != null) {
                setImageViewBitmap(R.id.notification_app_icon, appIconBitmap)
            } else {
                setImageViewResource(R.id.notification_app_icon, R.drawable.ic_buds_mapper)
            }
        }

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(ACTION_NOTIFICATION_DISMISSED).apply {
            setPackage(appContext.packageName)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            appContext,
            1,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_buds_mapper)
            .setCustomContentView(remoteViews)
            .setContentTitle("Buds Mapper")
            .setContentText(appLabel)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            Log.d(TAG, "Status notification updated for app: $appLabel ($packageName)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post status notification", e)
        }
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        Log.i(TAG, "Status notification cancelled")
    }

    fun release() {
        try {
            appContext.unregisterReceiver(dismissReceiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered
        }
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
            // Convert HARDWARE bitmaps to ARGB_8888 so software Canvas never crashes on One UI
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

    /**
     * Generates a compact 600x128 dark ambient artwork image with built-in contrast scrim.
     */
    private fun generateAestheticBackground(): Bitmap {
        val width = 600
        val height = 128
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Deep cosmic gradient base
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#121424"),
                    Color.parseColor("#1B1A3A"),
                    Color.parseColor("#10223A"),
                    Color.parseColor("#0C101E")
                ),
                floatArrayOf(0f, 0.35f, 0.75f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), basePaint)

        // 2. Subtle ambient glow orbs
        val orbPaint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.15f, height * 0.4f, 100f,
                Color.parseColor("#4D5B86E5"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.15f, height * 0.4f, 100f, orbPaint1)

        val orbPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width * 0.85f, height * 0.6f, 110f,
                Color.parseColor("#4436D1DC"),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(width * 0.85f, height * 0.6f, 110f, orbPaint2)

        return bitmap
    }

    companion object {
        private const val TAG = "StatusNotifMgr"
        private const val CHANNEL_ID = "buds_mapper_active_status_v4"
        private const val CHANNEL_NAME = "Buds Mapper Status"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_NOTIFICATION_DISMISSED = "com.budsmapper.app.ACTION_NOTIFICATION_DISMISSED"
    }
}
