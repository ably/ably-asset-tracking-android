package com.ably.tracking.example.publisher

import android.content.Context
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object S3Helper {

    fun init(context: Context) {
        Amplify.addPlugin(AWSCognitoAuthPlugin())
        Amplify.addPlugin(AWSS3StoragePlugin())
        Amplify.configure(context)
    }

    fun fetchLocationHistoryFilenames(onListLoaded: (filenamesWithSizes: List<CharSequence>, filenames: List<CharSequence>) -> Unit) {
        Amplify.Storage.list(
            "",
            { result ->
                val filenamesWithSizes = mutableListOf<String>()
                val filenames = mutableListOf<String>()
                result.items.forEach { item ->
                    filenamesWithSizes.add("${item.key} (${formatFileSize(item.size)})")
                    filenames.add(item.key)
                }
                onListLoaded(filenamesWithSizes, filenames)
            },
            { error -> Timber.e(error, "Error when listing S3 files") }
        )
    }

    private fun formatFileSize(size: Long): String? {
        if (size <= 0) return "0"
        val units = listOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return "${DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble()))} ${units[digitGroups]}"
    }
}
