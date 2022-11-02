# Java API Guide

## General

### Motivation

We are maintaining a Java API for users that do not want to add any Kotlin dependencies to their projects.
If you already have a codebase with both Java and Kotlin we suggest that you use the Kotlin API.

### Using the Java SDKs

After following the [usage guide](README.md#Usage) you should add the Java dependencies to your `build.gradle` file instead of the Kotlin ones.

```groovy
// AAT Publisher SDK
implementation ('com.ably.tracking:publishing-sdk-java:1.4.0')

// AAT Subscriber SDK
implementation ('com.ably.tracking:subscribing-sdk-java:1.4.0')

// AAT UI extensions
implementation ('com.ably.tracking:ui-sdk-java:1.4.0')
```

### Using the Java facades

For all classes and interfaces that use Kotlin-specific constructs we created facades that change only those methods and fields.
Therefore to create those facade objects you will need to create the original (Kotlin) objects first.

Usually in the facades we are replacing `suspend` functions with `CompletableFuture`s and `Flow`s with listener interfaces.

## Publisher SDK

### Creating Publisher Facade

To use the publisher in Java code you should first create the original `Publisher` object and then use it to create a `PublisherFacade`.

```java
Publisher publisher = Publisher.publishers()
  // configure the publisher instance
  .start();

PublisherFacade publisherFacade = PublisherFacade.wrap(publisher);
```

Then you should use the `PublisherFacade` instance to perform all operations.

### Using Publisher Facade

Below we only describe changes in the Java API.
For the full publisher SDK usage guide please see the [Kotlin guide](README.md#publishing-sdk).

Async operations are returning a `CompletableFuture` that will complete once the operation finishes.

```java
publisherFacade.trackAsync(trackable, trackableState -> {
    // handle trackableState changes
  })
  .thenRun(() -> {
    // operation completed successfully
  })
  .exceptionally(throwable -> {
    // operation failed
    return null;
  });
```

Flows of data were converted to a listener-based approach and in order to receive the data you should add a listener to the `PublisherFacade`.

```java
publisherFacade.addTrackableStateListener("trackable-id", trackableState -> {
  // handle trackableState changes
});
```

## Subscriber SDK

### Creating Subscriber Facade

To use the subscriber in Java code you should first create the original `Subscriber.Builder` object and use it to configure the subscriber instance.
Then use the builder object to create a `SubscriberFacade.Builder` and use it to create the `SubscriberFacade` object.

```java
Subscriber.Builder subscriberBuilder = Subscriber.subscribers()
  // configure the subscriber instance

SubscriberFacade.Builder subscriberFacadeBuilder = SubscriberFacade.Builder.wrap(subscriberBuilder);
subscriberFacadeBuilder.startAsync()
  .thenAccept(subscriberFacade -> {
    // from here you can use the subscriberFacade
  })
  .exceptionally(throwable -> {
    // creating subscriber facade failed
    return null;
  });
```

Then you should use the `SubscriberFacade` instance to perform all operations.

### Use Subscriber Facade

Below we only describe changes in the Java API.
For the full subscriber SDK usage guide please see the [Kotlin guide](README.md#subscribing-sdk).

Async operations are returning a `CompletableFuture` that will complete once the operation finishes.

```java
subscriberFacade.stopAsync()
  .thenRun(() -> {
    // operation completed successfully
  })
  .exceptionally(throwable -> {
    // operation failed
    return null;
  });
```

Flows of data were converted to a listener-based approach and in order to receive the data you should add a listener to the `SubscriberFacade`.

```java
subscriberFacade.addLocationListener(locationUpdate -> {
  // handle locationUpdate changes
});
```

## UI SDK

### Creating Location Animator Facade

To use the location animator in Java code you should first create the original `LocationAnimator` object and use it to create the `LocationAnimatorFacade`.

```java
LocationAnimator locationAnimator = new CoreLocationAnimator();

LocationAnimatorFacade locationAnimatorFacade = new CoreLocationAnimatorFacade(locationAnimator);
```

Then you should use the `LocationAnimatorFacade` instance to perform all operations.

### Using Location Animator Facade

Below we only describe changes in the Java API.
For the full location animator usage guide please see the [Kotlin guide](README.md#location-animator).

Flows of data were converted to a listener-based approach and in order to receive the data you should add a listener to the `LocationAnimatorFacade`.

```java
locationAnimatorFacade.addPositionListener(position -> {
  // handle new map marker position
});

locationAnimatorFacade.addCameraPositionListener(position -> {
  // handle new camera position
});
```
