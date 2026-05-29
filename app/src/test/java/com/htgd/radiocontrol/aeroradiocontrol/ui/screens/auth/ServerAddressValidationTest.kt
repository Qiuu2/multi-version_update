package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [validateServerAddress] — the presentation-layer adapter over
 * data-integration's `ServerAddress.parse()` (TASK-AR-005).
 *
 * Parsing correctness itself is data-integration's test surface; here we only
 * assert the adapter's behavior: blank → no error, valid → no error, and each
 * parse failure mapped to the right user-facing message.
 */
class ServerAddressValidationTest {

    @Test
    fun `both fields blank is not an error`() {
        assertNull(validateServerAddress("", ""))
        assertNull(validateServerAddress("   ", "  "))
    }

    @Test
    fun `host with valid port passes`() {
        assertNull(validateServerAddress("192.168.1.100", "8080"))
    }

    @Test
    fun `host only (blank port) passes — parse applies default port`() {
        assertNull(validateServerAddress("192.168.1.100", ""))
    }

    @Test
    fun `non-numeric port maps to digit message`() {
        assertEquals("端口必须是数字", validateServerAddress("192.168.1.100", "abc"))
    }

    @Test
    fun `out-of-range port maps to range message`() {
        assertEquals("端口需在 1–65535 之间", validateServerAddress("192.168.1.100", "70000"))
    }

    @Test
    fun `blank host with a port maps to fill-address message`() {
        // "host" is blank but a port is present → combined ":8080" → blank host.
        assertEquals("请填写服务器地址", validateServerAddress("", "8080"))
    }
}
