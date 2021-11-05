package com.ably.tracking.example.javapublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.annotation.SuppressLint;
import android.content.Context;

import com.ably.tracking.Accuracy;
import com.ably.tracking.BuilderConfigurationIncompleteException;
import com.ably.tracking.ConnectionException;
import com.ably.tracking.Resolution;
import com.ably.tracking.connection.Authentication;
import com.ably.tracking.connection.ConnectionConfiguration;
import com.ably.tracking.locationprovider.Destination;
import com.ably.tracking.locationprovider.RoutingProfile;
import com.ably.tracking.publisher.DefaultProximity;
import com.ably.tracking.publisher.DefaultResolutionConstraints;
import com.ably.tracking.publisher.DefaultResolutionSet;
import com.ably.tracking.publisher.Publisher;
import com.ably.tracking.publisher.ResolutionPolicy;
import com.ably.tracking.publisher.Trackable;
import com.ably.tracking.publisher.java.PublisherFacade;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SuppressLint("MissingPermission")
public class PublisherInterfaceUsageExamples {
    Context context;
    Publisher nativePublisher;
    Publisher.Builder publisherBuilder;
    ResolutionPolicy.Factory resolutionPolicyFactory;
    PublisherFacade publisher;

    @Before
    public void beforeEach() throws BuilderConfigurationIncompleteException, ConnectionException {
        context = mock(Context.class);
        nativePublisher = mock(Publisher.class);
        publisherBuilder = mock(Publisher.Builder.class, withSettings().defaultAnswer(RETURNS_SELF));
        resolutionPolicyFactory = mock(ResolutionPolicy.Factory.class);
        when(publisherBuilder.start()).thenReturn(nativePublisher);
        publisher = mock(PublisherFacade.class);
        when(publisher.addAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(publisher.trackAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(publisher.removeAsync(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(publisher.stopAsync()).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    public void publisherBuilderUsageExample() {
        try {
            publisherBuilder
                .connection(new ConnectionConfiguration(Authentication.basic("CLIENT_ID", "ABLY_API_KEY")))
//                .locationProvider(
//                    new MapboxLocationProvider(
//                        context,
//                        new MapConfiguration("MAPBOX_API_KEY"),
//                        new ConnectionConfiguration(Authentication.basic("CLIENT_ID", "ABLY_API_KEY")),
//                        LocationSourceRaw.createRaw(
//                            new MapboxLocationHistoryData(new ArrayList<>()),
//                            null
//                        ),
//                        null,
//                        () -> new NotificationCompat.Builder(context, "test-channel")
//                            .setContentTitle("Title")
//                            .setContentText("Text")
//                            .setSmallIcon(R.drawable.aat_logo)
//                            .build(),
//                        123
//                    )
//                )
                .resolutionPolicy(resolutionPolicyFactory)
                .start();
        } catch (BuilderConfigurationIncompleteException e) {
            // handle publisher start error
        } catch (ConnectionException e) {
            // handle publisher start error
        }
    }

    @Test
    public void publisherFacadeWrapperUsageExample() {
        PublisherFacade publisherFacade = PublisherFacade.wrap(nativePublisher);
    }

    @Test
    public void publisherUsageExample() {
        Trackable trackable = new Trackable("ID", null, null);
        Trackable activeTrackable = publisher.getActive();
        publisher.setRoutingProfile(RoutingProfile.CYCLING);
        RoutingProfile routingProfile = publisher.getRoutingProfile();
        try {
            publisher.trackAsync(trackable, trackableState -> {
                // handle trackableState
            }).get();
            publisher.addAsync(trackable, trackableState -> {
                // handle trackableState
            }).get();
            Boolean wasRemoved = publisher.removeAsync(trackable).get();
            publisher.stopAsync().get();
        } catch (ExecutionException e) {
            // handle execution exception
        } catch (InterruptedException e) {
            // handle interruption exception
        }
        publisher.addListener(locationUpdate -> {
            // handle locationUpdate
        });
        publisher.addTrackablesListener(trackables -> {
            // handle trackables
        });
        publisher.addLocationHistoryListener(locationHistory -> {
            // handle locationHistory
        });
        publisher.addTrackableStateListener("ID", trackableState -> {
            // handle trackableState
        });
    }

    @Test
    public void trackableCreationExample() {
        Trackable trackable = new Trackable(
            "ID",
            new Destination(1.0, 1.0),
            new DefaultResolutionConstraints(
                new DefaultResolutionSet(new Resolution(Accuracy.MAXIMUM, 1L, 1.0)),
                new DefaultProximity(1L, 1.0),
                50F,
                3F
            )
        );
    }
}
