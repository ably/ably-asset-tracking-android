# Publishing SDK specific rules

# Keep all classes from com.mapbox.navigation.core.replay.history package (without subpackages).
# They're needed for replaying history location events in the Publisher.
-keep class com.mapbox.navigation.core.replay.history.* {*;}
