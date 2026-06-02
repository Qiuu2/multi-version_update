package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.compose.ui.graphics.Color
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroColors

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

/**
 * The 5-state token pair for a terminal tile (PA-14 C-3, Phase C residual).
 *
 *   [fg] — strong tile foreground (dot, badge ring, fault accent stripe)
 *   [soft] — soft tile background (icon plate, status pill bg)
 *
 * The pair is resolved by [tileColors] from the new `c.tile<State>` /
 * `c.tile<State>Soft` v1.3 token aliases — same hexes as the existing
 * `c.status<State>` / `c.status<State>Soft` aliases, just bound to the role
 * "terminal tile state" so a future role-only hex tweak doesn't drag the global
 * status palette along (or vice-versa).
 */
data class TileColors(val fg: Color, val soft: Color)

/**
 * Maps a 5-state UI [TerminalStatus] to its tile color pair, sourcing the new
 * v1.3 `tile*` aliases (one place for the spec→token binding so screens compose
 * with `tileColors(status)` rather than restating the 5-way `when`). Mirrors the
 * pattern used in `StatusPill` for the pill bg/fg.
 *
 *   state    → fg                 / soft
 *   ──────── ──────────────────── ──────────────────────
 *   Online   → colors.tileOnline   / colors.tileOnlineSoft    (#16A34A / #E6F4F2)
 *   Offline  → colors.tileOffline  / colors.tileOfflineSoft   (#8A929F / #EEF0F3)
 *   Fault    → colors.tileFault    / colors.tileFaultSoft     (#DC2626 / #FDECEC)
 *   Playing  → colors.tilePlaying  / colors.tilePlayingSoft   (#2563EB / #E8EFFD)
 *   Paging   → colors.tilePaging   / colors.tilePagingSoft    (#EA580C / #FDEEE2)
 */
fun tileColors(status: TerminalStatus, colors: AeroColors): TileColors = when (status) {
    TerminalStatus.Online  -> TileColors(colors.tileOnline,  colors.tileOnlineSoft)
    TerminalStatus.Offline -> TileColors(colors.tileOffline, colors.tileOfflineSoft)
    TerminalStatus.Fault   -> TileColors(colors.tileFault,   colors.tileFaultSoft)
    TerminalStatus.Playing -> TileColors(colors.tilePlaying, colors.tilePlayingSoft)
    TerminalStatus.Paging  -> TileColors(colors.tilePaging,  colors.tilePagingSoft)
}
