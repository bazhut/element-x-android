/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.api.customemojis

import kotlinx.collections.immutable.ImmutableList

/**
 * A pack of custom emojis, as defined by [MSC2545](https://github.com/matrix-org/matrix-spec-proposals/pull/2545).
 *
 * @property name the display name of the pack, if it has one.
 * @property emojis the emojis of the pack that can be used as reactions.
 */
data class CustomEmojiPack(
    val name: String?,
    val emojis: ImmutableList<CustomEmoji>,
)
