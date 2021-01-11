# Rules that are shared in both Subscribing and Publishing SDKs

# Keep all annotations (needed in order to properly handle JSON names with @SerializedName annotation) and signatures
-keepattributes *Annotation*
-keepattributes Signature

# Keep GeoJSON classes (without it Subscriber couldn't parse messages from Publisher)
-keep class com.ably.tracking.common.GeoJsonMessage { *; }
-keep class com.ably.tracking.common.GeoJsonGeometry { *; }
-keep class com.ably.tracking.common.GeoJsonProperties { *; }

# Keep all enum values (if those enums are used) for enums that are in com.ably.tracking or any of its subpackages.
-keepclassmembers enum com.ably.tracking.** { *; }
