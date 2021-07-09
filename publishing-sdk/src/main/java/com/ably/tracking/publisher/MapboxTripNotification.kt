package com.ably.tracking.publisher

import android.app.Notification
import androidx.annotation.Keep
import com.mapbox.annotation.module.MapboxModule
import com.mapbox.annotation.module.MapboxModuleType
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.notification.TripNotification

@MapboxModule(MapboxModuleType.NavigationTripNotification, enableConfiguration = true)
@Keep
internal class MapboxTripNotification(private val notificationProvider: PublisherNotificationProvider) : TripNotification {
    override fun getNotification(): Notification = notificationProvider.getNotification()
    override fun getNotificationId(): Int = notificationProvider.getNotificationId()
    override fun onTripSessionStarted() = Unit
    override fun onTripSessionStopped() = Unit
    override fun updateNotification(routeProgress: RouteProgress?) = Unit
}
