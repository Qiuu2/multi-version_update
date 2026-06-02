package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import com.htgd.radiocontrol.aeroradiocontrol.data.api.AuthApi
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TokenDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TokenEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.network.HttpStatusSuccessPolicy
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for [RetrofitLoginAuthenticator]: URL construction, token mapping,
 * and the documented-assumption success/failure decisions.
 */
class RetrofitLoginAuthenticatorTest {

    private val api: AuthApi = mockk()
    private val authenticator = RetrofitLoginAuthenticator(
        authApi = api,
        successPolicy = HttpStatusSuccessPolicy(),
        ioDispatcher = UnconfinedTestDispatcher(),
    )
    private val address = ServerAddress("192.168.1.10", 8080)

    private fun ok(vararg tokens: TokenDto) =
        Response.success(TokenEnvelopeDto(tokens.toList()))

    private fun httpError(code: Int) = Response.error<TokenEnvelopeDto>(
        code, "".toResponseBody("application/json".toMediaType()),
    )

    @Test
    fun success_mapsTokenToAuthResult_andBuildsApiUrl() = runTest {
        val urlSlot = slot<String>()
        coEvery {
            api.login(url = capture(urlSlot), username = "admin", password = "pw")
        } returns ok(TokenDto(token = "jwt-xyz", priority = "1"))

        val result = authenticator.authenticate(address, "admin", "pw")

        assertTrue(result.isSuccess)
        assertEquals("jwt-xyz", result.getOrNull()!!.jwt)
        // refreshToken/account null until backend contract confirms (OPEN D-1).
        assertNull(result.getOrNull()!!.refreshToken)
        assertNull(result.getOrNull()!!.account)
        // Absolute URL includes host:port and the /api base segment.
        assertEquals("http://192.168.1.10:8080/api/authorizations", urlSlot.captured)
    }

    @Test
    fun non2xx_isFailure() = runTest {
        coEvery { api.login(any(), any(), any()) } returns httpError(401)

        val result = authenticator.authenticate(address, "admin", "wrong")

        assertTrue(result.isFailure)
    }

    @Test
    fun emptyData_isFailure() = runTest {
        coEvery { api.login(any(), any(), any()) } returns ok() // data = []

        val result = authenticator.authenticate(address, "admin", "pw")

        assertTrue(result.isFailure)
    }

    @Test
    fun blankToken_isFailure() = runTest {
        coEvery { api.login(any(), any(), any()) } returns ok(TokenDto(token = "  "))

        val result = authenticator.authenticate(address, "admin", "pw")

        assertTrue(result.isFailure)
    }
}
