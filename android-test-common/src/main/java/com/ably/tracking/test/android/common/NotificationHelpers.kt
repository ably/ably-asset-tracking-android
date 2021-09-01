package com.ably.tracking.test.android.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val NOTIFICATION_CHANNEL_ID = "test-notification-channel"

fun createNotificationChannel(context: Context) {
    // This guard is required by the official Android documentation as the notification
    // channels API is not available in the support library.
    // https://developer.android.com/training/notify-user/channels#CreateChannel
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Publisher Service Channel",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }
}
