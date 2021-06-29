package com.ably.tracking.publisher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import com.mapbox.annotation.module.MapboxModule
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.TripNotification

@MapboxModule(MapboxModuleType.NavigationTripNotification, enableConfiguration = true)
@Keep
internal class MapboxTripNotification(private val notification: AssetTrackingNotification) : TripNotification {
    override fun getNotification(): Notification = notification.getNotification()
    override fun getNotificationId(): Int = notification.getNotificationId()
    override fun onTripSessionStarted() = Unit
    override fun onTripSessionStopped() = Unit
    override fun updateNotification(routeProgress: RouteProgress?) = Unit
}

internal class DefaultMapboxTripNotification(context: Context) : TripNotification {
    private val NOTIFICATION_CHANNEL_ID = "DefaultTripNotificationChannelId"
    private val NOTIFICATION_ID = 819203
    private val notification: Notification

    init {
        createNotificationChannel(context)
        notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Ably Asset Tracking")
            .setContentText("Trip is started")
            .setSmallIcon(R.drawable.aat_logo)
            .build()
    }

    override fun getNotification(): Notification = notification
    override fun getNotificationId(): Int = NOTIFICATION_ID
    override fun onTripSessionStarted() = Unit
    override fun onTripSessionStopped() = Unit
    override fun updateNotification(routeProgress: RouteProgress?) = Unit

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).apply {
                // It is safe to call createNotificationChannel multiple times
                // https://developer.android.com/training/notify-user/channels#CreateChannel
                createNotificationChannel(
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        "AAT Publisher Channel",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }
}
