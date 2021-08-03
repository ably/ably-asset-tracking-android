# Publishing SDK specific rules

# Keep all classes from com.mapbox.navigation.core.replay.history package (without subpackages).
# They're needed for replaying history location events in the Publisher.
-keep class com.mapbox.navigation.core.replay.history.* {*;}

# Keep the generated interface required for creating a custom Mapbox trip notification.
# Without it the Mapbox initialization fails with exception "cannot find method ModuleProvider.createTripNotification"
-keep interface com.mapbox.module.Mapbox_TripNotificationModuleConfiguration$ModuleProvider { *; }
