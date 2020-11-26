package com.ably.tracking.example.javapublisher;

import com.ably.tracking.publisher.AssetPublisher;

import org.junit.Test;

public class AssetPublisherTest {
    @Test
    public void publishersFactoryShouldNotThrowError() {
        AssetPublisher.publishers();
    }

    @Test
    public void publishersShouldAllowToSetDeliveryDataWithOptionalParams() {
        AssetPublisher.publishers().delivery("some_id", null, null);
    }
}
