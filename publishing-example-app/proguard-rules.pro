# Publishing example app specific rules

# Keep all enum values (if those enums are used) for enums that are in com.amplifyframework or any of its subpackages.
# Needed for Amplify library.
-keepclassmembers enum com.amplifyframework.** { *; }

# Keep all enum values (if those enums are used) for enums that are in com.amazonaws or any of its subpackages.
# Needed for Amplify library.
-keepclassmembers enum com.amazonaws.** { *; }
