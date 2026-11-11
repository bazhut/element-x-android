/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.impl.customemojis

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.emoji.api.customemojis.CustomEmoji
import io.element.android.libraries.matrix.test.AN_EXCEPTION
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.tests.testutils.lambda.lambdaRecorder
import io.element.android.tests.testutils.lambda.value
import io.element.android.tests.testutils.testCoroutineDispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultGetCustomEmojisTest {
    @Test
    fun `invoke - reads the user image pack from the account data`() = runTest {
        val getAccountDataLambda = lambdaRecorder<String, Result<String?>> {
            Result.success("""{ "images": { "blobcat": { "url": "mxc://example.org/blobcat", "body": "Blob cat" } } }""")
        }
        val getCustomEmojis = createDefaultGetCustomEmojis(getAccountDataLambda)

        val result = getCustomEmojis()

        assertThat(result.getOrThrow()).containsExactly(
            CustomEmoji(shortcode = "blobcat", url = "mxc://example.org/blobcat", body = "Blob cat"),
        )
        getAccountDataLambda.assertions().isCalledOnce().with(value(USER_EMOTES_EVENT_TYPE))
    }

    @Test
    fun `invoke - returns an empty list when the user has no image pack`() = runTest {
        val getCustomEmojis = createDefaultGetCustomEmojis { Result.success(null) }
        assertThat(getCustomEmojis().getOrThrow()).isEmpty()
    }

    @Test
    fun `invoke - returns a failure when the account data can't be read`() = runTest {
        val getCustomEmojis = createDefaultGetCustomEmojis { Result.failure(AN_EXCEPTION) }
        assertThat(getCustomEmojis().exceptionOrNull()).isEqualTo(AN_EXCEPTION)
    }

    @Test
    fun `invoke - returns a failure when the image pack is invalid`() = runTest {
        val getCustomEmojis = createDefaultGetCustomEmojis { Result.success("not json") }
        assertThat(getCustomEmojis().isFailure).isTrue()
    }

    private fun TestScope.createDefaultGetCustomEmojis(
        getAccountDataLambda: (String) -> Result<String?>,
    ) = DefaultGetCustomEmojis(
        client = FakeMatrixClient(getAccountDataLambda = getAccountDataLambda),
        dispatchers = testCoroutineDispatchers(),
    )
}
