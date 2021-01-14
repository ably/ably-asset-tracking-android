package com.ably.tracking.example.javasubscriber;

import android.content.Context;

import com.ably.tracking.Accuracy;
import com.ably.tracking.ConnectionConfiguration;
import com.ably.tracking.LogConfiguration;
import com.ably.tracking.Resolution;
import com.ably.tracking.subscriber.Subscriber;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import timber.log.Timber;

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
            .assetStatus(isOnline -> { })
            .connection(new ConnectionConfiguration("API_KEY", "CLIENT_ID"))
            .enhancedLocations(location -> { })
            .log(new LogConfiguration(true))
            .resolution(new Resolution(Accuracy.MAXIMUM, 1L, 1.0))
            .trackingId("ID")
            .start();
    }

    @Test
    public void publisherUsageExample() {
        subscriber.sendChangeRequest(new Resolution(Accuracy.MAXIMUM, 1L, 1.0),
            result -> {
                if (result.isSuccess()) {
                    Timber.d("Success");
                } else {
                    Timber.e("Failed with error information: %s", result.getFailure().getErrorInformation());
                }
            }
        );
        subscriber.stop(result -> {
            if (result.isSuccess()) {
                Timber.d("Success");
            } else {
                Timber.e("Failed with error information: %s", result.getFailure().getErrorInformation());
            }
        });
    }
}
