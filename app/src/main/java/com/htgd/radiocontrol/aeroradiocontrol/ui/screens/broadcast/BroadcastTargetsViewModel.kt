package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A selectable broadcast target zone (id + display name). */
data class ZoneTarget(val id: String, val name: String)

/**
 * Supplies the zone list for the broadcast / temp-file-broadcast target pickers
 * (TASK-AR-106). Both screens only need each zone's id + name for selectable
 * chips — not terminal status/counts — so this exposes a thin [ZoneTarget] list
 * from the shared [TerminalRepository] SSOT (same source as the hub/zone-detail).
 *
 * Scope (回执无关): this de-mocks ONLY the target-selection data; the broadcast
 * three-mode logic (page/talk/cast) is untouched and remains a later task.
 *
 * The picker degrades quietly to an empty chip row before the first refresh
 * resolves or when no zones exist — a selection control with no options is not
 * an error state, so no full 5-state machine here.
 */
@HiltViewModel
class BroadcastTargetsViewModel @Inject constructor(
    private val repository: TerminalRepository,
) : ViewModel() {

    val zones: StateFlow<List<ZoneTarget>> =
        repository.observeZones()
            .map { list -> list.map { ZoneTarget(it.id, it.name) } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    init {
        viewModelScope.launch { repository.refresh() }
    }
}
