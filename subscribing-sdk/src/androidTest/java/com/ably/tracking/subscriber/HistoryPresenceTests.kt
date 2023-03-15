@file:OptIn(ExperimentalCoroutinesApi::class)

package com.ably.tracking.subscriber

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ably.tracking.common.Ably
import com.ably.tracking.common.DefaultAbly
import com.ably.tracking.common.DefaultAblySdkFactory
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.test.android.common.testLogD
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class HistoryPresenceTests {


    val CLIENT_ID = "IntegrationTestsClient"
    val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

    @Test
    fun createAndStartPublisherAndSubscriberAndWaitUntilDataEnds() = runTest {
        // given
        val ably = getAblyInstance()
        val startConnectionResult = ably.startConnection()
        assertThat(startConnectionResult.isSuccess).isTrue()

        // when
        val history = ably.getRecentPresenceHistory("pizza", 5 * 60 * 60 * 1000).getOrNull()!!
        testLogD(history.toString())

        assertThat(history).isNotNull()
        // then
    }

    private fun getAblyInstance(): Ably =
        DefaultAbly(
            DefaultAblySdkFactory(),
            ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY)),
            Logging.aatDebugLogger,
            CoroutineScope(Dispatchers.IO + SupervisorJob())
        )
}
