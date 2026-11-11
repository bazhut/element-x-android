/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.emoji.impl.picker

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesBinding
import io.element.android.emojibasebindings.Emoji
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.designsystem.theme.components.IconSource
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.emoji.api.customemojis.CustomEmoji
import io.element.android.libraries.emoji.api.customemojis.GetCustomEmojis
import io.element.android.libraries.emoji.api.picker.EmojiPickerPresenter
import io.element.android.libraries.emoji.api.picker.EmojiPickerState
import io.element.android.libraries.emoji.api.recentemojis.GetRecentEmojis
import io.element.android.libraries.emoji.impl.EmojibaseProvider
import io.element.android.libraries.emoji.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

@AssistedInject
class DefaultEmojiPickerPresenter(
    private val emojibaseProvider: EmojibaseProvider,
    @Assisted private val getRecentEmojis: GetRecentEmojis,
    @Assisted private val getCustomEmojis: GetCustomEmojis,
    private val coroutineDispatchers: CoroutineDispatchers,
) : EmojiPickerPresenter {
    @AssistedFactory
    @ContributesBinding(SessionScope::class)
    interface Factory : EmojiPickerPresenter.Factory {
        override fun create(
            getRecentEmojis: GetRecentEmojis,
            getCustomEmojis: GetCustomEmojis,
        ): DefaultEmojiPickerPresenter
    }

    @Composable
    override fun present(): EmojiPickerState {
        val queryState = rememberTextFieldState()
        var isSearchActive by remember { mutableStateOf(false) }
        var emojiResults by remember { mutableStateOf<SearchBarResultState<ImmutableList<Emoji>>>(SearchBarResultState.Initial) }

        val data by produceState(EmojiPickerData.Empty) {
            val storeDeferred = async { emojibaseProvider.getStore() }
            val recentsDeferred = async { getRecentEmojis().getOrNull() ?: persistentListOf() }
            val customEmojiPacksDeferred = async {
                getCustomEmojis()
                    .onFailure { Timber.e(it, "Failed to load custom emojis") }
                    .getOrNull()
                    .orEmpty()
            }
            val store = storeDeferred.await()
            val recentEmojiUnicodes = recentsDeferred.await()
            val customEmojiPacks = customEmojiPacksDeferred.await()
            value = withContext(coroutineDispatchers.computation) {
                val baseCategories = store.categories.map { (category, emojis) ->
                    EmojiCategory(
                        titleId = category.title,
                        icon = IconSource.Vector(category.icon),
                        emojis = emojis,
                    )
                }
                val customCategories = customEmojiPacks.map { pack ->
                    EmojiCategory(
                        titleId = R.string.emoji_picker_category_custom,
                        title = pack.name,
                        icon = IconSource.Resource(io.element.android.compound.R.drawable.ic_compound_reaction),
                        emojis = pack.emojis.map { it.toPickerEmoji() }.toImmutableList(),
                    )
                }
                val allEmojis = (customCategories.flatMap { it.emojis } + store.allEmojis).toImmutableList()
                // Custom emojis which are not in the user's image pack anymore are not displayed
                val recentEmojis = recentEmojiUnicodes
                    .mapNotNull { unicode -> allEmojis.find { it.unicode == unicode } }
                    .toImmutableList()
                val recentCategory = recentEmojis.takeIf { it.isNotEmpty() }?.let {
                    EmojiCategory(
                        titleId = R.string.emoji_picker_category_recent,
                        icon = IconSource.Resource(io.element.android.compound.R.drawable.ic_compound_history),
                        emojis = it,
                    )
                }
                EmojiPickerData(
                    categories = (listOfNotNull(recentCategory) + customCategories + baseCategories).toImmutableList(),
                    allEmojis = allEmojis,
                )
            }
        }

        val searchQuery = queryState.text.toString()
        LaunchedEffect(searchQuery, data) {
            if (searchQuery.isEmpty() || data.allEmojis.isEmpty()) {
                emojiResults = SearchBarResultState.Initial
                return@LaunchedEffect
            }
            delay(100.milliseconds)
            val lowercaseQuery = searchQuery.lowercase()
            val results = withContext(coroutineDispatchers.computation) {
                data.allEmojis
                    .asSequence()
                    .filter { emoji ->
                        emoji.tags.orEmpty().any { it.contains(lowercaseQuery, ignoreCase = true) } ||
                            emoji.shortcodes.any { it.contains(lowercaseQuery, ignoreCase = true) }
                    }
                    .take(60)
                    .toImmutableList()
            }
            emojiResults = SearchBarResultState.Results(results)
        }

        val isInPreview = LocalInspectionMode.current
        fun handleEvent(event: EmojiPickerEvent) {
            when (event) {
                is EmojiPickerEvent.ToggleSearchActive -> if (!isInPreview) {
                    isSearchActive = event.isActive
                }
            }
        }

        return DefaultEmojiPickerState(
            categories = data.categories,
            searchQuery = queryState,
            isSearchActive = isSearchActive,
            searchResults = emojiResults,
            eventSink = ::handleEvent,
        )
    }
}

private data class EmojiPickerData(
    val categories: ImmutableList<EmojiCategory>,
    val allEmojis: ImmutableList<Emoji>,
) {
    companion object {
        val Empty = EmojiPickerData(persistentListOf(), persistentListOf())
    }
}

/**
 * Maps a [CustomEmoji] to an [Emoji] the picker can display. The `mxc://` URL is stored in [Emoji.unicode], since it's the value
 * used as reaction key, see [io.element.android.libraries.emoji.api.customemojis.isCustom].
 */
internal fun CustomEmoji.toPickerEmoji() = Emoji(
    hexcode = "",
    label = body ?: shortcode,
    tags = body?.let { persistentListOf(it) },
    shortcodes = persistentListOf(shortcode),
    unicode = url,
    skins = null,
)
