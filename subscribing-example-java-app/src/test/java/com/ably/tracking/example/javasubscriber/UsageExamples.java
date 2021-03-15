package com.ably.tracking.example.javasubscriber;

import com.ably.tracking.Accuracy;
import com.ably.tracking.ConnectionConfiguration;
import com.ably.tracking.Resolution;
import com.ably.tracking.subscriber.Subscriber;
import com.ably.tracking.subscriber.java.SubscriberFacade;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class UsageExamples {
    SubscriberFacade subscriberFacade;
    Subscriber nativeSubscriber;
    Subscriber.Builder subscriberBuilder;

    @Before
    public void beforeEach() {
        nativeSubscriber = mock(Subscriber.class);
        subscriberBuilder = mock(Subscriber.Builder.class, withSettings().defaultAnswer(RETURNS_SELF));
        when(subscriberBuilder.start()).thenReturn(nativeSubscriber);
        subscriberFacade = mock(SubscriberFacade.class);
        when(subscriberFacade.sendChangeRequestAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(subscriberFacade.stopAsync()).thenReturn(CompletableFuture.completedFuture(any()));
    }

    @Test
    public void subscriberBuilderUsageExample() {
        Subscriber nativeSubscriber = subscriberBuilder
            .connection(new ConnectionConfiguration("API_KEY", "CLIENT_ID"))
            .trackingId("TRACKING_ID")
            .resolution(new Resolution(Accuracy.BALANCED, 1000L, 1.0))
            .start();
    }

    @Test
    public void subscriberFacadeWrapperUsageExample() {
        SubscriberFacade subscriberFacade = SubscriberFacade.wrap(nativeSubscriber);
    }

    @Test
    public void subscriberFacadeUsageExample() {
        subscriberFacade.addListener(assetState -> {
            // handle assetState
        });

        subscriberFacade.addLocationListener(locationUpdate -> {
            // handle locationUpdate
        });

        try {
//        // use methods that return a completable future
            subscriberFacade.sendChangeRequestAsync(new Resolution(Accuracy.MAXIMUM, 1L, 1.0)).get();
            subscriberFacade.stopAsync().get();
        } catch (ExecutionException e) {
            // handle execution exception
        } catch (InterruptedException e) {
            // handle interruption exception
        }
    }
}
