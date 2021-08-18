# Integration tests specfic rule

# Because this module only has android test classes the proguard doesn't see any code and when
# shrinking it removes all possible code. In order to make tests work we're explicitly specifying
# to keep all classes from AAT and Kotlin standard libs. Even though we're keeping the classes
# we're still obfuscating them with 'allowobfuscation'.
-keep,allowobfuscation class com.ably.tracking.** { *; }
-keep,allowobfuscation class kotlin.** { *; }
-keep,allowobfuscation class kotlinx.** { *; }

# This is needed for AuthenticationTests.jwtAuthenticationShouldCreateWorkingConnectionBetweenPublisherAndSubscriber()
# test which needs to create a JWT authentication config.
-keep,allowobfuscation class io.jsonwebtoken.** { *; }
