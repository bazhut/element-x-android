/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.api.customemojis

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Returns the custom emojis the current user can react with.
 */
fun interface GetCustomEmojis {
    suspend operator fun invoke(): Result<ImmutableList<CustomEmoji>>
}

/**
 * A [GetCustomEmojis] that never returns any custom emoji. Handy for pickers that must only offer
 * unicode emojis (e.g. the user-status picker).
 */
object EmptyGetCustomEmojis : GetCustomEmojis {
    override suspend fun invoke(): Result<ImmutableList<CustomEmoji>> = Result.success(persistentListOf())
}
