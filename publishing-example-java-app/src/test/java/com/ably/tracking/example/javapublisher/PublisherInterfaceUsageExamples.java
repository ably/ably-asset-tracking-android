package com.ably.tracking.example.javapublisher;

import android.annotation.SuppressLint;
import android.content.Context;

import com.ably.tracking.Accuracy;
import com.ably.tracking.ConnectionConfiguration;
import com.ably.tracking.LogConfiguration;
import com.ably.tracking.Resolution;
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

import timber.log.Timber;

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

            },
                new LocationSourceRaw(""),
                historyData -> {

                }))
            .locationUpdatedListener(location -> {

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
            result -> {
                if (result.isSuccess()) {
                    Timber.d("Success");
                } else {
                    Timber.e(result.exception());
                }
            }
        );
        publisher.add(
            trackable,
            result -> {
                if (result.isSuccess()) {
                    Timber.d("Success");
                } else {
                    Timber.e(result.exception());
                }
            }
        );
        publisher.remove(trackable,
            result -> {
                if (result.isSuccess()) {
                    Timber.d("Success");
                } else {
                    Timber.e(result.exception());
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
}
