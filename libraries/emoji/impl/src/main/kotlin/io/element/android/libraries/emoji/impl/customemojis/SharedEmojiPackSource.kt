/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.impl.customemojis

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.element.android.appconfig.EmojiConfig
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.services.toolbox.api.systemclock.SystemClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.time.Duration.Companion.minutes

/**
 * Provides the content of the image pack shared with every user, see [EmojiConfig.SHARED_EMOJI_PACK_URL].
 */
fun interface SharedEmojiPackSource {
    /**
     * Returns the JSON content of the shared image pack, or null if none is configured.
     */
    suspend fun fetch(): Result<String?>
}

private val CACHE_DURATION = 10.minutes

@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class DefaultSharedEmojiPackSource internal constructor(
    private val url: String?,
    private val okHttpClient: () -> OkHttpClient,
    private val systemClock: SystemClock,
    private val dispatchers: CoroutineDispatchers,
) : SharedEmojiPackSource {
    @Inject
    constructor(
        okHttpClient: () -> OkHttpClient,
        systemClock: SystemClock,
        dispatchers: CoroutineDispatchers,
    ) : this(EmojiConfig.SHARED_EMOJI_PACK_URL, okHttpClient, systemClock, dispatchers)

    private val mutex = Mutex()
    private var cachedContent: String? = null
    private var cachedAtMillis = 0L

    override suspend fun fetch(): Result<String?> {
        val url = url ?: return Result.success(null)
        return mutex.withLock {
            val cachedContent = cachedContent
            if (cachedContent != null && systemClock.epochMillis() - cachedAtMillis < CACHE_DURATION.inWholeMilliseconds) {
                return@withLock Result.success(cachedContent)
            }
            download(url)
                .onSuccess { content ->
                    this.cachedContent = content
                    cachedAtMillis = systemClock.epochMillis()
                }
                // Keep offering the last known pack while the server can't be reached
                .recoverCatching { error -> cachedContent ?: throw error }
        }
    }

    private suspend fun download(url: String): Result<String> = withContext(dispatchers.io) {
        runCatchingExceptions {
            val request = Request.Builder().url(url).build()
            okHttpClient().newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Unexpected HTTP status ${response.code} for the shared image pack" }
                response.body.string()
            }
        }
    }
}
