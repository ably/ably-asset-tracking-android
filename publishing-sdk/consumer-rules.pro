# Keep all enum values (if those enums are used) for enums that are in com.ably.tracking or any of its subpackages.
-keepclassmembers enum com.ably.tracking.** { *; }

# Keep all classes from com.mapbox.navigation.core.replay.history package (without subpackages).
# They're needed for replaying history location events in the Publisher.
-keep class com.mapbox.navigation.core.replay.history.* {*;}
