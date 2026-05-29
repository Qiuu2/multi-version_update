package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TaskLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plan A [TaskRepository] — STUB (TASK-PA-03b interface-first; real impl is a
 * later PA task).
 *
 * Purpose right now: a compilable, injectable seam so fe-business can write the
 * Task Tab ViewModel against the LIVE ICD-TaskRepository-v1 contract before the v3
 * adapter exists. Every method returns an empty/neutral value and is NOT wired to
 * the v3 RequestManger / V3CallbackAdapter yet.
 *
 * The real impl will mirror [V3TerminalRepository]: V3CallbackAdapter (RequestManger
 * → raw JSON) → Gson into the reverse-engineered SchemeDto → Mapper → domain →
 * in-memory SSOT → Flow; refresh()/setSchemeActive() hitting the v3 task endpoints
 * (e.g. sechenableordisable); status derived per "v3 适配实测验证". Until then, do
 * NOT rely on this returning data.
 */
@Singleton
class V3TaskRepository @Inject constructor() : TaskRepository {

    // STUB — real v3 adapter impl pending (PA task TBD): empty SSOT, never populated.
    private val schemesFlow = MutableStateFlow<List<Scheme>>(emptyList())

    override fun observeSchemes(): Flow<List<Scheme>> =
        // STUB — real v3 adapter impl pending (PA task TBD): always emits emptyList().
        schemesFlow.asStateFlow()

    override suspend fun refresh(): Result<Unit> =
        // STUB — real v3 adapter impl pending (PA task TBD): no fetch, no-op success.
        Result.success(Unit)

    override suspend fun setSchemeActive(schemeId: String, active: Boolean): Result<Unit> =
        // STUB — real v3 adapter impl pending (PA task TBD): no v3 call, no-op success.
        Result.success(Unit)

    override suspend fun getExecutionLog(): Result<List<TaskLog>> =
        // STUB — real v3 adapter impl pending (PA task TBD): empty log.
        Result.success(emptyList())
}
