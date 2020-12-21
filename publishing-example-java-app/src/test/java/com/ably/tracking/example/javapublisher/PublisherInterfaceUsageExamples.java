package com.ably.tracking.example.javapublisher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import com.ably.tracking.Accuracy;
import com.ably.tracking.AddTrackableListener;
import com.ably.tracking.ConnectionConfiguration;
import com.ably.tracking.LogConfiguration;
import com.ably.tracking.RemoveTrackableListener;
import com.ably.tracking.Resolution;
import com.ably.tracking.TrackTrackableListener;
import com.ably.tracking.publisher.DebugConfiguration;
import com.ably.tracking.publisher.DefaultProximity;
import com.ably.tracking.publisher.DefaultResolutionConstraints;
import com.ably.tracking.publisher.DefaultResolutionSet;
import com.ably.tracking.publisher.Destination;
import com.ably.tracking.publisher.LocationSourceRaw;
import com.ably.tracking.publisher.MapConfiguration;
import com.ably.tracking.publisher.Publisher;
import com.ably.tracking.publisher.ResolutionPolicy;
import com.ably.tracking.publisher.Trackable;
import com.ably.tracking.publisher.TransportationMode;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import io.ably.lib.realtime.ConnectionStateListener;

import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressLint("MissingPermission")
public class PublisherInterfaceUsageExamples {
    Context context;
    Publisher publisher;
    Publisher.Builder publisherBuilder;
    ResolutionPolicy.Factory resolutionPolicyFactory;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        publisher = mock(Publisher.class);
        publisherBuilder = mock(Publisher.Builder.class, withSettings().defaultAnswer(RETURNS_SELF));
        resolutionPolicyFactory = mock(ResolutionPolicy.Factory.class);
        when(publisherBuilder.start()).thenReturn(publisher);
    }

    @Test
    public void publisherBuilderUsageExample() {
        publisherBuilder
            .androidContext(context)
            .connection(new ConnectionConfiguration("API_KEY", "CLIENT_ID"))
            .debug(new DebugConfiguration(connectionStateChange -> {
                onConnectionStateChanged(connectionStateChange);
            },
                new LocationSourceRaw(""),
                historyData -> {
                    onHistoryData(historyData);
                }))
            .locationUpdatedListener(location -> {
                onLocationUpdated(location);
            })
            .log(new LogConfiguration(true))
            .map(new MapConfiguration("API_KEY"))
            .resolutionPolicy(resolutionPolicyFactory)
            .start();
    }

    @Test
    public void publisherUsageExample() {
        Trackable trackable = new Trackable("ID", null, null, null);
        publisher.track(
            trackable,
            new TrackTrackableListener() {
                @Override
                public void onSuccess() {
                    doOnSuccess();
                }

                @Override
                public void onError(@NotNull Exception exception) {
                    doOnError();
                }
            }
        );
        publisher.add(
            trackable,
            new AddTrackableListener() {
                @Override
                public void onSuccess() {
                    doOnSuccess();
                }

                @Override
                public void onError(@NotNull Exception exception) {
                    doOnError();
                }
            }
        );
        publisher.remove(trackable,
            new RemoveTrackableListener() {
                @Override
                public void onSuccess(boolean wasPresent) {
                    doOnSuccess();
                }

                @Override
                public void onError(@NotNull Exception exception) {
                    doOnError();
                }
            }
        );
        Trackable activeTrackable = publisher.getActive();
        TransportationMode transportationMode = new TransportationMode("TBC");
        publisher.setTransportationMode(transportationMode);
        transportationMode = publisher.getTransportationMode();
        publisher.stop();
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

    private void doOnSuccess() {

    }

    private void doOnError() {

    }

    private void onConnectionStateChanged(ConnectionStateListener.ConnectionStateChange connectionStateChange) {

    }

    private void onHistoryData(String historyData) {

    }

    private void onLocationUpdated(Location location) {

    }
}
