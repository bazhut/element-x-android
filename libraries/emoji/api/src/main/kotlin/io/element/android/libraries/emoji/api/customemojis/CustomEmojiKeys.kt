/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.api.customemojis

import io.element.android.emojibasebindings.Emoji

private const val MXC_PREFIX = "mxc://"

/**
 * Whether this reaction key references a custom emoji, i.e. it is an `mxc://` URI
 * (see [MSC4027](https://github.com/matrix-org/matrix-spec-proposals/pull/4027)).
 */
fun String.isCustomEmojiKey(): Boolean = startsWith(MXC_PREFIX)

/**
 * Whether this picker [Emoji] is a custom emoji. For custom emojis, [Emoji.unicode] holds the `mxc://` URI
 * of the image, which is also the reaction key to send, and [Emoji.shortcodes] holds its shortcode.
 */
val Emoji.isCustom: Boolean
    get() = unicode.isCustomEmojiKey()

/**
 * A textual representation of this [Emoji], suitable for accessibility labels:
 * the emoji itself for unicode emojis, `:shortcode:` for custom emojis.
 */
val Emoji.displayText: String
    get() = if (isCustom) {
        shortcodes.firstOrNull()?.let { ":$it:" } ?: label
    } else {
        unicode
    }
