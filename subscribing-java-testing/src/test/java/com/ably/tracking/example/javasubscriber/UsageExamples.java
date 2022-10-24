package com.ably.tracking.example.javasubscriber;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ably.tracking.Accuracy;
import com.ably.tracking.Resolution;
import com.ably.tracking.connection.Authentication;
import com.ably.tracking.connection.ConnectionConfiguration;
import com.ably.tracking.connection.TokenRequest;
import com.ably.tracking.java.AuthenticationFacade;
import com.ably.tracking.subscriber.Subscriber;
import com.ably.tracking.subscriber.java.SubscriberFacade;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
            .connection(new ConnectionConfiguration(Authentication.basic("CLIENT_ID", "API_KEY"), null))
            // or
            // .connection(new ConnectionConfiguration(createTokenRequestAuthentication(), null))
            // or
            // .connection(new ConnectionConfiguration(createJwtAuthentication(), null))
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

    private Authentication createTokenRequestAuthentication() {
        return AuthenticationFacade.tokenRequest(tokenParams -> CompletableFuture.supplyAsync(() -> {
            // get the token from you auth servers
            return new TokenRequest() {
                @NonNull
                @Override
                public String getMac() {
                    return null;
                }

                @NonNull
                @Override
                public String getNonce() {
                    return null;
                }

                @NonNull
                @Override
                public String getKeyName() {
                    return null;
                }

                @Override
                public long getTimestamp() {
                    return 0;
                }

                @Nullable
                @Override
                public String getClientId() {
                    return null;
                }

                @Nullable
                @Override
                public String getCapability() {
                    return null;
                }

                @Override
                public long getTtl() {
                    return 0;
                }
            };
        }));
    }

    private Authentication createJwtAuthentication() {
        return AuthenticationFacade.jwt(tokenParams -> CompletableFuture.supplyAsync(() -> {
            // get the token from you auth servers
            return "created-jwt";
        }));
    }
}
