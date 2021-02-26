package com.ably.tracking.example.javapublisher;

import android.annotation.SuppressLint;
import android.content.Context;

import com.ably.tracking.Accuracy;
import com.ably.tracking.ConnectionConfiguration;
import com.ably.tracking.LogConfiguration;
import com.ably.tracking.Resolution;
import com.ably.tracking.publisher.DefaultProximity;
import com.ably.tracking.publisher.DefaultResolutionConstraints;
import com.ably.tracking.publisher.DefaultResolutionSet;
import com.ably.tracking.publisher.Destination;
import com.ably.tracking.publisher.LocationHistoryData;
import com.ably.tracking.publisher.LocationSourceAbly;
import com.ably.tracking.publisher.LocationSourceRaw;
import com.ably.tracking.publisher.MapConfiguration;
import com.ably.tracking.publisher.Publisher;
import com.ably.tracking.publisher.ResolutionPolicy;
import com.ably.tracking.publisher.RoutingProfile;
import com.ably.tracking.publisher.Trackable;
import com.ably.tracking.publisher.java.PublisherFacade;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressLint("MissingPermission")
public class PublisherInterfaceUsageExamples {
    Context context;
    Publisher nativePublisher;
    Publisher.Builder publisherBuilder;
    ResolutionPolicy.Factory resolutionPolicyFactory;
    PublisherFacade publisher;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        nativePublisher = mock(Publisher.class);
        publisherBuilder = mock(Publisher.Builder.class, withSettings().defaultAnswer(RETURNS_SELF));
        resolutionPolicyFactory = mock(ResolutionPolicy.Factory.class);
        when(publisherBuilder.start()).thenReturn(nativePublisher);
        publisher = PublisherFacade.wrap(nativePublisher);
    }

    @Test
    public void publisherBuilderUsageExample() {
        publisherBuilder
            .androidContext(context)
            .connection(new ConnectionConfiguration("API_KEY", "CLIENT_ID"))
            .log(new LogConfiguration(true))
            .map(new MapConfiguration("API_KEY"))
            .resolutionPolicy(resolutionPolicyFactory)
            .locationSource(LocationSourceRaw.createRaw(new LocationHistoryData("1.0", new ArrayList<>()), null))
            .locationSource(LocationSourceAbly.create("CHANNEL_ID"))
            .start();
    }

    @Test
    public void publisherUsageExample() {
        Trackable trackable = new Trackable("ID", null, null, null);
        Trackable activeTrackable = nativePublisher.getActive();
        nativePublisher.setRoutingProfile(RoutingProfile.CYCLING);
        RoutingProfile routingProfile = nativePublisher.getRoutingProfile();
        // TODO - uncomment when PublisherFacade is implemented
//        CompletableFuture<Void> trackResult = publisher.trackAsync(trackable);
//        CompletableFuture<Void> addResult = publisher.addAsync(trackable);
//        CompletableFuture<Boolean> removeResult = publisher.removeAsync(trackable);
//        CompletableFuture<Void> stopResult = publisher.stopAsync();
//        publisher.addListener(locationUpdate -> {
//        });
    }

    @Test
    public void trackableCreationExample() {
        Trackable trackable = new Trackable(
            "ID",
            "METADATA",
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
