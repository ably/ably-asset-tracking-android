# Rules that are shared in both Subscribing and Publishing SDKs

# Keep all annotations (needed in order to properly handle JSON names with @SerializedName annotation) and signatures
-keepattributes *Annotation*
-keepattributes Signature

# Keep all message classes that are sent over Ably from com.ably.tracking.common.message or any of its subpackages (without it Subscriber couldn't parse messages from Publisher)
-keep class com.ably.tracking.common.message.** { *; }

# Keep all enum values (if those enums are used) for enums that are in com.ably.tracking.common or any of its subpackages.
-keepclassmembers enum com.ably.tracking.common.** { *; }
