package com.ably.tracking.ui.java.animation

import com.ably.tracking.ui.animation.LocationAnimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CoreLocationAnimatorFacade(
    private val locationAnimator: LocationAnimator
) : LocationAnimatorFacade, LocationAnimator by locationAnimator {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun addPositionListener(listener: PositionListener) {
        locationAnimator.positionsFlow
            .onEach { listener.onPositionChanged(it) }
            .launchIn(scope)
    }

    override fun addCameraPositionListener(listener: PositionListener) {
        locationAnimator.cameraPositionsFlow
            .onEach { listener.onPositionChanged(it) }
            .launchIn(scope)
    }
}
