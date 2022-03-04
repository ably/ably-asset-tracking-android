# Upgrade / Migration Guide

## Version 1.0.0 to 1.1.0

### Update the Ably Asset Tracking dependency

Update the versions of Ably Asset Tracking dependency (or dependencies) you're using to `1.1.0-rc.2`.

### Use the Mapbox snapshot repository

In Ably Asset Tracking version 1.1.0 we are using a special version of the navigation profile which requires us to use the Mapbox snapshot repository. In order to do that you need to add this repository in you `build.gradle` file (preferably next to the main Mapbox repository declaration):

```groovy
repositories {
    // Mapbox Snapshot repository required to get the new asset tracking navigation profile.
    maven {
        url 'https://api.mapbox.com/downloads/v2/snapshots/maven'
        authentication {
            basic(BasicAuthentication)
        }
        credentials {
            username = 'mapbox'
            password = property('MAPBOX_DOWNLOADS_TOKEN')
        }
    }
}
```

### Publisher builder adjustments

It is not required, but highly advisable, to turn off the location predictions for the publisher. This can be done through the publisher builder by calling `predictions(false)`.

If you would like to use a slower sending resolution for the trackables but keep the high accuracy, we advise to set a constant location engine resolution to a fast value. This can be done through the publisher builder with the `constantLocationEngineResolution(Resolution(Accuracy.HIGH, 1000L, 1.0))` method call (you can of course adjust the resolution to your needs).

In order to be able to animate smoothly the publisher position on the subscriber side, we are sending the publisher's calculated resolution to the subscribers. This was available before as a debug feature and was disabled by default. Now, it is enabled by default, so if you are calling publisher builder with `sendResolution(false)` then you should remove that call.

### Animation module

In version 1.1.0 we've added a new module that aims to improve the experience of Ably Asset Tracking users on the subscriber side. For now, the module contains an extension that takes care of creating a smooth animations of the location updates received from the publisher. In order to use those features you first have to add the new dependency to your `build.gradle` file:

```groovy
dependencies {
    implementation 'com.ably.tracking:ui-sdk:1.1.0-rc.2'
}
```

Now you should have access to the `LocationAnimator`, `CoreLocationAnimator` and `Position` classes.

To use the animation logic you need to create a new instance of the `CoreLocationAnimator` and use its shared flows to receive both map marker and map camera location updates.

```kotlin
val locationAnimator: LocationAnimator = CoreLocationAnimator()
locationAnimator.positionsFlow
    .onEach { mapMarkerPosition -> setMapMarkerPosition(mapMarkerPosition) }
    .launchIn(scope)

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
