package com.ably.tracking.example.javapublisher;

import org.junit.Test;

import com.ably.tracking.publisher.AssetPublisher;
import kotlin.NotImplementedError;

import static org.junit.Assert.*;

public class AssetPublisherTest {
    @Test
    public void publishersFactoryShouldNotThrowError() {
        AssetPublisher.publishers();
    }
}
