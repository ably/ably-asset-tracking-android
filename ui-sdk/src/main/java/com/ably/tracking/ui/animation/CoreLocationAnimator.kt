package com.ably.tracking.ui.animation

import android.os.SystemClock
import android.view.animation.LinearInterpolator
import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

private const val DEFAULT_INTENTIONAL_ANIMATION_DELAY_IN_MILLISECONDS: Long = 2_000L
private const val DEFAULT_ANIMATION_STEPS_BETWEEN_CAMERA_UPDATES: Int = 1
private const val IDLE_ANIMATION_LOOP_DELAY_IN_MILLISECONDS: Long = 50L
private const val SINGLE_ANIMATION_FRAME_INTERVAL_IN_MILLISECONDS: Long = (1000 / 60.0).toLong() // 60 FPS
private const val ANIMATION_REQUESTS_BUFFER_SIZE = 20
private const val POSITIONS_BUFFER_SIZE = 10
private const val CAMERA_POSITIONS_BUFFER_SIZE = 10
private const val UNKNOWN_DURATION: Long = -1L

class CoreLocationAnimator(
    /**
     * A constant delay added to the animation duration. It helps to smooth out movement
     * when we receive a location update later than we've expected.
     */
    private val intentionalAnimationDelayInMilliseconds: Long = DEFAULT_INTENTIONAL_ANIMATION_DELAY_IN_MILLISECONDS,
    /**
     * How often should the camera updates be sent.
     */
    private val animationStepsBetweenCameraUpdates: Int = DEFAULT_ANIMATION_STEPS_BETWEEN_CAMERA_UPDATES,
) : LocationAnimator {
    override val positionsFlow: Flow<Position>
        get() = _positionsFlow
    override val cameraPositionsFlow: Flow<Position>
        get() = _cameraPositionsFlow

    private val _positionsFlow = MutableSharedFlow<Position>(replay = 1, extraBufferCapacity = POSITIONS_BUFFER_SIZE)
    private val _cameraPositionsFlow =
        MutableSharedFlow<Position>(replay = 1, extraBufferCapacity = CAMERA_POSITIONS_BUFFER_SIZE)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var animationRequestsChannel: Channel<AnimationRequest> = Channel(ANIMATION_REQUESTS_BUFFER_SIZE)
    private val animationSteps: MutableList<AnimationStep> = mutableListOf()
    private var previousFinalPosition: Position? = null
    private var animationStepsCounter: Int = 0

    init {
        scope.launch {
            for (request in animationRequestsChannel) {
                processAnimationRequest(request)
            }
        }
        scope.launch {
            animationLoop()
        }
    }

    override fun animateLocationUpdate(
        locationUpdate: LocationUpdate,
        expectedIntervalBetweenLocationUpdatesInMilliseconds: Long,
    ) {
        scope.launch {
            animationRequestsChannel.send(
                AnimationRequest(locationUpdate, expectedIntervalBetweenLocationUpdatesInMilliseconds)
            )
        }
    }

    override fun stop() {
        scope.cancel()
    }

    private fun processAnimationRequest(request: AnimationRequest) {
        val steps = createAnimationStepsFromRequest(request)
        val expectedAnimationDurationInMilliseconds =
            intentionalAnimationDelayInMilliseconds + request.expectedIntervalBetweenLocationUpdatesInMilliseconds
        synchronized(animationSteps) {
            animationSteps.addAll(steps)
            val animationStepDurationInMilliseconds = expectedAnimationDurationInMilliseconds / animationSteps.size
            animationSteps.forEach { it.durationInMilliseconds = animationStepDurationInMilliseconds }
            previousFinalPosition = animationSteps.last().endPosition
        }
    }

    private fun createAnimationStepsFromRequest(request: AnimationRequest): List<AnimationStep> {
        val requestPositions = (request.locationUpdate.skippedLocations + request.locationUpdate.location)
            .map { it.toPosition() }
        return requestPositions.foldIndexed(mutableListOf()) { index, steps, position ->
            val startPosition =
                if (index == 0) getNewAnimationStartingPosition(request.locationUpdate)
                else requestPositions[index - 1]
            steps.add(AnimationStep(startPosition, position, UNKNOWN_DURATION))
            steps
        }
    }

    private fun getNewAnimationStartingPosition(locationUpdate: LocationUpdate): Position =
        previousFinalPosition
            ?: locationUpdate.skippedLocations.firstOrNull()?.toPosition()
            ?: locationUpdate.location.toPosition()

    private suspend fun animationLoop() {
        while (true) {
            if (animationSteps.isNotEmpty()) {
                val currentStep = synchronized(animationSteps) { animationSteps.removeFirst() }
                animateStep(currentStep)
            } else {
                delay(IDLE_ANIMATION_LOOP_DELAY_IN_MILLISECONDS)
            }
        }
    }

    private suspend fun animateStep(animationStep: AnimationStep) {
        val interpolator = LinearInterpolator()
        val startTimeInMillis = SystemClock.uptimeMillis()

        var timeElapsedFromStartInMilliseconds: Long
        var timeProgressPercentage = 0f
        var distanceProgressPercentage: Float

        animationStepsCounter++
        if (animationStepsCounter >= animationStepsBetweenCameraUpdates) {
            _cameraPositionsFlow.emit(animationStep.endPosition)
            animationStepsCounter = 0
        }

        while (timeProgressPercentage < 1) {
            timeElapsedFromStartInMilliseconds = SystemClock.uptimeMillis() - startTimeInMillis
            timeProgressPercentage = timeElapsedFromStartInMilliseconds / animationStep.durationInMilliseconds.toFloat()
            distanceProgressPercentage = interpolator.getInterpolation(timeProgressPercentage)
            _positionsFlow.emit(
                interpolateLinear(distanceProgressPercentage, animationStep.startPosition, animationStep.endPosition)
            )
            delay(SINGLE_ANIMATION_FRAME_INTERVAL_IN_MILLISECONDS)
        }
    }

    private fun interpolateLinear(fraction: Float, first: Position, second: Position): Position {
        val latitude = interpolateLinear(fraction, first.latitude, second.latitude)
        val longitude = interpolateLinear(fraction, first.longitude, second.longitude)
        val accuracy = interpolateLinear(fraction, first.accuracy, second.accuracy)
        return Position(latitude, longitude, second.bearing, accuracy)
    }

    private fun interpolateLinear(fraction: Float, a: Double, b: Double): Double = (b - a) * fraction + a

    private fun interpolateLinear(fraction: Float, a: Float, b: Float): Float = (b - a) * fraction + a

    private fun Location.toPosition() = Position(latitude, longitude, bearing, accuracy)

    private data class AnimationRequest(
        val locationUpdate: LocationUpdate,
        val expectedIntervalBetweenLocationUpdatesInMilliseconds: Long,
    )

    private data class AnimationStep(
        val startPosition: Position,
        val endPosition: Position,
        var durationInMilliseconds: Long,
    )
}
