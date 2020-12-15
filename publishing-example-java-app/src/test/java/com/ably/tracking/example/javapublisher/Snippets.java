package com.ably.tracking.example.javapublisher;

import com.ably.tracking.Accuracy;
import com.ably.tracking.Resolution;
import com.ably.tracking.publisher.DefaultProximity;
import com.ably.tracking.publisher.DefaultResolutionConstraints;
import com.ably.tracking.publisher.DefaultResolutionSet;
import com.ably.tracking.publisher.Proximity;
import com.ably.tracking.publisher.ResolutionPolicy;
import com.ably.tracking.publisher.Trackable;
import com.ably.tracking.publisher.TrackableResolutionRequest;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Snippets {
    /**
     * A contrived snippet to prove that we can implement a resolution policy in Java.
     */
    @Test
    public void implementingResolutionPolicy() {
        // Implement the ResolutionPolicy interface.
        final ResolutionPolicy policy = new ResolutionPolicy() {
            @NotNull
            @Override
            public Resolution resolve(@NotNull Set<Resolution> resolutions) {
                // Return nonsense values, so we can validate them at the end of this test.
                return new Resolution(Accuracy.MINIMUM, -666, -999);
            }

            @NotNull
            @Override
            public Resolution resolve(@NotNull TrackableResolutionRequest request) {
                // Return nonsense values, so we can validate them at the end of this test.
                return new Resolution(Accuracy.MAXIMUM, -1666, -1999);
            }
        };

        // Call the policy in the same manner that the Kotlin-based pubisher would.
        final Resolution resolutionsResult = policy.resolve(new HashSet<Resolution>());
        final Resolution requestsResult = policy.resolve(new TrackableResolutionRequest(new Trackable("", null, null, null), Collections.emptySet()));

        // Validate that our policy returned what we told it to above.
        Assert.assertEquals(Accuracy.MINIMUM, resolutionsResult.getAccuracy());
        Assert.assertEquals(-666, resolutionsResult.getDesiredInterval());
        Assert.assertEquals(-999, resolutionsResult.getMinimumDisplacement(), 0.1);
        Assert.assertEquals(Accuracy.MAXIMUM, requestsResult.getAccuracy());
        Assert.assertEquals(-1666, requestsResult.getDesiredInterval());
        Assert.assertEquals(-1999, requestsResult.getMinimumDisplacement(), 0.1);
    }

    /**
     * Validates that we can construct a set of default resolution constraints in Java and apply them to a new
     * Trackable item.
     */
    @Test
    public void creatingDefaultResolutionConstraints() {
        final DefaultResolutionSet resolutions = new DefaultResolutionSet(
            new Resolution(Accuracy.LOW, 10000, 100),
            new Resolution(Accuracy.BALANCED, 5000, 50),
            new Resolution(Accuracy.BALANCED, 2000, 20),
            new Resolution(Accuracy.HIGH, 1000, 10)
        );

        final long twoMinutes = 2 * 60 * 1000;
        final DefaultResolutionConstraints constraints = new DefaultResolutionConstraints(
            resolutions,
            new DefaultProximity(twoMinutes),
            20.0f,
            3.0f
        );

        final Trackable trackable = new Trackable("Foo", null, null, constraints);

        Assert.assertEquals("Foo", trackable.getId());

        Assert.assertTrue(trackable.getConstraints() instanceof DefaultResolutionConstraints);
        final DefaultResolutionConstraints returnedConstraints = (DefaultResolutionConstraints) trackable.getConstraints();

        final DefaultProximity returnedProximity = (DefaultProximity) returnedConstraints.getProximityThreshold();
        Assert.assertEquals(twoMinutes, returnedProximity.getTemporal().longValue());

        Assert.assertEquals(20.0f, returnedConstraints.getBatteryLevelThreshold(), 0.1f);
        Assert.assertEquals(3.0f, returnedConstraints.getLowBatteryMultiplier(), 0.1f);

        Assert.assertEquals(Accuracy.LOW, returnedConstraints.getResolutions().getFarWithoutSubscriber().getAccuracy());
        Assert.assertEquals(10000, returnedConstraints.getResolutions().getFarWithoutSubscriber().getDesiredInterval());
        Assert.assertEquals(100, returnedConstraints.getResolutions().getFarWithoutSubscriber().getMinimumDisplacement(), 0.1);

        Assert.assertEquals(Accuracy.BALANCED, returnedConstraints.getResolutions().getFarWithSubscriber().getAccuracy());
        Assert.assertEquals(5000, returnedConstraints.getResolutions().getFarWithSubscriber().getDesiredInterval());
        Assert.assertEquals(50, returnedConstraints.getResolutions().getFarWithSubscriber().getMinimumDisplacement(), 0.1);

        Assert.assertEquals(Accuracy.BALANCED, returnedConstraints.getResolutions().getNearWithoutSubscriber().getAccuracy());
        Assert.assertEquals(2000, returnedConstraints.getResolutions().getNearWithoutSubscriber().getDesiredInterval());
        Assert.assertEquals(20, returnedConstraints.getResolutions().getNearWithoutSubscriber().getMinimumDisplacement(), 0.1);

        Assert.assertEquals(Accuracy.HIGH, returnedConstraints.getResolutions().getNearWithSubscriber().getAccuracy());
        Assert.assertEquals(1000, returnedConstraints.getResolutions().getNearWithSubscriber().getDesiredInterval());
        Assert.assertEquals(10, returnedConstraints.getResolutions().getNearWithSubscriber().getMinimumDisplacement(), 0.1);
    }

    /**
     * A contrived snippet to prove that we can implement a proximity handler in Java.
     */
    @Test
    public void implementingProximityHandler() {
        final List<String> log = new ArrayList<>();

        // Implement the ProximityHandler interface.
        final ResolutionPolicy.Methods.ProximityHandler handler = new ResolutionPolicy.Methods.ProximityHandler() {
            @Override
            public void onProximityReached(@NotNull Proximity threshold) {
                log.add("reached");
                Assert.assertTrue(threshold instanceof DefaultProximity);
                final DefaultProximity dp = (DefaultProximity) threshold;
                Assert.assertEquals(333.0, dp.getSpatial(), 0.1);
            }

            @Override
            public void onProximityCancelled() {
                log.add("cancelled");
            }
        };

        // Call the handler in the same manner that a Kotlin-based publisher would.
        handler.onProximityReached(new DefaultProximity(333.0));
        handler.onProximityCancelled();

        // FWIW, validate the handled received in the called order.
        Assert.assertEquals(Arrays.asList("reached", "cancelled"), log);
    }
}
