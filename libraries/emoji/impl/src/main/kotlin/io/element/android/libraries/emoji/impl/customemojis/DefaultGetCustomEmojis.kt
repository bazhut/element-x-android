/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.impl.customemojis

import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.core.extensions.mapCatchingExceptions
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.emoji.api.customemojis.CustomEmojiPack
import io.element.android.libraries.emoji.api.customemojis.GetCustomEmojis
import io.element.android.libraries.matrix.api.MatrixClient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The global account data event types holding the personal image pack of the user,
 * see [MSC2545](https://github.com/matrix-org/matrix-spec-proposals/pull/2545).
 * Most clients still write the unstable type, so both are read.
 */
internal val USER_IMAGE_PACK_EVENT_TYPES = listOf("m.image_pack", "im.ponies.user_emotes")

/**
 * Returns the packs of custom emojis of the current user: their personal packs, then the pack shared with every user.
 *
 * An emoji found in several packs is only kept in the first one, since its URL is used as the reaction key.
 * Packs which can't be loaded are skipped, unless none of them can be.
 */
@ContributesBinding(SessionScope::class)
class DefaultGetCustomEmojis(
    private val client: MatrixClient,
    private val sharedEmojiPackSource: SharedEmojiPackSource,
    private val dispatchers: CoroutineDispatchers,
) : GetCustomEmojis {
    override suspend fun invoke(): Result<ImmutableList<CustomEmojiPack>> = withContext(dispatchers.io) {
        val results = USER_IMAGE_PACK_EVENT_TYPES.map { eventType -> client.getAccountData(eventType).parsePack() } +
            sharedEmojiPackSource.fetch().parsePack()
        val failures = results.mapNotNull { it.exceptionOrNull() }
        failures.forEach { Timber.e(it, "Failed to load an image pack") }
        if (failures.size == results.size) {
            return@withContext Result.failure(failures.first())
        }
        val loadedUrls = mutableSetOf<String>()
        val packs = results.mapNotNull { it.getOrNull() }
            .map { pack -> pack.copy(emojis = pack.emojis.filter { loadedUrls.add(it.url) }.toImmutableList()) }
            .filter { it.emojis.isNotEmpty() }
        Result.success(packs.toImmutableList())
    }

    private fun Result<String?>.parsePack(): Result<CustomEmojiPack?> = mapCatchingExceptions { content ->
        content?.let(ImagePackParser::parse)
    }
}
