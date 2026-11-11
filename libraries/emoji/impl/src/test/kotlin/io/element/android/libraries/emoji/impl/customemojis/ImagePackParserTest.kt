/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.impl.customemojis

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.emoji.api.customemojis.CustomEmoji
import org.junit.Assert.assertThrows
import org.junit.Test

class ImagePackParserTest {
    @Test
    fun `parse - returns the images with their shortcode, url and body`() {
        val content = """
            {
              "images": {
                "party_parrot": { "url": "mxc://example.org/parrot", "body": "Party parrot" },
                "blobcat": { "url": "mxc://example.org/blobcat" }
              }
            }
        """.trimIndent()
        assertThat(ImagePackParser.parse(content)).containsExactly(
            CustomEmoji(shortcode = "party_parrot", url = "mxc://example.org/parrot", body = "Party parrot"),
            CustomEmoji(shortcode = "blobcat", url = "mxc://example.org/blobcat", body = null),
        ).inOrder()
    }

    @Test
    fun `parse - returns an empty list when there are no images`() {
        assertThat(ImagePackParser.parse("""{ "pack": { "display_name": "My pack" } }""")).isEmpty()
    }

    @Test
    fun `parse - ignores images without a valid mxc url`() {
        val content = """
            {
              "images": {
                "no_url": { "body": "No url" },
                "http_url": { "url": "https://example.org/image.png" },
                "not_a_string": { "url": 42 },
                "not_an_object": "mxc://example.org/string",
                "valid": { "url": "mxc://example.org/valid" }
              }
            }
        """.trimIndent()
        assertThat(ImagePackParser.parse(content).map { it.shortcode }).containsExactly("valid")
    }

    @Test
    fun `parse - only keeps images usable as emoticons`() {
        val content = """
            {
              "pack": { "usage": ["sticker"] },
              "images": {
                "pack_usage": { "url": "mxc://example.org/1" },
                "sticker": { "url": "mxc://example.org/2", "usage": ["sticker"] },
                "emoticon": { "url": "mxc://example.org/3", "usage": ["emoticon"] },
                "both": { "url": "mxc://example.org/4", "usage": ["sticker", "emoticon"] }
              }
            }
        """.trimIndent()
        assertThat(ImagePackParser.parse(content).map { it.shortcode }).containsExactly("emoticon", "both").inOrder()
    }

    @Test
    fun `parse - absent or empty usage means all usages`() {
        val content = """
            {
              "pack": { "usage": [] },
              "images": {
                "no_usage": { "url": "mxc://example.org/1" },
                "empty_usage": { "url": "mxc://example.org/2", "usage": [] }
              }
            }
        """.trimIndent()
        assertThat(ImagePackParser.parse(content).map { it.shortcode }).containsExactly("no_usage", "empty_usage").inOrder()
    }

    @Test
    fun `parse - ignores images reusing an already used url`() {
        val content = """
            {
              "images": {
                "first": { "url": "mxc://example.org/same" },
                "second": { "url": "mxc://example.org/same" }
              }
            }
        """.trimIndent()
        assertThat(ImagePackParser.parse(content).map { it.shortcode }).containsExactly("first")
    }

    @Test
    fun `parse - throws when the content is not a json object`() {
        assertThrows(IllegalArgumentException::class.java) { ImagePackParser.parse("[]") }
        assertThrows(IllegalArgumentException::class.java) { ImagePackParser.parse("not json") }
    }
}
