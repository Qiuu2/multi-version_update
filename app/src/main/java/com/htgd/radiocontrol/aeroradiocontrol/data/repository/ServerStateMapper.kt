package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.dto.ServerStateDto
import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerHealth
import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerState

/**
 * DTO -> domain mapper for the server-state domain.
 *
 * Kept separate from the Repository (like [TerminalMapper]) so the wire->domain
 * translation — especially the [deriveHealth] status derivation — is unit-testable
 * in isolation and swappable if the real `state` value set turns out richer.
 */

/**
 * Maps a [ServerStateDto] to a [ServerState].
 *
 * Unlike a terminal (dropped when it has no id), a server-state row has no
 * identity field to gate on — every field is optional health data. So this never
 * returns null; a fully-empty DTO maps to a ServerState whose [ServerState.health]
 * is [ServerHealth.Offline] (state absent → not serving), with null detail fields.
 * The Repository decides whether an empty `data` array means "no snapshot".
 */
fun ServerStateDto.toServerState(): ServerState = ServerState(
    health = deriveHealth(),
    name = name,
    ip = ip,
    gate = gate,
    connection = connection,
    maxConnection = maxConnection,
    taskCount = taskCount,
    bandwidth = bandwidth,
    ctrlPort = ctrlPort,
    dataPort = dataPort,
)

/**
 * Derives the domain [ServerHealth] from the wire `state` int.
 *
 * documented-assumption (Plan A: pinned by "v3 适配实测验证", real v3 JSON):
 *   - state == null      -> Offline   (server answered but sent no state)
 *   - state == 0         -> Offline   (conservative: not serving)
 *   - state != 0         -> Online    (SDK considers it serving)
 * Should the real value set prove to carry more cases (degraded/maintenance/…),
 * add them here and any unclassifiable value resolves to
 * [ServerHealth.Unknown] (R-003 — never crashes the UI). When confirmed, only
 * this function changes; the domain type and fe's boundary map stay.
 */
fun ServerStateDto.deriveHealth(): ServerHealth = when (state) {
    null, 0 -> ServerHealth.Offline
    else -> ServerHealth.Online
}
