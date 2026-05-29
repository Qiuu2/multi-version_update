package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Media
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.MediaRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.OnDemandCastAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the broadcast Tab 点播 (Cast) mode — broadcast SD1.
 *
 * Composes three seams (cast-seam-contract.md):
 *   - [MediaRepository]  — the media library to pick from (real v3 list).
 *   - [OnDemandCastAdapter] — the native cast action (AAR; one-shot Result, NO mic).
 *   - [TerminalRepository] — to resolve selected ZONE targets → TERMINAL ids, since
 *     the cast adapter takes terminal ids (List<Int>) but the shared target picker
 *     (BroadcastTargetsViewModel) selects zones (see [castMedia]).
 *
 * Gate order (NO RECORD_AUDIO — 点播 is server-side playback, not mic capture):
 *   isAvailable() → Ready/Unavailable; on cast: isAvailable() (adapter re-gates) →
 *   castMedia Result → [CastResult] banner. Does NOT register a CallBackIntf (the
 *   adapter owns that; cast confirmation is the synchronous Result, not a Flow).
 *
 * No polling: a media library is not live state — refresh once on open + manual
 * retry, unlike the status Tabs.
 */
@HiltViewModel
class CastViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    terminalRepository: TerminalRepository,
    private val castAdapter: OnDemandCastAdapter,
) : ViewModel() {

    /** Shared zone→terminal resolver (Option A); identical logic to the voice VM. */
    private val targetResolver = BroadcastTargetResolver(terminalRepository)

    /** null = library refresh not yet resolved (Loading); success/failure thereafter. */
    private val refreshResult = MutableStateFlow<Result<Unit>?>(null)

    /** One-shot cast outcome for a banner; null = nothing to show. Cleared by [consumeResult]. */
    private val _castResult = MutableStateFlow<CastResult?>(null)
    val castResult: StateFlow<CastResult?> = _castResult

    val uiState: StateFlow<CastUiState> =
        if (!castAdapter.isAvailable()) {
            // Capability gate fails closed: no point loading the library.
            MutableStateFlow<CastUiState>(CastUiState.Unavailable)
        } else {
            combine(mediaRepository.observeMedia(), refreshResult) { media, refresh ->
                deriveState(media, refresh)
            }.let { it }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = if (castAdapter.isAvailable()) CastUiState.Loading else CastUiState.Unavailable,
        )

    init {
        if (castAdapter.isAvailable()) refresh()
    }

    /** Re-fetch the media library into the SSOT (open + retry CTA). */
    fun refresh() {
        viewModelScope.launch {
            refreshResult.value = mediaRepository.refresh()
        }
    }

    /**
     * Casts [selectedMediaIds] to the terminals under [selectedZoneIds].
     *
     * Resolves zones → terminal ids: reads the TerminalRepository SSOT, flattens the
     * selected zones' terminals, parses each id to Int (defensively dropping any
     * non-numeric id — documented-assumption: v3 terminal ids are numeric, ESC for
     * PM). Empty media / empty resolved targets short-circuit to a Failure banner so
     * the native call never fires with nothing to do.
     */
    fun castMedia(selectedMediaIds: Set<String>, selectedZoneIds: Set<String>) {
        viewModelScope.launch {
            val mediaIds = selectedMediaIds.mapNotNull { it.toIntOrNull() }
            if (mediaIds.isEmpty()) {
                _castResult.value = CastResult.Failure("请选择要播放的音频")
                return@launch
            }
            val terminalIds = targetResolver.resolveTerminalIds(selectedZoneIds)
            if (terminalIds.isEmpty()) {
                _castResult.value = CastResult.Failure("请选择目标终端")
                return@launch
            }
            castAdapter.castMedia(mediaIds, terminalIds)
                .onSuccess { _castResult.value = CastResult.Success }
                .onFailure { _castResult.value = CastResult.Failure(it.message ?: "点播失败") }
        }
    }

    /** Stops the current cast (best-effort). */
    fun stopCast() {
        viewModelScope.launch { castAdapter.stopCast() }
    }

    /** Clears the one-shot [castResult] after the screen has shown it. */
    fun consumeResult() {
        _castResult.value = null
    }

    private fun deriveState(media: List<Media>, refresh: Result<Unit>?): CastUiState {
        val mediaUis = media.map { it.toMediaUi() }
        return when {
            mediaUis.isNotEmpty() -> CastUiState.Ready(mediaUis)
            refresh == null -> CastUiState.Loading
            refresh.isFailure -> CastUiState.Error(refresh.exceptionOrNull()?.message ?: "加载媒体库失败")
            // refresh succeeded but library is empty — a picker with no options is a
            // valid (non-error) Ready(emptyList), not Error.
            else -> CastUiState.Ready(emptyList())
        }
    }
}
