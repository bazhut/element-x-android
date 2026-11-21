/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.impl.customemojis

import io.element.android.libraries.emoji.api.customemojis.CustomEmoji
import io.element.android.libraries.emoji.api.customemojis.CustomEmojiPack
import io.element.android.libraries.emoji.api.customemojis.isCustomEmojiKey
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val USAGE_EMOTICON = "emoticon"

/**
 * Parses the content of an image pack event, as defined by [MSC2545](https://github.com/matrix-org/matrix-spec-proposals/pull/2545).
 *
 * Only images that can be used as emoticons are kept: images whose `usage` (or, if absent, the pack `usage`) contains `emoticon`,
 * or doesn't specify any usage at all. Images without a valid `mxc://` URL are ignored, as well as images reusing an URL
 * already used by a previous image, since the URL is used as the reaction key.
 */
internal object ImagePackParser {
    /**
     * @throws IllegalArgumentException if [content] is not a valid JSON object.
     */
    fun parse(content: String): CustomEmojiPack {
        val root = requireNotNull(Json.parseToJsonElement(content) as? JsonObject) { "Image pack content is not a JSON object" }
        val pack = root["pack"] as? JsonObject
        return CustomEmojiPack(
            name = pack?.get("display_name").stringOrNull()?.takeIf { it.isNotBlank() },
            emojis = parseEmojis(root["images"] as? JsonObject, packUsage = pack?.usage()).toImmutableList(),
        )
    }

    private fun parseEmojis(images: JsonObject?, packUsage: List<String>?): List<CustomEmoji> {
        if (images == null) return emptyList()
        return images.mapNotNull { (shortcode, element) ->
            val image = element as? JsonObject ?: return@mapNotNull null
            val url = image["url"].stringOrNull()?.takeIf { it.isCustomEmojiKey() } ?: return@mapNotNull null
            val usage = image.usage() ?: packUsage
            if (shortcode.isBlank() || (usage != null && USAGE_EMOTICON !in usage)) return@mapNotNull null
            CustomEmoji(
                shortcode = shortcode,
                url = url,
                body = image["body"].stringOrNull()?.takeIf { it.isNotBlank() },
            )
        }.distinctBy { it.url }
    }

    /**
     * Returns the `usage` of an image or a pack, or null if it is absent or empty, which means "all usages" according to the MSC.
     */
    private fun JsonObject.usage(): List<String>? {
        return (this["usage"] as? JsonArray)
            ?.mapNotNull { it.stringOrNull() }
            ?.takeIf { it.isNotEmpty() }
    }

    private fun JsonElement?.stringOrNull(): String? {
        return (this as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
}
