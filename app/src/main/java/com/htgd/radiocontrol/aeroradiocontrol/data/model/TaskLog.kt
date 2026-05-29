package com.htgd.radiocontrol.aeroradiocontrol.data.model

/**
 * Domain model for one execution-log entry (ICD-TaskRepository-v1).
 *
 * Repository OUTPUT type, fetched one-shot via TaskRepository.getExecutionLog()
 * (v3 has no push / no dedicated log-stream endpoint, so a one-shot fits the
 * polling model). Field shape is a conservative skeleton — the real v3 log
 * source/fields are confirmed at impl time ("v3 适配实测验证"); the endpoint
 * inventory shows no dedicated log endpoint, so it may derive from task info.
 */
data class TaskLog(
    val id: String,
    val taskName: String,
    val timestamp: String,
    val message: String,
)
