package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TaskLog
import kotlinx.coroutines.flow.Flow

/**
 * Data access for 作息 (schedule) schemes and their tasks (ICD-TaskRepository-v1,
 * §13 LIVE).
 *
 * This INTERFACE is the contract fe-business's Task Tab ViewModel depends on,
 * shaped to that screen's rendering need (same seam idiom as [TerminalRepository]).
 * The impl is [V3TaskRepository] — currently a STUB; the real v3 adapter
 * (RequestManger → JSON → reverse-engineered SchemeDto/Mapper → SSOT) lands in a
 * later PA task.
 *
 * Scope (PA-03b): read + start/stop only. CRUD (add/delete/edit time/title/zone)
 * is a follow-up increment → ICD_UPDATE.
 *
 * Status derivation: [Scheme.tasks] carry an Unknown-tolerant SchemeTaskStatus
 * (same pattern as TerminalStatus). The real taskstate/projectstate → status
 * mapping is confirmed at impl time ("v3 适配实测验证"); the UI maps the domain
 * type at the ViewModel boundary and keeps an Unknown branch so it can't break.
 */
interface TaskRepository {

    /** Observe all schemes (each with its nested tasks). Emits the current
     *  snapshot immediately, then re-emits after each successful refresh. */
    fun observeSchemes(): Flow<List<Scheme>>

    /** Fetch schemes+tasks from the v3 stack into the SSOT. Failure = network/
     *  parse error; the previous snapshot stays until a refresh succeeds (same
     *  contract as [TerminalRepository.refresh]). */
    suspend fun refresh(): Result<Unit>

    /** Start/stop one scheme (v3 /task/sechenableordisable). On success the
     *  scheme's `active` flips and is re-emitted through [observeSchemes]. */
    suspend fun setSchemeActive(schemeId: String, active: Boolean): Result<Unit>

    /** Fetch the execution log one-shot (v3 has no push / no dedicated log-stream
     *  endpoint, so a one-shot fits the polling model — fe calls on demand). */
    suspend fun getExecutionLog(): Result<List<TaskLog>>
}
