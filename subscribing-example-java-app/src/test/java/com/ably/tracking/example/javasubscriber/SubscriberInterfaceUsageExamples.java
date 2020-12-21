package com.ably.tracking.example.javasubscriber;

import android.content.Context;
import android.location.Location;

import com.ably.tracking.Accuracy;
import com.ably.tracking.ConnectionConfiguration;
import com.ably.tracking.LogConfiguration;
import com.ably.tracking.Resolution;
import com.ably.tracking.subscriber.Subscriber;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class SubscriberInterfaceUsageExamples {
    Context context;
    Subscriber subscriber;
    Subscriber.Builder subscriberBuilder;

    @Before
    public void beforeEach() {
        context = mock(Context.class);
        subscriber = mock(Subscriber.class);
        subscriberBuilder = mock(Subscriber.Builder.class, withSettings().defaultAnswer(RETURNS_SELF));
        when(subscriberBuilder.start()).thenReturn(subscriber);
    }

    @Test
    public void publisherBuilderUsageExample() {
        subscriberBuilder
            .assetStatusListener(isOnline -> {
                onAssetStatusChanged(isOnline);
                return null;
            })
            .connection(new ConnectionConfiguration("API_KEY", "CLIENT_ID"))
            .enhancedLocationUpdatedListener(location -> {
                onLocationUpdated(location);
                return null;
            })
            .log(new LogConfiguration(true))
            .rawLocationUpdatedListener(location -> {
                onLocationUpdated(location);
                return null;
            })
            .resolution(new Resolution(Accuracy.MAXIMUM, 1L, 1.0))
            .trackingId("ID")
            .start();
    }

    @Test
    public void publisherUsageExample() {
        subscriber.sendChangeRequest(new Resolution(Accuracy.MAXIMUM, 1L, 1.0), () -> {
            onSuccess();
            return null;
        }, e -> {
            onError();
            return null;
        });
        subscriber.stop();
    }

    private void onSuccess() {

    }

    private void onError() {

    }

    private void onAssetStatusChanged(Boolean isOnline) {

    }

    private void onLocationUpdated(Location location) {

    }
}
