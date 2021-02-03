package com.ably.tracking.example.javasubscriber;

import com.ably.tracking.Accuracy;
import com.ably.tracking.ConnectionConfiguration;
import com.ably.tracking.LogConfiguration;
import com.ably.tracking.Resolution;
import com.ably.tracking.subscriber.Subscriber;
import com.ably.tracking.subscriber.java.SubscriberFacade;

import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class UsageExamples {
    @Test
    public void publisherUsageExample() {
        final Subscriber nativeSubscriber =
            Subscriber.subscribers()
                .connection(new ConnectionConfiguration("API_KEY", "CLIENT_ID"))
                .log(new LogConfiguration(true))
                .resolution(new Resolution(Accuracy.MAXIMUM, 1L, 1.0))
                .trackingId("ID")
                .start();

        final SubscriberFacade subscriber = SubscriberFacade.wrap(nativeSubscriber);

        // TODO - uncomment when SubscriberFacade is implemented
//        subscriber.addListener(isOnline -> {
//            // handle isOnline
//        });
//
//        subscriber.addEnhancedLocationListener(locationUpdate -> {
//            // handle locationUpdate
//        });
//
//        // use methods that return a completable future
//        try {
//            subscriber.sendChangeRequestAsync(new Resolution(Accuracy.MAXIMUM, 1L, 1.0)).get();
//            subscriber.stopAsync().get();
//        } catch (ExecutionException e) {
//            // handle execution exception
//        } catch (InterruptedException e) {
//            // handle interruption exception
//        }
    }
}
