package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import androidx.annotation.RequiresPermission
import com.ably.tracking.BuilderConfigurationIncompleteException
import com.ably.tracking.ConnectionException
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents a publisher. Publishers maintain the Ably connection, making use of navigation resources as required to
 * track [Trackable] objects.
 */
interface Publisher {
    companion object Factory {
        /**
         * Returns the default state of the publisher [Builder], which is incapable of starting of [Publisher]
         * instances until it has been configured fully.
         */
        @JvmStatic
        fun publishers(): Builder {
            return PublisherBuilder()
        }
    }

    /**
     * Adds a [Trackable] object and makes it the actively tracked object, meaning that the state of the [active] field
     * will be updated to this object, if that wasn't already the case.
     *
     * If this object was already in this publisher's tracked set then this method only serves to change the actively
     * tracked object.
     *
     * @param trackable The object to be added to this publisher's tracked set, if it's not already there, and to be
     * made the actively tracked object.
     * @return [StateFlow] that represents the [TrackableState] of the added [Trackable].
     *
     * @throws ConnectionException when something goes wrong with the Ably connection
     */
    @JvmSynthetic
    suspend fun track(trackable: Trackable): StateFlow<TrackableState>

    /**
     * Adds a [Trackable] object, but does not make it the actively tracked object, meaning that the state of the
     * [active] field will not change.
     *
     * If this object was already in this publisher's tracked set then this method does nothing.
     *
     * @param trackable The object to be added to this publisher's tracked set, if it's not already there.
     * @return [StateFlow] that represents the [TrackableState] of the added [Trackable].
     *
     * @throws ConnectionException when something goes wrong with the Ably connection
     */
    @JvmSynthetic
    suspend fun add(trackable: Trackable): StateFlow<TrackableState>

    /**
     * Removes a [Trackable] object if it is known to this publisher, otherwise does nothing and returns false.
     *
     * If the removed object is the current actively [active] object then that state will be cleared, meaning that for
     * another object to become the actively tracked delivery then the [track] method must be subsequently called.
     *
     * @param trackable The object to be removed from this publisher's tracked set, it it's there.
     *
     * @return `true` if the object was known to this publisher, otherise `false`.
     * @throws ConnectionException when something goes wrong with the Ably connection
     */
    @JvmSynthetic
    suspend fun remove(trackable: Trackable): Boolean

    /**
     * The actively tracked object, being the [Trackable] object whose destination will be used for location
     * enhancement, if available.
     *
     * This state can be changed by calling the [track] method.
     */
    val active: Trackable?

    /**
     * The active means of transport for this publisher.
     */
    var routingProfile: RoutingProfile

    /**
     * The shared flow emitting location values when they become available.
     */
    val locations: SharedFlow<LocationUpdate>
        @JvmSynthetic get

    /**
     * The shared flow emitting all trackables tracked by the publisher.
     */
    val trackables: SharedFlow<Set<Trackable>>
        @JvmSynthetic get

    /**
     * The shared flow emitting trip location history when it becomes available.
     */
    val locationHistory: SharedFlow<LocationHistoryData>
        @JvmSynthetic get

    /**
     * Returns a trackable state flow representing the [TrackableState] for an already added [Trackable].
     *
     * @param trackableId The ID of an already added trackable.
     * @return [StateFlow] that represents the [TrackableState] of the added [Trackable]. If the trackable doesn't exist it returns null.
     */
    @JvmSynthetic
    fun getTrackableState(trackableId: String): StateFlow<TrackableState>?

    /**
     * Stops this publisher from publishing locations. Once a publisher has been stopped, it cannot be restarted.
     * Please note that calling this method will remove the notification provided by [Builder.backgroundTrackingNotificationProvider].
     *
     * @param timeoutInMilliseconds After this duration the stopping procedure will be canceled. Default value is 30 seconds.
     *
     * @throws ConnectionException If something goes wrong during connection closing
     * @throws TimeoutCancellationException If the operation does not complete in the [timeoutInMilliseconds] time
     */
    @JvmSynthetic
    suspend fun stop(timeoutInMilliseconds: Long = 30_000L)

    /**
     * The methods implemented by builders capable of starting [Publisher] instances.
     *
     * All methods except [start] return a new [Builder] instance, being a copy of this instance but with the
     * relevant property mutated.
     *
     * The starting point is always the default builder state, returned by the static [publishers] method.
     */
    interface Builder {
        /**
         * **REQUIRED** Sets the Ably connection configuration.
         *
         * @param configuration The configuration to be used for Ably connection.
         * @return A new instance of the builder with this property changed.
         */
        fun connection(configuration: ConnectionConfiguration): Builder

        /**
         * **REQUIRED** Sets the maps configuration.
         *
         * @param configuration The configuration to be used for maps.
         * @return A new instance of the builder with this property changed.
         */
        fun map(configuration: MapConfiguration): Builder

