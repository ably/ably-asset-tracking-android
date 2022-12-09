# Architectural notes

## Gradle setup

### Shared configuration

The projects uses multiple Gradle modules. Instead of repeating the same configuration in each of the modules we use
the root [build.gradle](/build.gradle) file and `subprojects` block for all the shared configuration. The module-specific build.gradle files
should only contain additional configuration required for the given module.

### Modules

The project consists of multiple modules, each of them having a different purpose.
Modules with `sdk` in their name are part of the public API of the SDK.
Modules ending with `-java` suffix are part of the [Java API facade](#java-api-facade).

- [`android-test-common`](android-test-common/) contains shared code for the Android integration tests.
- `common` contains shared code used by both Publisher and Subscriber which should not be visible in the public API.
- `core-sdk` contains shared code used by both Publisher and Subscriber which is visible in the public API.
- `core-sdk-java` contains shared public API code used by both Publisher and Subscriber API facades for Java users, not required by users accessing the SDK from Kotlin.
- `integration-testing-app` contains integration tests that aim to test both Publisher and Subscriber just like users would use them.
- `publishing-example-app` contains an example app for the Publisher SDK used to showcase all its features.
- `publishing-java-testing` contains integration tests that exercise and showcase the Publisher Java API.
- `publishing-sdk` contains the Publisher SDK code, including its primary public API.
- `publishing-sdk-java` contains the Publisher SDK's API facade for Java users, not required by users accessing the SDK from Kotlin.
- `subscribing-example-app` contains an example app for the Subscriber SDK used to showcase all its features.
- `subscribing-java-testing` contains integration tests that exercise and showcase the Subscriber Java API.
- `subscribing-sdk` contains the Subscriber SDK code, including its primary public API.
- `subscribing-sdk-java` contains the Subscriber SDK's API facade for Java users, not required by users accessing the SDK from Kotlin.
- `test-common` contains shared code for the unit tests.
- `ui-sdk` contains Android User Interface enhancements for the AAT SDKs, an optional public API.
- `ui-sdk-java` contains the Android User Interface enhancements' API facade for Java users, not required by users accessing the SDK from Kotlin.

Not all modules are being published as artifacts in the maven repositories. Only modules that include the [publish.gradle](/publish.gradle)
will be published and available for AAT users.

## Java API facade

The main API for AAT is written in Kotlin and we are using features such as coroutines in it. However, those constructs are not available in the Java world.
In order to be able to have an idiomatic Kotlin API while not forcing Java users to include any Kotlin dependencies we decided to create a Java API facade.
The facade API extends the original API and only replaces the things that are Kotlin-only (e.g. coroutines, flows) with Java constructs (e.g. `CompletableFuture`, listeners).

In order to hide certain methods and fields from the Java API we use the `@JvmSynthetic` annotation.

The Java API facades should be tested in the `publishing-java-testing` and `subscribing-java-testing` modules to make sure that they work correctly
and provide the same features as the Kotlin API.

## Synchronous `Worker` event queue

As described in the [specification](https://github.com/ably/ably-asset-tracking-common/blob/main/specification/README.md#multithreading:-handling-asynchronous-events-safely)
we have taken the synchronous event queue approach to secure the SDK from asynchronous access.

### Event queue became `Worker` queue

After a few iterations we've refactored the event queue to a worker queue. This allowed us to spread the logic across multiple `Worker` classes instead of keeping it all in the `CorePublisher`.
Additionally, we can now easily unit test the workers to make sure they work correctly. The initial worker queue approach used additional `WorkerResult`s and `ResultHandler`s
which were used to handle optional worker's work result and either queue more work or call user callbacks. However, in the newest iteration we removed them in favor of keeping
the whole logic in the `Worker` classes which made it easier to comprehend what's happening. Additionally, the newest version makes sure you don't modify the `properties` from
any asynchronous work thread.

### When and how to use the `Worker` queue

When you want to perform some work that involves accessing or modifying the shared SDK state (called from now on `properties`) you need to use the worker queue. That's because we
synchronize access to the `properties` by only using them from the safe synchronous queue thread.

To use the queue you have to queue a `WorkerSpecification` for your work. This will be used by the `WorkerFactory` to create an appropriate worker and inject any dependencies it needs.
Therefore, you need to create both a class that implements the `Worker` interface that will contain the logic of your work and a corresponding `WorkerSpecification` that will hold
all the input data required by the `Worker`.

The `Worker` should put its logic into the `doWork()` method to perform both synchronous work where they can use the `properties` and asynchronous work using `doAsyncWork {}` block.
The async work is performed on another thread from which you should not access or alter the `properties`. After the asynchronous work is done you should use the `postWork()` method
to queue other workers or call the user callback if it's provided.

The `Worker` also has the `doWhenStopped()` method which will be called when the worker is processed after the SDK was stopped. In most cases from this method you should either
do nothing or call the provided user callback with an appropriate error.

## Custom location engines

In order to be able to change location engine resolution after its creation we had to create our custom implementations.
When Mapbox will support this feature we should switch to using their location engines and stop maintaining our versions.
