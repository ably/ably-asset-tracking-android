package com.ably.tracking.example.publisher

import androidx.multidex.MultiDexApplication

class PublishingExampleApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        S3Helper.init(this)
    }
}
