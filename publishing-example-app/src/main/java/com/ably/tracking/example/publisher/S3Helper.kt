package com.ably.tracking.example.publisher

import android.content.Context
import com.ably.tracking.publisher.LOCATION_HISTORY_VERSION
import com.ably.tracking.publisher.LocationHistoryData
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.s3.AWSS3StoragePlugin
import com.google.gson.Gson
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import timber.log.Timber

object S3Helper {

    private lateinit var gson: Gson
    private var isInitialized = false

    fun init(context: Context) {
        if (isConfigFileProvided(context)) {
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.addPlugin(AWSS3StoragePlugin())
            Amplify.configure(
                AmplifyConfiguration.builder(context).devMenuEnabled(false).build(),
                context
            )
            gson = Gson()
            isInitialized = true
        }
    }

    private fun isConfigFileProvided(context: Context) =
        context.resources.getIdentifier("amplifyconfiguration", "raw", context.packageName) != 0

    fun fetchLocationHistoryFilenames(
        onListLoaded: (filenamesWithSizes: List<CharSequence>, filenames: List<CharSequence>) -> Unit,
        onUninitialized: (() -> Unit)? = null
    ) {
        if (isInitialized) {
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
        } else {
            onUninitialized?.invoke()
        }
    }

    private fun formatFileSize(size: Long): String? {
        if (size <= 0) return "0"
        val units = listOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return "${DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    fun downloadHistoryData(
        context: Context,
        filename: String,
        onHistoryDataDownloaded: (historyData: LocationHistoryData) -> Unit,
        onError: (exception: StorageException) -> Unit,
        onUninitialized: (() -> Unit)? = null
    ) {
        if (isInitialized) {
            Amplify.Storage.downloadFile(
                filename,
                File(context.getExternalFilesDir(null), filename),
                { result ->
                    val historyJson = result.file.readText()
                    onHistoryDataDownloaded(gson.fromJson(historyJson, LocationHistoryData::class.java))
                },
                { error ->
                    Timber.e(error, "Error when downloading S3 file")
                    onError.invoke(error)
                }
            )
        } else {
            onUninitialized?.invoke()
        }
    }

    fun uploadHistoryData(
        context: Context,
        historyData: LocationHistoryData,
        onUninitialized: (() -> Unit)? = null
    ) {
        if (isInitialized) {
            val dateString = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.US).format(Date())
            val filename = "${LOCATION_HISTORY_VERSION}_$dateString"
            File(context.getExternalFilesDir(null), filename).let { fileToUpload ->
                fileToUpload.writeText(gson.toJson(historyData))
                Amplify.Storage.uploadFile(
                    filename,
                    fileToUpload,
                    { result -> Timber.i("Uploaded history data to S3: ${result.key}") },
                    { error -> Timber.e(error, "Error when uploading S3 file") }
                )
            }
        } else {
            onUninitialized?.invoke()
        }
    }
}
