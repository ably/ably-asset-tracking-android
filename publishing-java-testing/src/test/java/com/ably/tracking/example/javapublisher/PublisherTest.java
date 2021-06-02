package com.ably.tracking.example.javapublisher;

import com.ably.tracking.publisher.Publisher;

import org.junit.Test;

public class PublisherTest {
    @Test
    public void publishersFactoryShouldNotThrowError() {
        Publisher.publishers();
    }
}
