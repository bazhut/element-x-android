/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.api.picker

import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.emoji.api.customemojis.GetCustomEmojis
import io.element.android.libraries.emoji.api.recentemojis.GetRecentEmojis

fun interface EmojiPickerPresenter : Presenter<EmojiPickerState> {
    /**
     * Creates a presenter for one picker instance.
     */
    fun interface Factory {
        /**
         * @param getRecentEmojis how the picker sources its recent tab; pass [io.element.android.libraries.emoji.api.recentemojis.EmptyGetRecentEmojis]
         * for a picker that should have no recent tab at all.
         * @param getCustomEmojis how the picker sources its custom emojis tab; pass
         * [io.element.android.libraries.emoji.api.customemojis.EmptyGetCustomEmojis] for a picker that should only offer unicode emojis.
         */
        fun create(
            getRecentEmojis: GetRecentEmojis,
            getCustomEmojis: GetCustomEmojis,
        ): EmojiPickerPresenter
    }
}
