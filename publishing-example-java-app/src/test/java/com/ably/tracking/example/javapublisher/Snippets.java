package com.ably.tracking.example.javapublisher;

import androidx.annotation.Nullable;

import com.ably.tracking.Accuracy;
import com.ably.tracking.Resolution;
import com.ably.tracking.publisher.ResolutionPolicy;
import com.ably.tracking.publisher.Trackable;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class Snippets {
    /**
     * A contrived snippet to prove that we can implement a resolution policy in Java.
    @Test
    public void implementingResolutionPolicy() {
        // Implement the ResolutionPolicy interface.
        final ResolutionPolicy policy = new ResolutionPolicy() {
            @NotNull
            @Override
            public Resolution resolve(@NotNull final Set<? extends ResolutionRequest> requests) {
                Assert.assertEquals(2, requests.size());

                // Return nonsense values, so we can validate them at the end of this test.
                return new Resolution(Accuracy.MINIMUM, -666, -999);
            }
        };

        // Create a set of resolution requests.
        final Set<ResolutionRequest> resolutionRequests = new HashSet<>();
        resolutionRequests.add(
            resolutionRequest(
                new Resolution(Accuracy.BALANCED, 1000, 10),
                new Trackable("1", null, null, null),
                ResolutionRequest.Origin.LOCAL
            )
        );
        resolutionRequests.add(
            resolutionRequest(
                new Resolution(Accuracy.MAXIMUM, 200, 2),
                new Trackable("2", null, null, null),
                ResolutionRequest.Origin.SUBSCRIBER
            )
        );

        // Call the policy in the same manner that the Kotlin-based pubisher would.
        final Resolution resolved = policy.resolve(resolutionRequests);

        // Validate that our policy returned what we told it to above.
        Assert.assertEquals(Accuracy.MINIMUM, resolved.getAccuracy());
        Assert.assertEquals(-666, resolved.getDesiredInterval());
        Assert.assertEquals(-999, resolved.getMinimumDisplacement(), 0.1);
    }

     * Returns a Java implementation of the ResolutionRequest interface.
     * The returned object has sensible, type-aware implementations of both the hashCode() and equals(Object) methods.
    private ResolutionRequest resolutionRequest(final Resolution resolution, final Trackable trackable, final ResolutionRequest.Origin origin) {
        return new ResolutionRequest() {
            @NotNull @Override
            public Resolution getResolution() {
                return resolution;
            }

            @NotNull @Override
            public Trackable getTrackable() {
                return trackable;
            }

            @NotNull @Override
            public Origin getOrigin() {
                return origin;
            }

            @Override
            public int hashCode() {
                return getResolution().hashCode() ^ getTrackable().hashCode() ^ getOrigin().hashCode();
            }

            @Override
            public boolean equals(@Nullable final Object obj) {
                if (!(obj instanceof ResolutionRequest)) {
                    return false;
                }

                final ResolutionRequest other = (ResolutionRequest)obj;
                return getResolution().equals(other.getResolution()) &&
                    getTrackable().equals(other.getTrackable()) &&
                    getOrigin().equals(other.getOrigin());
            }
        };
    }
    */
}
