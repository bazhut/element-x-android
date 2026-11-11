/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.api.customemojis

/**
 * A custom emoji, i.e. an image from an image pack (see [MSC2545](https://github.com/matrix-org/matrix-spec-proposals/pull/2545)).
 *
 * Reacting with a custom emoji uses its [url] as the reaction key
 * (see [MSC4027](https://github.com/matrix-org/matrix-spec-proposals/pull/4027)).
 *
 * @property shortcode the shortcode of the emoji in its pack, without the surrounding colons.
 * @property url the `mxc://` URI of the image.
 * @property body an optional human readable description of the image.
 */
data class CustomEmoji(
    val shortcode: String,
    val url: String,
    val body: String? = null,
)
