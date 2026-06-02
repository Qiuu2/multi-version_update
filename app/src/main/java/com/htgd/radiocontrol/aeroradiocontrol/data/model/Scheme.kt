package com.htgd.radiocontrol.aeroradiocontrol.data.model

/**
 * Domain model for a 作息 (schedule) scheme — a named program of timed tasks, as
 * the Task Tab consumes it (ICD-TaskRepository-v1).
 *
 * Repository OUTPUT type. [tasks] is nested (consistent with [Zone] nesting its
 * terminals — fe's preference, saves a combine in the ViewModel). [active] is
 * whether the scheme is currently running (derived from the v3 projectstate).
 *
 * Contract source: the LIVE reverse-engineered SchemeDto (v3 `TaskZuoxiModel`).
 */
data class Scheme(
    val id: String,
    val name: String,
    val active: Boolean,
    val tasks: List<SchemeTask> = emptyList(),
)
