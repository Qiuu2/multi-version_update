package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.ServerAddress

/**
 * Presentation-layer adapter from data-integration's [ServerAddress.parse] to
 * the `validateServer: (ip, port) -> String?` hook [LoginScreen] consumes
 * (TASK-AR-005; ICD-AuthState consumption approved by PM 2026-05-27).
 *
 * We do NOT reimplement parsing — [ServerAddress.parse] is the single source of
 * truth (owned by data-integration, unit-tested there). This only maps its
 * [Result.failure] into a user-facing Chinese message and adapts the two-field
 * (ip / port) login form into the single "host:port" string parse expects.
 *
 * Returns null when the address is valid (or when both fields are blank — an
 * empty form is an idle state, not a validation error; emptiness is enforced by
 * the submit button's enablement, not here).
 */
fun validateServerAddress(ip: String, port: String): String? {
    val host = ip.trim()
    val portText = port.trim()
    if (host.isEmpty() && portText.isEmpty()) return null

    val combined = if (portText.isEmpty()) host else "$host:$portText"
    return ServerAddress.parse(combined).fold(
        onSuccess = { null },
        onFailure = { messageFor(it) },
    )
}

/**
 * Maps a [ServerAddress.parse] failure to a field-level message. Parse throws
 * [IllegalArgumentException] (via `require`/explicit throw) with an English
 * developer string; we translate to the user-facing copy rather than leaking it.
 */
private fun messageFor(cause: Throwable): String = when {
    cause.message?.contains("Port is not a number", ignoreCase = true) == true -> "端口必须是数字"
    cause.message?.contains("Port out of range", ignoreCase = true) == true -> "端口需在 1–65535 之间"
    cause.message?.contains("Host is blank", ignoreCase = true) == true -> "请填写服务器地址"
    cause.message?.contains("blank", ignoreCase = true) == true -> "请填写服务器地址"
    else -> "服务器地址格式不正确"
}
