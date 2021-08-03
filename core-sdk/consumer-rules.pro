# Rules that are shared in both Subscribing and Publishing SDKs

# Keep all enum values (if those enums are used) for enums that are in com.ably.tracking or any of its subpackages.
-keepclassmembers enum com.ably.tracking.** { *; }
