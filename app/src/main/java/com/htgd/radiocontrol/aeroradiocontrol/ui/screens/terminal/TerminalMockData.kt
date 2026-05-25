package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus

/** Mock domain for the terminal hub. Replaced by repository-backed data in a later phase. */
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
    val onlineCount: Int get() = terminals.count { it.status != TerminalStatus.Offline }
    val faultCount: Int get() = terminals.count { it.status == TerminalStatus.Fault }
}

object TerminalMock {
    val zones: List<ZoneUi> = listOf(
        ZoneUi(
            id = "z1", name = "教学楼 A",
            terminals = listOf(
                TerminalUi("z1-1", "A101", TerminalStatus.Online),
                TerminalUi("z1-2", "A102", TerminalStatus.Playing),
                TerminalUi("z1-3", "A103", TerminalStatus.Fault),
                TerminalUi("z1-4", "A104", TerminalStatus.Offline),
            ),
        ),
        ZoneUi(
            id = "z2", name = "运动场",
            terminals = listOf(
                TerminalUi("z2-1", "东看台", TerminalStatus.Online),
                TerminalUi("z2-2", "西看台", TerminalStatus.Paging),
                TerminalUi("z2-3", "主席台", TerminalStatus.Online),
            ),
        ),
        ZoneUi(
            id = "z3", name = "宿舍区",
            terminals = listOf(
                TerminalUi("z3-1", "1号楼", TerminalStatus.Offline),
                TerminalUi("z3-2", "2号楼", TerminalStatus.Fault),
                TerminalUi("z3-3", "3号楼", TerminalStatus.Online),
                TerminalUi("z3-4", "4号楼", TerminalStatus.Online),
            ),
        ),
    )

    fun zone(id: String): ZoneUi? = zones.firstOrNull { it.id == id }

    /** All fault terminals across every zone, for the pinned top banner/section. */
    fun faultTerminals(): List<TerminalUi> =
        zones.flatMap { it.terminals }.filter { it.status == TerminalStatus.Fault }
}
