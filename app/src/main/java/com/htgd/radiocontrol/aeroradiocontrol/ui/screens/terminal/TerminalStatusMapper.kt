package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus as DomainStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus as UiStatus

/**
 * Maps the data layer's domain [DomainStatus] (sealed, Unknown-tolerant) to the
 * UI's presentation [UiStatus] enum at the ViewModel boundary (TASK-AR-102).
 *
 * Known cases are 1:1. The domain's [DomainStatus.Unknown] (R-003 fallback for
 * a status the mapper couldn't classify — real value set pending OPEN INQ-O-1
 * D-3 / O-2) maps to [UiStatus.Offline]: a neutral, non-actionable grey state,
 * the safest existing UI value (an unknown terminal should not appear actionable
 * as online). The UI enum has no dedicated "未知" value — adding one would change
 * the shared atom `ui/components/atoms/StatusPill.kt`; deferred unless PM/Critic
 * want a distinct affordance (would need a STATUS_UPDATE per STD-SHAREDFILE).
 *
 * The `when` is exhaustive over the sealed type, so a new domain status added
 * upstream is a compile error here — forcing an explicit mapping decision rather
 * than a silent misrender.
 */
fun DomainStatus.toUiStatus(): UiStatus = when (this) {
    DomainStatus.Online  -> UiStatus.Online
    DomainStatus.Offline -> UiStatus.Offline
    DomainStatus.Fault   -> UiStatus.Fault
    DomainStatus.Playing -> UiStatus.Playing
    DomainStatus.Paging  -> UiStatus.Paging
    is DomainStatus.Unknown -> UiStatus.Offline
}