        /**
         * **REQUIRED** Sets the Android Context.
         *
         * @param context The context of the application.
         * @return A new instance of the builder with this property changed.
         */
        fun androidContext(context: Context): Builder

        /**
         * **OPTIONAL** Set the means of transport being used for the initial state of publishers created from this builder.
         * If not set then the default value is [RoutingProfile.DRIVING].
         *
         * @param profile The means of transport.
         * @return A new instance of the builder with this property changed.
         */
        fun profile(profile: RoutingProfile): Builder

        /**
         * **REQUIRED** Sets the policy factory to be used to define the target resolution for publishers created from this builder.
         *
         * @param factory The factory, whose [createResolutionPolicy][ResolutionPolicy.Factory.createResolutionPolicy]
         * method will be called exactly once when [start] is called.
         * @return A new instance of the builder with this property changed.
         */
        fun resolutionPolicy(factory: ResolutionPolicy.Factory): Builder

        /**
         * **OPTIONAL** Sets the location source to be used instead of the GPS.
         * The location source will be providing location updates for the [Publisher].
         *
         * @param locationSource The location source from which location updates will be received.
         * @return A new instance of the builder with this property changed.
         */
        fun locationSource(locationSource: LocationSource?): Builder

        /**
         * EXPERIMENTAL API
         * **OPTIONAL** Sets the log handler.
         *
         * @param logHandler The class that will handle log messages.
         * @return A new instance of the builder with this property changed.
         */
        fun logHandler(logHandler: LogHandler): Builder

        /**
         * **REQUIRED** Sets the notification that will be displayed for the background tracking service.
         * Please note that this notification will be removed when you call the [stop] method.
         *
         * @param notificationProvider It will be used to create the notification.
         * @param notificationId The ID of the notification used by the Android OS to display it.
         * @return A new instance of the builder with this property changed.
         */
        fun backgroundTrackingNotificationProvider(
            notificationProvider: PublisherNotificationProvider,
            notificationId: Int
        ): Builder

        /**
         * EXPERIMENTAL API
         * **OPTIONAL** Enables sending of raw location updates. This should only be enabled for diagnostics.
         * In the production environment this should be always disabled.
         * By default this is disabled.
         *
         * @param enabled Whether the sending of raw location updates is enabled.
         * @return A new instance of the builder with this property changed.
         */
        fun rawLocations(enabled: Boolean): Builder

        /**
         * **OPTIONAL** Enables sending of calculated resolutions.
         * By default this is enabled.
         *
         * @param enabled Whether the sending of calculated resolutions is enabled.
         * @return A new instance of the builder with this property changed.
         */
        fun sendResolution(enabled: Boolean): Builder

        /**
         * **OPTIONAL** Enables using predictions for enhanced location updates.
         * By default this is enabled.
         *
         * @param enabled Whether the predictions for enhanced locations are enabled.
         * @return A new instance of the builder with this property changed.
         */
        fun predictions(enabled: Boolean): Builder

        /**
         * EXPERIMENTAL API
         * **OPTIONAL** Specifies a callback that will be called with the filepath of raw history data from the Navigation SDK component.
         * This will be probably removed in the future. Do not use this in the production environment.
         *
         * @param callback The callback that will be called with the raw history data filepath.
         * @return A new instance of the builder with this property changed.
         */
        fun rawHistoryDataCallback(callback: (String) -> Unit): Builder

        /**
         * **OPTIONAL** Enables using a constant location engine resolution.
         * If the [resolution] is not null then instead of using [ResolutionPolicy] to calculate a dynamic resolution for the location engine
         * the [resolution] will be used as the location engine resolution.
         * If the [resolution] is null then the constant resolution is disabled and the location engine resolution will be calculated by the [ResolutionPolicy].
         * By default this is disabled.
         *
         * @param resolution The resolution that will be used as the location engine resolution.
         * @return A new instance of the builder with this property changed.
         */
        fun constantLocationEngineResolution(resolution: Resolution?): Builder

        /**
         * **OPTIONAL** Set the type of vehicle being used by the publisher user.
         * If not set then the default value is [VehicleProfile.CAR].
         *
         * @param profile The type of vehicle.
         * @return A new instance of the builder with this property changed.
         */
        fun vehicleProfile(profile: VehicleProfile): Builder

        /**
         * Creates a [Publisher] and starts publishing.
         *
         * The returned publisher instance does not start in a state whereby it is actively tracking anything. If
         * tracking is required from the outset then the [track][Publisher.track] method must be subsequently called.
         *
         * In order to detect device's location ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission must be granted.
         *
         * @return A new publisher instance.
         * @throws com.ably.tracking.BuilderConfigurationIncompleteException If all required params aren't set
         * @throws ConnectionException If something goes wrong during connection initialization
         */
        @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
        @Throws(BuilderConfigurationIncompleteException::class, ConnectionException::class)
        fun start(): Publisher
    }
}
