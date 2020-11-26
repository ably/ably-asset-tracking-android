package com.ably.tracking.example.publisher

import android.content.Context
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.AWSS3StoragePlugin

object S3Helper {

    fun init(context: Context) {
        Amplify.addPlugin(AWSCognitoAuthPlugin())
        Amplify.addPlugin(AWSS3StoragePlugin())
        Amplify.configure(context)
    }
}
