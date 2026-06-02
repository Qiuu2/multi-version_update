package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerState
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the server health snapshot (ICD-ServerStateRepository-v1,
 * GET /server/serverstate).
 *
 * This INTERFACE is the contract fe-business's Service Tab (系统健康度) ViewModel
 * depends on. Single-object shape (not a list): the endpoint reports one server.
 *
 *   observeServerState(): Flow<ServerState?>  — the screen observes; emits the
 *                                               current snapshot, starting at
 *                                               `null` (no data yet), re-emitting
 *                                               after each successful refresh.
 *   suspend refresh(): Result<Unit>           — fetches into the SSOT;
 *                                               success/failure drives the
 *                                               screen's loading/error/retry.
 *
 * SSOT contract (identical to [TerminalRepository]): start null, refresh-success
 * publishes the new snapshot, refresh-FAILURE keeps the last snapshot (caller
 * drives retry from Result.failure). The SSOT is in-memory (cleared on process
 * death); a Room cache could replace the backing Flow later without changing this
 * surface.
 */
interface ServerStateRepository {

    /** Observe the server-state snapshot. Emits `null` until the first successful
     *  refresh, then the latest snapshot, re-emitting on each success. */
    fun observeServerState(): Flow<ServerState?>

    /** Fetch the server state into the SSOT. Failure = network/parse error or an
     *  empty `data` array; the previous snapshot stays until a refresh succeeds. */
    suspend fun refresh(): Result<Unit>
}
