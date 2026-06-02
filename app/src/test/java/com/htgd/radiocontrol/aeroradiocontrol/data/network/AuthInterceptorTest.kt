package com.htgd.radiocontrol.aeroradiocontrol.data.network

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStore
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [AuthInterceptor]: Bearer injection, login exemption, and the
 * 401 → refresh → retry-once chain (success and failure). ★ NEXT-2: the
 * refresh-failure path additionally clears `ServerConfig.baseUrl` so the static
 * `Constant.serveraddress` cache goes null in lockstep with the dead session.
 */
class AuthInterceptorTest {

    private fun authStore(jwt: String?): AuthStore = mockk(relaxed = true) {
        every { this@mockk.jwt } returns MutableStateFlow(jwt)
    }

    /**
     * ★ NEXT-2: relaxed ServerConfig fake with a valid baseUrl so the NEXT-3
     * fail-fast guard (jwt blank OR baseUrl blank → NO_SESSION) doesn't fire on
     * tests that have a valid jwt. Tests asserting on setBaseUrl("") still work
     * because mockk(relaxed = true) records all calls.
     */
    private fun serverConfig(): ServerConfig = mockk(relaxed = true) {
        every { baseUrl() } returns "http://10.0.0.1:99/api"
    }

    private fun request(path: String) =
        Request.Builder().url("http://server/api$path").build()

    @Test
    fun attachesBearerForNonLoginRequest() {
        val store = authStore("jwt-1")
        val interceptor = AuthInterceptor(store, serverConfig())
        val chain = SequencedChain(request("/terminal/terminalinfo"), listOf(200))

        interceptor.intercept(chain)

        assertEquals("Bearer jwt-1", chain.proceeded[0].header("Authorization"))
    }

    @Test
    fun skipsAuthHeaderForLoginRequest() {
        val store = authStore("jwt-1")
        val interceptor = AuthInterceptor(store, serverConfig())
        val chain = SequencedChain(request("/authorizations"), listOf(200))

        interceptor.intercept(chain)

        assertNull(chain.proceeded[0].header("Authorization"))
    }

    @Test
    fun on401_refreshesWithStaleJwtAndRetriesWithNewToken() {
        val store = authStore("jwt-stale")
        coEvery { store.refresh(knownStaleJwt = "jwt-stale") } returns Result.success("jwt-fresh")
        val config = serverConfig()
        val interceptor = AuthInterceptor(store, config)
        // First call 401, retry 200.
        val chain = SequencedChain(request("/terminal/terminalinfo"), listOf(401, 200))

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.proceeded.size)
        assertEquals("Bearer jwt-stale", chain.proceeded[0].header("Authorization"))
        assertEquals("Bearer jwt-fresh", chain.proceeded[1].header("Authorization"))
        coVerify(exactly = 1) { store.refresh(knownStaleJwt = "jwt-stale") }
        // ★ NEXT-2: refresh SUCCESS → serverConfig is NOT cleared (session is alive).
        verify(exactly = 0) { config.setBaseUrl("") }
    }

    @Test
    fun on401_refreshFailure_retriesWithoutToken_andClearsServerConfig() {
        val store = authStore("jwt-stale")
        coEvery { store.refresh(knownStaleJwt = "jwt-stale") } returns
            Result.failure(IllegalStateException("refresh failed"))
        val config = serverConfig()
        val interceptor = AuthInterceptor(store, config)
        val chain = SequencedChain(request("/terminal/terminalinfo"), listOf(401, 401))

        val response = interceptor.intercept(chain)

        // Retried once; second attempt carries no token (session was cleared).
        assertEquals(401, response.code)
        assertEquals(2, chain.proceeded.size)
        assertNull(chain.proceeded[1].header("Authorization"))
        // ★ NEXT-2: refresh failure → static Constant.serveraddress cache cleared
        // in lockstep with the cleared session so no later repo races on stale URL.
        verify(exactly = 1) { config.setBaseUrl("") }
    }

    @Test
    fun non401_passesThroughWithoutRefresh() {
        val store = authStore("jwt-1")
        val interceptor = AuthInterceptor(store, serverConfig())
        val chain = SequencedChain(request("/terminal/terminalinfo"), listOf(500))

        val response = interceptor.intercept(chain)

        assertEquals(500, response.code)
        assertEquals(1, chain.proceeded.size)
        coVerify(exactly = 0) { store.refresh(any()) }
    }
}

/**
 * [Interceptor.Chain] that returns a predefined sequence of response codes
 * (one per proceed call) and records every request it proceeded.
 */
class SequencedChain(
    private val request: Request,
    private val codes: List<Int>,
) : Interceptor.Chain {
    val proceeded = mutableListOf<Request>()
    private var index = 0

    override fun request(): Request = request

    override fun proceed(request: Request): Response {
        proceeded += request
        val code = codes[index.coerceAtMost(codes.lastIndex)]
        index++
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("")
            .body("".toResponseBody(null))
            .build()
    }

    override fun connection() = null
    override fun call(): okhttp3.Call = throw UnsupportedOperationException()
    override fun connectTimeoutMillis() = 0
    override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
    override fun readTimeoutMillis() = 0
    override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
    override fun writeTimeoutMillis() = 0
    override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
}
