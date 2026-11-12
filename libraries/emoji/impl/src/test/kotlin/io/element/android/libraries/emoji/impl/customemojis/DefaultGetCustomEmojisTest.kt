/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.impl.customemojis

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.emoji.api.customemojis.CustomEmoji
import io.element.android.libraries.emoji.api.customemojis.CustomEmojiPack
import io.element.android.libraries.matrix.test.AN_EXCEPTION
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.lambda.value
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultGetCustomEmojisTest {
    @Test
    fun `invoke - reads the user image packs from both account data types`() = runTest {
        val getAccountDataLambda = lambdaRecorder<String, Result<String?>> { eventType ->
            when (eventType) {
                "m.image_pack" -> Result.success("""{ "pack": { "display_name": "Stable" }, "images": { "a": { "url": "mxc://example.org/a" } } }""")
                else -> Result.success("""{ "images": { "blobcat": { "url": "mxc://example.org/blobcat", "body": "Blob cat" } } }""")
            }
        }
        val getCustomEmojis = createDefaultGetCustomEmojis(getAccountDataLambda)

        val result = getCustomEmojis()

        assertThat(result.getOrThrow()).containsExactly(
            CustomEmojiPack(name = "Stable", emojis = persistentListOf(CustomEmoji(shortcode = "a", url = "mxc://example.org/a"))),
            CustomEmojiPack(name = null, emojis = persistentListOf(CustomEmoji(shortcode = "blobcat", url = "mxc://example.org/blobcat", body = "Blob cat"))),
        ).inOrder()
        getAccountDataLambda.assertions().isCalledExactly(2).withSequence(
            listOf(value("m.image_pack")),
            listOf(value("im.ponies.user_emotes")),
        )
    }

    @Test
    fun `invoke - the shared pack comes after the personal ones`() = runTest {
        val getCustomEmojis = createDefaultGetCustomEmojis(
            getAccountDataLambda = { eventType ->
                Result.success(if (eventType == "im.ponies.user_emotes") """{ "images": { "mine": { "url": "mxc://example.org/mine" } } }""" else null)
            },
            sharedEmojiPackSource = {
                Result.success("""{ "pack": { "display_name": "Team" }, "images": { "team": { "url": "mxc://example.org/team" } } }""")
            },
        )

        val packs = getCustomEmojis().getOrThrow()

        assertThat(packs.map { it.name }).containsExactly(null, "Team").inOrder()
        assertThat(packs.last().emojis.map { it.url }).containsExactly("mxc://example.org/team")
    }

    @Test
    fun `invoke - an emoji found in several packs is only kept in the first one`() = runTest {
        val pack = """{ "images": { "parrot": { "url": "mxc://example.org/parrot" }, "other": { "url": "mxc://example.org/other" } } }"""
        val getCustomEmojis = createDefaultGetCustomEmojis(
            getAccountDataLambda = { eventType -> Result.success(if (eventType == "m.image_pack") pack else null) },
            sharedEmojiPackSource = { Result.success("""{ "images": { "parrot": { "url": "mxc://example.org/parrot" } } }""") },
        )

        val packs = getCustomEmojis().getOrThrow()

        assertThat(packs).hasSize(1)
        assertThat(packs.single().emojis.map { it.shortcode }).containsExactly("parrot", "other").inOrder()
    }

    @Test
    fun `invoke - returns an empty list when the user has no image pack`() = runTest {
        val getCustomEmojis = createDefaultGetCustomEmojis { Result.success(null) }
        assertThat(getCustomEmojis().getOrThrow()).isEmpty()
    }

    @Test
    fun `invoke - a pack which can't be loaded doesn't prevent loading the others`() = runTest {
        val getCustomEmojis = createDefaultGetCustomEmojis(
            getAccountDataLambda = { Result.failure(AN_EXCEPTION) },
            sharedEmojiPackSource = { Result.success("""{ "images": { "team": { "url": "mxc://example.org/team" } } }""") },
        )
        assertThat(getCustomEmojis().getOrThrow().single().emojis.map { it.shortcode }).containsExactly("team")
    }

    @Test
    fun `invoke - returns a failure when no pack can be loaded`() = runTest {
        val getCustomEmojis = createDefaultGetCustomEmojis(
            getAccountDataLambda = { Result.failure(AN_EXCEPTION) },
            sharedEmojiPackSource = { Result.failure(AN_EXCEPTION) },
        )
        assertThat(getCustomEmojis().exceptionOrNull()).isEqualTo(AN_EXCEPTION)
    }

    @Test
    fun `invoke - returns a failure when every image pack is invalid`() = runTest {
        val getCustomEmojis = createDefaultGetCustomEmojis(
            getAccountDataLambda = { Result.success("not json") },
            sharedEmojiPackSource = { Result.success("not json") },
        )
        assertThat(getCustomEmojis().isFailure).isTrue()
    }

    private fun TestScope.createDefaultGetCustomEmojis(
        getAccountDataLambda: (String) -> Result<String?>,
        sharedEmojiPackSource: SharedEmojiPackSource = SharedEmojiPackSource { Result.success(null) },
    ) = DefaultGetCustomEmojis(
        client = FakeMatrixClient(getAccountDataLambda = getAccountDataLambda),
        sharedEmojiPackSource = sharedEmojiPackSource,
        dispatchers = testCoroutineDispatchers(),
    )
}
