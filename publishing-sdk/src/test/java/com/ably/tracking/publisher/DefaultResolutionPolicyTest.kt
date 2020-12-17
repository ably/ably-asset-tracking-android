package com.ably.tracking.publisher

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DefaultResolutionPolicyTest {

    private lateinit var policy: ResolutionPolicy
    private val batteryDataProvider = mockk<BatteryDataProvider>()
    private val defaultResolution = Resolution(Accuracy.MINIMUM, 1000L, 100.0)
    private val hooks = HooksStub()
    private val methods = MethodsStub()

    @Before
    fun beforeEach() {
        mockBatteryLevel(100f)
        policy = DefaultResolutionPolicy(hooks, methods, defaultResolution, batteryDataProvider)
    }

    @Test
    fun `resolving empty request should return the default resolution`() {
        // given
        val emptyRequest = TrackableResolutionRequest(anyTrackable(), emptySet())

        // when
        val resolvedResolution = policy.resolve(emptyRequest)

        // then
        Assert.assertEquals(defaultResolution, resolvedResolution)
    }

    @Test
    fun `resolving a request with trackable but no remote resolutions should return the resolution from trackable`() {
        // given
        val trackableResolution = anyResolution()
        val resolutionRequest = TrackableResolutionRequest(anyTrackable(trackableResolution), emptySet())

        // when
        val resolvedResolution = policy.resolve(resolutionRequest)

        // then
        Assert.assertEquals(trackableResolution, resolvedResolution)
    }

    @Test
    fun `resolving a request with only trackable and battery below the threshold should return the resolution from trackable with desiredInterval changed by multiplier`() {
        // given
        mockBatteryLevel(49f)
        val trackableResolution = Resolution(Accuracy.MAXIMUM, 20L, 0.5)
        val batteryLevelThreshold = 50f
        val lowBatteryMultiplier = 2f
        val resolutionRequest = TrackableResolutionRequest(
            anyTrackable(trackableResolution, batteryLevelThreshold, lowBatteryMultiplier),
            emptySet()
        )

        // when
        val resolvedResolution = policy.resolve(resolutionRequest)

        // then
        val expectedResolution = Resolution(Accuracy.MAXIMUM, 40L, 0.5)
        Assert.assertEquals(expectedResolution, resolvedResolution)
    }

    @Test
    fun `resolving a request with only single remote resolution should return that remote resolution`() {
        // given
        val remoteResolution = Resolution(Accuracy.MAXIMUM, 20L, 0.5)
        val resolutionRequest = TrackableResolutionRequest(anyTrackable(), setOf(remoteResolution))

        // when
        val resolvedResolution = policy.resolve(resolutionRequest)

        // then
        Assert.assertEquals(remoteResolution, resolvedResolution)
    }

    @Test
    fun `resolving a request with multiple remote resolutions should return resolution with best fields combination (highest accuracy, lowest desiredInterval and minimumDisplacement)`() {
        // given
        val remoteResolutions = setOf(
            Resolution(Accuracy.LOW, 14L, 0.1),
            Resolution(Accuracy.BALANCED, 5L, 51.5),
            Resolution(Accuracy.MAXIMUM, 20L, 2.5)
        )
        val resolutionRequest = TrackableResolutionRequest(anyTrackable(), remoteResolutions)

        // when
        val resolvedResolution = policy.resolve(resolutionRequest)

        // then
        val expectedResolution = Resolution(Accuracy.MAXIMUM, 5L, 0.1)
        Assert.assertEquals(expectedResolution, resolvedResolution)
    }

    @Test
    fun `resolving a request with trackable constraints and multiple remote resolutions should return resolution with best fields combination (highest accuracy, lowest desiredInterval and minimumDisplacement)`() {
        // given
        val remoteResolutions = setOf(
            Resolution(Accuracy.LOW, 14L, 0.1),
            Resolution(Accuracy.BALANCED, 5L, 51.5),
            Resolution(Accuracy.MAXIMUM, 20L, 2.5)
        )
        val trackableResolution = Resolution(Accuracy.BALANCED, 1L, 0.5)
        val resolutionRequest = TrackableResolutionRequest(anyTrackable(trackableResolution), remoteResolutions)

        // when
        val resolvedResolution = policy.resolve(resolutionRequest)

        // then
        val expectedResolution = Resolution(Accuracy.MAXIMUM, 1L, 0.1)
        Assert.assertEquals(expectedResolution, resolvedResolution)
    }

    @Test
    fun `resolving a request with trackable constraints and multiple remote resolutions and battery level below threshold should return resolution with best fields combination (highest accuracy, lowest desiredInterval and minimumDisplacement) and modified desiredInterval`() {
        // given
        mockBatteryLevel(49f)
        val trackableResolution = Resolution(Accuracy.BALANCED, 1L, 0.5)
        val batteryLevelThreshold = 50f
        val lowBatteryMultiplier = 10f
        val remoteResolutions = setOf(
            Resolution(Accuracy.LOW, 14L, 0.1),
            Resolution(Accuracy.BALANCED, 5L, 51.5),
            Resolution(Accuracy.MAXIMUM, 20L, 2.5)
        )
        val resolutionRequest = TrackableResolutionRequest(
            anyTrackable(trackableResolution, batteryLevelThreshold, lowBatteryMultiplier),
            remoteResolutions
        )

        // when
        val resolvedResolution = policy.resolve(resolutionRequest)

        // then
        val expectedResolution = Resolution(Accuracy.MAXIMUM, 10L, 0.1)
        Assert.assertEquals(expectedResolution, resolvedResolution)
    }

    @Test
    fun `resolving a request with resolution set should return farWithoutSubscriber when above threshold and no subscriber is present`() {
        // given
        val trackableResolutionSet = anyDefaultResolutionSet()
        val trackable = anyTrackable(trackableResolutionSet)
        val resolutionRequest = TrackableResolutionRequest(trackable, emptySet())

        // when
        val resolvedResolution = policy.resolve(resolutionRequest)

        // then
        Assert.assertEquals(trackableResolutionSet.farWithoutSubscriber, resolvedResolution)
    }

    @Test
    fun `resolving a request with resolution set should return farWithSubscriber when above threshold and at least one subscriber is present`() {
        // given
        val trackableResolutionSet = anyDefaultResolutionSet()
        val trackable = anyTrackable(trackableResolutionSet)
        val resolutionRequest = TrackableResolutionRequest(trackable, emptySet())
        mockAboveThresholdWithSubscribers(trackable)

        // when
        val resolvedResolution = policy.resolve(resolutionRequest)

        // then
        Assert.assertEquals(trackableResolutionSet.farWithSubscriber, resolvedResolution)
    }

    @Test
    fun `resolving a request with resolution set should return nearWithoutSubscriber when below threshold and no subscriber is present`() {
        // given
        val trackableResolutionSet = anyDefaultResolutionSet()
        val trackable = anyTrackable(trackableResolutionSet)
        val resolutionRequest = TrackableResolutionRequest(trackable, emptySet())
        mockBelowThresholdWithoutSubscribers(trackable)

        // when
        val resolvedResolution = policy.resolve(resolutionRequest)

        // then
        Assert.assertEquals(trackableResolutionSet.nearWithoutSubscriber, resolvedResolution)
    }

    @Test
    fun `resolving a request with resolution set should return nearWithSubscriber when below threshold and at least one subscriber is present`() {
        // given
        val trackableResolutionSet = anyDefaultResolutionSet()
        val trackable = anyTrackable(trackableResolutionSet)
        val resolutionRequest = TrackableResolutionRequest(trackable, emptySet())
        mockBelowThresholdWithSubscribers(trackable)

        // when
        val resolvedResolution = policy.resolve(resolutionRequest)

        // then
        Assert.assertEquals(trackableResolutionSet.nearWithSubscriber, resolvedResolution)
    }

    /**
     * [level] should be between [DefaultBatteryDataProvider.MINIMUM_BATTERY_PERCENTAGE] and [DefaultBatteryDataProvider.MAXIMUM_BATTERY_PERCENTAGE]
     */
    private fun mockBatteryLevel(level: Float) {
        every { batteryDataProvider.getCurrentBatteryPercentage() } returns level
    }

    private fun mockAboveThresholdWithSubscribers(trackable: Trackable) {
        val subscriber = Subscriber("some_id", trackable)
        hooks.subscriberSetListener?.onSubscriberAdded(subscriber)
    }

    private fun mockBelowThresholdWithoutSubscribers(trackable: Trackable) {
        hooks.trackableSetListener?.onActiveTrackableChanged(trackable)
        methods.onProximityReached()
    }

    private fun mockBelowThresholdWithSubscribers(trackable: Trackable) {
        val subscriber = Subscriber("some_id", trackable)
        hooks.subscriberSetListener?.onSubscriberAdded(subscriber)
        hooks.trackableSetListener?.onActiveTrackableChanged(trackable)
        methods.onProximityReached()
    }

    private fun anyResolution() = Resolution(Accuracy.BALANCED, 123L, 1.23)

    private fun anyDefaultResolutionSet() = DefaultResolutionSet(
        farWithoutSubscriber = Resolution(Accuracy.LOW, 5000L, 10.0),
        farWithSubscriber = Resolution(Accuracy.BALANCED, 3000L, 6.0),
        nearWithoutSubscriber = Resolution(Accuracy.HIGH, 2500L, 4.5),
        nearWithSubscriber = Resolution(Accuracy.MAXIMUM, 500L, 0.7)
    )

    private fun anyTrackable() = Trackable("test_id")

    private fun anyTrackable(resolution: Resolution) = anyTrackable(DefaultResolutionSet(resolution))

    private fun anyTrackable(resolutionSet: DefaultResolutionSet) = anyTrackable(resolutionSet, 10f, 1f)

    private fun anyTrackable(resolution: Resolution, batteryLevelThreshold: Float, lowBatteryMultiplier: Float) =
        anyTrackable(DefaultResolutionSet(resolution), batteryLevelThreshold, lowBatteryMultiplier)

    private fun anyTrackable(
        resolutionSet: DefaultResolutionSet,
        batteryLevelThreshold: Float,
        lowBatteryMultiplier: Float
    ) = Trackable(
        "test_id",
        constraints = DefaultResolutionConstraints(
            resolutionSet,
            DefaultProximity(1L),
            batteryLevelThreshold,
            lowBatteryMultiplier
        )
    )
}
