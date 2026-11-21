/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.impl.customemojis

import com.google.common.truth.Truth.assertThat
import io.element.android.services.toolbox.test.systemclock.FakeSystemClock
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

private const val A_PACK = """{ "images": { "parrot": { "url": "mxc://example.org/parrot" } } }"""

class DefaultSharedEmojiPackSourceTest {
    private lateinit var server: MockWebServer
    private val systemClock = FakeSystemClock(epochMillisResult = 0)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetch - returns null when no shared pack is configured`() = runTest {
        val source = createSource(url = null)
        assertThat(source.fetch().getOrThrow()).isNull()
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `fetch - downloads the pack once and then uses the cache`() = runTest {
        server.enqueue(MockResponse().setBody(A_PACK))
        val source = createSource()

        assertThat(source.fetch().getOrThrow()).isEqualTo(A_PACK)
        systemClock.epochMillisResult = 9.minutes.inWholeMilliseconds
        assertThat(source.fetch().getOrThrow()).isEqualTo(A_PACK)

        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `fetch - downloads the pack again once the cache expired, keeping the last one if the server fails`() = runTest {
        server.enqueue(MockResponse().setBody(A_PACK))
        server.enqueue(MockResponse().setResponseCode(500))
        val source = createSource()

        assertThat(source.fetch().getOrThrow()).isEqualTo(A_PACK)
        systemClock.epochMillisResult = 11.minutes.inWholeMilliseconds
        assertThat(source.fetch().getOrThrow()).isEqualTo(A_PACK)

        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `fetch - returns a failure when the pack can't be downloaded`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val source = createSource()
        assertThat(source.fetch().isFailure).isTrue()
    }

    private fun TestScope.createSource(
        url: String? = server.url("/emoji-pack.json").toString(),
    ) = DefaultSharedEmojiPackSource(
        url = url,
        okHttpClient = { OkHttpClient() },
        systemClock = systemClock,
        dispatchers = testCoroutineDispatchers(),
    )
}
