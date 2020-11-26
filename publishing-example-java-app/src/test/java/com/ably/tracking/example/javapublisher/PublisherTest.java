package com.ably.tracking.example.javapublisher;

import com.ably.tracking.publisher.Publisher;

import org.junit.Test;

public class PublisherTest {
    @Test
    public void publishersFactoryShouldNotThrowError() {
        Publisher.publishers();
    }

    @Test
    public void publishersShouldAllowToSetDeliveryDataWithOptionalParams() {
        Publisher.publishers().delivery("some_id", null, null);
    }
}
