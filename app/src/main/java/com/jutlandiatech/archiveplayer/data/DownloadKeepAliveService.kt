package com.jutlandiatech.archiveplayer.data

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.jutlandiatech.archiveplayer.ui.AppTexts

/** Keeps the process alive while [Downloads] has work, so downloads continue after the app is closed (R60). */
object DownloadKeepAlive {
    @Volatile var active = false

    fun start(context: Context) {
        active = true
        runCatching { ContextCompat.startForegroundService(context, Intent(context, DownloadKeepAliveService::class.java)) }
    }
}

class DownloadKeepAliveService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val check = object : Runnable {
        override fun run() {
            if (DownloadKeepAlive.active) {
                handler.postDelayed(this, 2_000)
            } else {
                ServiceCompat.stopForeground(this@DownloadKeepAliveService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val language = SettingsStore(this).load()?.appLanguage ?: "en"
        val texts = AppTexts.load(this).forLanguage(language)
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(texts.text("download.channel", emptyMap())).build()
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(texts.text("download.notification", emptyMap()))
            .setOngoing(true)
            .setSilent(true)
            .build()
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        handler.removeCallbacks(check)
        handler.postDelayed(check, 2_000)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(check)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private companion object {
        const val CHANNEL = "downloads"
        const val NOTIFICATION_ID = 2
    }
}
