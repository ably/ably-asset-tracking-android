package com.ably.tracking.example.javasubscriber;

import com.ably.tracking.Accuracy;
import com.ably.tracking.connection.AuthenticationConfiguration;
import com.ably.tracking.Resolution;
import com.ably.tracking.connection.TokenRequest;
import com.ably.tracking.java.AuthenticationConfigurationFactory;
import com.ably.tracking.subscriber.Subscriber;
import com.ably.tracking.subscriber.java.SubscriberFacade;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class UsageExamples {
    SubscriberFacade subscriberFacade;
    Subscriber nativeSubscriber;
    Subscriber.Builder nativeSubscriberBuilder;
    SubscriberFacade.Builder subscriberFacadeBuilder;

    @Before
    public void beforeEach() {
        nativeSubscriber = mock(Subscriber.class);
        nativeSubscriberBuilder = mock(Subscriber.Builder.class, withSettings().defaultAnswer(RETURNS_SELF));
        subscriberFacadeBuilder = mock(SubscriberFacade.Builder.class, withSettings().defaultAnswer(RETURNS_SELF));
        when(subscriberFacadeBuilder.startAsync()).thenReturn(CompletableFuture.completedFuture(subscriberFacade));
        subscriberFacade = mock(SubscriberFacade.class);
        when(subscriberFacade.resolutionPreferenceAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(subscriberFacade.stopAsync()).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    public void subscriberBuilderUsageExample() {
        Subscriber.Builder nativeBuilder = Subscriber.subscribers()
            .connection(AuthenticationConfigurationFactory.createBasic("API_KEY", "CLIENT_ID"))
            .trackingId("TRACKING_ID")
            .resolution(new Resolution(Accuracy.BALANCED, 1000L, 1.0));
        SubscriberFacade.Builder wrappedSubscriberBuilder = SubscriberFacade.Builder.wrap(nativeBuilder);
        try {
            // normally we'd use that wrapped builder from above, but we're not able to mock that static "wrap" method here
            SubscriberFacade subscriberFacade = subscriberFacadeBuilder.startAsync().get();
        } catch (ExecutionException e) {
            // handle execution exception
        } catch (InterruptedException e) {
            // handle interruption exception
        }
    }

    @Test
    public void subscriberFacadeBuilderWrapperUsageExample() {
        SubscriberFacade.Builder subscriberFacadeBuilder = SubscriberFacade.Builder.wrap(nativeSubscriberBuilder);
    }

    @Test
    public void subscriberTokenAuthUsageExample() {
        AuthenticationConfiguration configuration = AuthenticationConfigurationFactory.createToken((params) -> new TokenRequest(0, "", "", 0, "", "", ""), "CLIENT_ID");
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
            subscriberFacade.resolutionPreferenceAsync(new Resolution(Accuracy.MAXIMUM, 1L, 1.0)).get();
            subscriberFacade.stopAsync().get();
        } catch (ExecutionException e) {
            // handle execution exception
        } catch (InterruptedException e) {
            // handle interruption exception
        }
    }
}
