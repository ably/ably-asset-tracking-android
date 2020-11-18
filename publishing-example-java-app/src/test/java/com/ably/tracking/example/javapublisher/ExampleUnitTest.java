package com.ably.tracking.example.javapublisher;

import org.junit.Test;

import com.ably.tracking.publisher.AssetPublisher;
import kotlin.NotImplementedError;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void publishers_factory_should_not_throw_error() {
        AssetPublisher.publishers();
    }
}
