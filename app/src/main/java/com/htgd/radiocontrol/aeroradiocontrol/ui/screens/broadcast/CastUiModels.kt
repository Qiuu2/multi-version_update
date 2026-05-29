package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import androidx.compose.runtime.Immutable

/**
 * Presentation view types for the broadcast Tab 点播 (Cast) mode (broadcast SD1).
 *
 * The Cast ViewModel maps the data layer's [com.htgd.radiocontrol.aeroradiocontrol.data.model.Media]
 * into these UI types at its boundary (see CastUiMapper.kt), so the screen never
 * touches domain/DTO types. Mirrors the terminal/task/service UiModels split.
 */

/** A selectable media file in the 点播 picker. [duration] is preformatted ("03:20"
 *  or "--"); [id] stays a String for selection (parsed to Int only at cast time). */
@Immutable
data class MediaUi(
    val id: String,
    val name: String,
    val duration: String,
)

/**
 * 点播 mode state. The media picker degrades quietly to an empty list (a picker
 * with no options is not an error), so the list itself has no 5-state machine; the
 * cast *action* outcome is carried by [CastResult].
 */
@Immutable
sealed interface CastUiState {
    /** Library not yet loaded (first refresh unresolved). */
    data object Loading : CastUiState
    /** Library load failed with nothing to show → retry. */
    data class Error(val message: String) : CastUiState
    /** Media available to pick (possibly empty — empty is a valid, non-error list). */
    data class Ready(val media: List<MediaUi>) : CastUiState
    /** This device can't run the native cast path (isAvailable()=false). */
    data object Unavailable : CastUiState
}

/** One-shot result of a [castMedia] attempt, surfaced to the screen as a banner. */
sealed interface CastResult {
    /** Cast started OK on the server. */
    data object Success : CastResult
    /** Cast failed; [message] explains (native error / unavailable / no targets). */
    data class Failure(val message: String) : CastResult
}
