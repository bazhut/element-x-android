/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.appconfig

object EmojiConfig {
    /**
     * The URL of an image pack, in the [MSC2545](https://github.com/matrix-org/matrix-spec-proposals/pull/2545) format,
     * whose custom emojis are offered to every user as reactions. Null to disable it.
     *
     * Set at build time by the `ELEMENT_ANDROID_SHARED_EMOJI_PACK_URL` environment variable
     * or the `services.emoji.shared_pack_url` entry of `local.properties`.
     */
    val SHARED_EMOJI_PACK_URL: String? = BuildConfig.SHARED_EMOJI_PACK_URL?.takeIf { it.isNotBlank() }
}
