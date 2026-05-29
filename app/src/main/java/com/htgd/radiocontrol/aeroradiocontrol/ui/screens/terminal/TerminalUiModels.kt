package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus

/**
 * Presentation view models for the terminal Tab (TASK-AR-102 / AR-105).
 *
 * The ViewModels map the data layer's domain models (`data.model.Zone` /
 * `data.model.Terminal` / sealed `data.model.TerminalStatus`) into these UI
 * types, so screens never touch data DTOs/domain types and the per-zone counts
 * are computed here at the presentation layer (fe owns the count rules).
 *
 * Extracted from the old TerminalMockData so the de-mocked screens depend on real
 * UI models, not the mock file (deleted in AR-105).
 */
data class TerminalUi(
    val id: String,
    val name: String,
    val status: TerminalStatus,
)

data class ZoneUi(
    val id: String,
    val name: String,
    val terminals: List<TerminalUi>,
) {
    /** Online = anything not explicitly offline (paging/playing/fault count as reachable). */
    val onlineCount: Int get() = terminals.count { it.status != TerminalStatus.Offline }
    val faultCount: Int get() = terminals.count { it.status == TerminalStatus.Fault }
}

/**
 * Domain [Zone] → presentation [ZoneUi], mapping each terminal's domain status to
 * the UI enum (Unknown → safe fallback via [toUiStatus]). Shared by the hub and
 * zone-detail ViewModels so the projection lives in one place.
 */
fun Zone.toZoneUi(): ZoneUi = ZoneUi(
    id = id,
    name = name,
    terminals = terminals.map { t ->
        TerminalUi(id = t.id, name = t.name, status = t.status.toUiStatus())
    },
)
