package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [LoginUiState] — the AR-005 pre-research presentation model.
 *
 * Pure data/logic only; the Composable rendering is covered by Compose UI tests
 * on-device (this host cannot run them).
 */
class LoginUiStateTest {

    @Test
    fun `default Idle state has no errors and is not submitting`() {
        val s = LoginUiState.Idle
        assertEquals(LoginUiState.Phase.Idle, s.phase)
        assertFalse(s.isSubmitting)
        assertNull(s.formError)
        assertFalse(s.fieldErrors.hasAny)
    }

    @Test
    fun `isSubmitting reflects the Submitting phase`() {
        assertTrue(LoginUiState(phase = LoginUiState.Phase.Submitting).isSubmitting)
        assertFalse(LoginUiState(phase = LoginUiState.Phase.Success).isSubmitting)
    }

    @Test
    fun `hasAny is true when any single field error is set`() {
        assertTrue(LoginUiState.FieldErrors(account = "必填").hasAny)
        assertTrue(LoginUiState.FieldErrors(password = "必填").hasAny)
        assertTrue(LoginUiState.FieldErrors(server = "端口非法").hasAny)
        assertFalse(LoginUiState.FieldErrors().hasAny)
    }

    @Test
    fun `address validation failure is carried as a server field error`() {
        val s = LoginUiState(
            fieldErrors = LoginUiState.FieldErrors(server = "端口超出范围: 70000"),
        )
        assertEquals("端口超出范围: 70000", s.fieldErrors.server)
        assertTrue(s.fieldErrors.hasAny)
        // A field error is independent of the form-level banner.
        assertNull(s.formError)
    }

    @Test
    fun `form-level error is independent of field errors`() {
        val s = LoginUiState(formError = "无法连接服务器")
        assertEquals("无法连接服务器", s.formError)
        assertFalse(s.fieldErrors.hasAny)
    }
}
