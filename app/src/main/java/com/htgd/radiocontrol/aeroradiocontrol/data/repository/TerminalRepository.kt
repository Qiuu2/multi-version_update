package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import kotlinx.coroutines.flow.Flow

/**
 * Data access for terminals and zones (ICD-Endpoints-v1 GET /terminal endpoints).
 *
 * This INTERFACE is the contract fe-business's TerminalHub ViewModel (AR-102)
 * and ZoneDetail (AR-105) depend on. Shaped to fe's actual rendering need:
 *
 *   observeZones(): Flow<List<Zone>>   — the screen observes; each Zone carries
 *                                        its terminals (nested, per fe). The
 *                                        Flow re-emits whenever refresh() updates
 *                                        the in-memory source of truth.
 *   suspend refresh(): Result<Unit>    — fetches from network into the SSOT;
 *                                        success/failure drives the screen's
 *                                        loading / error / retry states.
 *
 * Derived counts (per-zone online/fault, global fault) are the UI's job — not
 * exposed here.
 *
 * Partial semantics (PM Q3): this repo does NOT distinguish "fresh success" from
 * "served stale cache but refresh failed" — there is no persistent cache layer
 * yet (the SSOT is in-memory, cleared on process death). So no Partial signal is
 * exposed; fe binds the Partial UI state to fe-platform's realtime
 * connectionState later (AR-102 keeps a Partial placeholder for now). When a
 * Room cache lands, this interface can gain a richer result type → ICD_UPDATE.
 *
 * (`observeZones` is a hot Flow over an in-memory MutableStateFlow today; a Room
 * Flow is a drop-in replacement later without changing this surface.)
 */
interface TerminalRepository {

    /** Observe all zones (each with its nested terminals). Emits the current
     *  snapshot immediately, then re-emits after each successful refresh. */
    fun observeZones(): Flow<List<Zone>>

    /** Observe the flat terminal list (for screens that want terminals, not
     *  grouped by zone). Same SSOT as observeZones. */
    fun observeTerminals(): Flow<List<Terminal>>

    /** Fetch zones+terminals from the network into the SSOT. Failure = network/
     *  parse error; the previous snapshot stays until a refresh succeeds. */
    suspend fun refresh(): Result<Unit>
}
