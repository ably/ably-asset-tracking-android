package com.ably.tracking.example.javasubscriber;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ably.tracking.connection.TokenRequest;

/**
 * This is an empty implementation of the interface that uses dummy values.
 * It is created to show how this interface can be implemented.
 */
public class EmptyTokenRequest implements TokenRequest {
    @NonNull
    @Override
    public String getMac() {
        return null;
    }

    @NonNull
    @Override
    public String getNonce() {
        return null;
    }

    @NonNull
    @Override
    public String getKeyName() {
        return null;
    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Nullable
    @Override
    public String getClientId() {
        return null;
    }

    @Nullable
    @Override
    public String getCapability() {
        return null;
    }

    @Override
    public long getTtl() {
        return 0;
    }
}
