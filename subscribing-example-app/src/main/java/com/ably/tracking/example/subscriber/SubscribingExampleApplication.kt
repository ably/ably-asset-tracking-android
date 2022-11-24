package com.ably.tracking.example.subscriber

import androidx.multidex.MultiDexApplication
import timber.log.Timber
import timber.log.Timber.DebugTree

class SubscribingExampleApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree())
    }
}
