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
import io.element.android.libraries.emoji.api.customemojis.CustomEmoji
import io.element.android.libraries.emoji.api.customemojis.GetCustomEmojis
import io.element.android.libraries.matrix.api.MatrixClient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.withContext

/**
 * The global account data event type holding the personal image pack of the user,
 * see [MSC2545](https://github.com/matrix-org/matrix-spec-proposals/pull/2545).
 */
internal const val USER_EMOTES_EVENT_TYPE = "im.ponies.user_emotes"

/**
 * Returns the custom emojis from the personal image pack of the current user.
 */
@ContributesBinding(SessionScope::class)
class DefaultGetCustomEmojis(
    private val client: MatrixClient,
    private val dispatchers: CoroutineDispatchers,
) : GetCustomEmojis {
    override suspend fun invoke(): Result<ImmutableList<CustomEmoji>> = withContext(dispatchers.io) {
        client.getAccountData(USER_EMOTES_EVENT_TYPE)
            .mapCatchingExceptions { content ->
                if (content == null) {
                    persistentListOf()
                } else {
                    ImagePackParser.parse(content).toImmutableList()
                }
            }
    }
}
