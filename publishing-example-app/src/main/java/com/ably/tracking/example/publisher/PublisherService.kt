package com.ably.tracking.example.publisher

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ably.tracking.publisher.Publisher

class PublisherService : Service() {
    private val NOTIFICATION_ID = 5235
    private val binder = Binder()
    var publisher: Publisher? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Asset Tracking")
            .setContentText("Publisher is working")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    inner class Binder : android.os.Binder() {
        fun getService(): PublisherService = this@PublisherService
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
