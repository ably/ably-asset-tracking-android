package com.ably.tracking.example.subscriber

import android.app.Application
import timber.log.Timber
import timber.log.Timber.DebugTree

class SubscribingExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree())
    }
}
