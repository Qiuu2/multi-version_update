package com.htgd.radiocontrol.aeroradiocontrol.data.auth

/**
 * A parsed server endpoint: host + port, no scheme.
 *
 * Why a value type instead of a raw String?
 *   The base-URL interceptor (TASK-AR-002) needs host and port separately to
 *   rebuild the request URL. Parsing once at the login screen — and failing
 *   loudly there — is far better than letting a malformed "192.168.1.1:abc"
 *   reach OkHttp and blow up mid-request on a background thread.
 *
 * Scheme is intentionally absent: this deployment is plaintext HTTP over a
 * campus LAN (a known, documented constraint). The interceptor hardcodes
 * `http`. If TLS ever lands, scheme moves here.
 */
data class ServerAddress(
    val host: String,
    val port: Int,
) {
    companion object {

        /** Default HTTP port when the user types only a host. */
        const val DEFAULT_PORT = 80

        /**
         * Parses "host" or "host:port" into a [ServerAddress].
         *
         * Returns [Result.failure] on blank host or out-of-range port rather
         * than throwing, so the login screen can `.fold { }` it into a field
         * validation error. We do NOT validate that the host is reachable —
         * that is the ping check's job (HealthRepository), not parsing's.
         */
        fun parse(input: String): Result<ServerAddress> = runCatching {
            val trimmed = input.trim()
            require(trimmed.isNotBlank()) { "Server address is blank" }

            // Split on the last ':' so IPv4 "host:port" parses; bare hosts get
            // the default port. (IPv6 literals are not supported — this LAN is
            // IPv4-only; revisit if that changes.)
            val sepIndex = trimmed.lastIndexOf(':')
            val host: String
            val port: Int
            if (sepIndex == -1) {
                host = trimmed
                port = DEFAULT_PORT
            } else {
                host = trimmed.substring(0, sepIndex)
                port = trimmed.substring(sepIndex + 1).toIntOrNull()
                    ?: throw IllegalArgumentException("Port is not a number")
            }

            require(host.isNotBlank()) { "Host is blank" }
            require(port in 1..65535) { "Port out of range: $port" }
            ServerAddress(host, port)
        }
    }
}
