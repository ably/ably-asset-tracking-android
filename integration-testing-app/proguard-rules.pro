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

# This is needed for ObfuscationTest to receive messages from the Ably SDK
-keep,allowobfuscation class io.ably.** { *; }

# This is needed for ChannelModesTest to make HTTP request to Ably REST API
-keep,allowobfuscation class okhttp3.** { *; }

-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.firebase.installations.FirebaseInstallations
-dontwarn com.google.firebase.installations.InstallationTokenResult
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn javax.xml.bind.DatatypeConverter
-dontwarn org.bouncycastle.jce.ECNamedCurveTable
-dontwarn org.bouncycastle.jce.spec.ECNamedCurveParameterSpec
-dontwarn org.slf4j.impl.StaticLoggerBinder