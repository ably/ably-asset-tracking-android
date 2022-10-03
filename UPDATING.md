# Upgrade / Migration Guide

## Version 1.3.0 to 1.4.0

### Token based auth configuration

The preferred way of using token based auth has changed. Now, the `clientId` is not required when using either `tokenRequest` or `jwt` auth.
The client ID will be inferred from the token provided to the AAT SDK from the auth token callback.

```kotlin
Authentication.tokenRequest {
    // return a token request (AAT will use its client ID)
}

Authentication.jwt {
    // return a JWT token (AAT will use its client ID)
}
```

The old methods that required a `clientId` have been deprecated and will be removed in a future release.

## Version 1.1.1 to 1.2.0

### Update the Ably Asset Tracking dependency

Update the versions of Ably Asset Tracking dependency (or dependencies) you're using to `1.2.0`.

### Connection configuration enhancements

The option to specify a non-default Ably environment such as 'sandbox' was added to the `ConnectionConfiguration`.

```kotlin
ConnectionConfiguration(
    authentication = Authentication.basic(CLIENT_ID, ABLY_API_KEY),
    environment = "custom-environment",
)
```

## Version 1.1.0 to 1.1.1

### Update the Ably Asset Tracking dependency

Update the versions of Ably Asset Tracking dependency (or dependencies) you're using to `1.1.1`.

### Publisher builder adjustments

Because AAT works best when the predictions are disabled we decided to remove the option to enable predictions.
Since this version the predictions are always disabled and the method `predictions()` was removed from the publisher builder.

## Version 1.0.0 to 1.1.0

### Update the Ably Asset Tracking dependency

Update the versions of Ably Asset Tracking dependency (or dependencies) you're using to `1.1.0`.

### Publisher builder adjustments

It is not required, but highly advisable, to turn off the location predictions for the publisher. This can be done through the publisher builder by calling `predictions(false)`.

If you would like to use a slower sending resolution for the trackables but keep the high accuracy, we advise to set a constant location engine resolution to a fast value. This can be done through the publisher builder with the `constantLocationEngineResolution(Resolution(Accuracy.HIGH, 1000L, 1.0))` method call (you can of course adjust the resolution to your needs).

In order to be able to animate smoothly the publisher position on the subscriber side, we are sending the publisher's calculated resolution to the subscribers. This was available before as a debug feature and was disabled by default. Now, it is enabled by default, so if you are calling publisher builder with `sendResolution(false)` then you should remove that call.

### Animation module

In version 1.1.0 we've added a new module that aims to improve the experience of Ably Asset Tracking users on the subscriber side. For now, the module contains an extension that takes care of creating a smooth animations of the location updates received from the publisher. In order to use those features you first have to add the new dependency to your `build.gradle` file:

```groovy
dependencies {
    implementation 'com.ably.tracking:ui-sdk:1.1.0'
}
```

Now you should have access to the `LocationAnimator`, `CoreLocationAnimator` and `Position` classes.

To use the animation logic you need to create a new instance of the `CoreLocationAnimator` and use its shared flows to receive both map marker and map camera location updates.
The `CoreLocationAnimator` class takes the following configuration options:

- `intentionalAnimationDelayInMilliseconds` - A constant delay added to the animation duration. This helps to smooth out movement when a location update is received later than expected due to network latency.
  The higher value the less realtime the animation becomes. The default value is 2000 milliseconds.
- `animationStepsBetweenCameraUpdates` - How many map marker steps need to be animated before a camera update is sent.
  One step is created for each location from a location update, so this can be thought of as the number of locations between camera updates.
  Setting a higher value might improve UX but, if set too high, may cause the map marker to move out of the screen. The default value is 1.

```kotlin
val locationAnimator: LocationAnimator = CoreLocationAnimator(
  intentionalAnimationDelayInMilliseconds,
  animationStepsBetweenCameraUpdates,
)
```

The animator exposes two flows, one for map marker (`positionsFlow`) and the other for camera updates (`cameraPositionsFlow`).
The map marker flow emits 60 positions per second, so you should simply update the marker's position each time the flow emits a new position.
The camera flow emits 1 position when a new camera update is ready, so you can either move the camera there or animate its movement by yourself.

```kotlin
// 60 positions per second
locationAnimator.positionsFlow
    .onEach { mapMarkerPosition -> setMapMarkerPosition(mapMarkerPosition) }
    .launchIn(scope)

// 1 position when a camera update is ready
locationAnimator.cameraPositionsFlow
    .onEach { cameraPosition -> moveCamera(cameraPosition) }
    .launchIn(scope)
```

The last step is to notify the location animator each time a new location update from the subscriber arrives. You also need to provide the expected interval of location updates in milliseconds which since 1.1.0 can be received from the subscriber.

```kotlin
var locationUpdateIntervalInMilliseconds = 0L

subscriber.nextLocationUpdateIntervals
    .onEach { locationUpdateIntervalInMilliseconds = it }
    .launchIn(scope)

subscriber.locations
    .onEach { locationAnimator.animateLocationUpdate(it, locationUpdateIntervalInMilliseconds) }
    .launchIn(scope)
```

If you won't need the location animator anymore you should call its `stop()` method which will cancel and stop any ongoing animation. Please note that once you stop a `LocationAnimator` instance you can no longer use it and you should create a new instance to continue animation.
