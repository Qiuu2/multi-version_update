package com.htgd.radiocontrol.aeroradiocontrol.data.network

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStore
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.ServerAddress
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit tests for [DynamicBaseUrlInterceptor]: placeholder → real host + /api,
 * passthrough for already-absolute URLs, and the not-configured failure.
 */
class DynamicBaseUrlInterceptorTest {

    private fun authStoreWith(address: ServerAddress?): AuthStore = mockk {
        every { serverAddress } returns MutableStateFlow(address)
    }

    @Test
    fun rewritesPlaceholderToRealHostWithApiPrefix() {
        val store = authStoreWith(ServerAddress("192.168.1.10", 8080))
        val interceptor = DynamicBaseUrlInterceptor(store)
        val chain = RecordingChain(
            Request.Builder().url("http://placeholder.invalid/authorizations").build(),
        )

        interceptor.intercept(chain)

        // placeholder host → real host:port, and /api prepended to the path.
        assertEquals("http://192.168.1.10:8080/api/authorizations", chain.proceededUrl())
    }

    @Test
    fun rewritePreservesNestedPathAndQuery() {
        val store = authStoreWith(ServerAddress("10.0.0.5", 80))
        val interceptor = DynamicBaseUrlInterceptor(store)
        val chain = RecordingChain(
            Request.Builder()
                .url("http://placeholder.invalid/terminal/mediainfo/2?folder=root")
                .build(),
        )

        interceptor.intercept(chain)

        assertEquals(
            "http://10.0.0.5/api/terminal/mediainfo/2?folder=root",
            chain.proceededUrl(),
        )
    }

    @Test
    fun leavesNonPlaceholderUrlUntouched() {
        val store = authStoreWith(ServerAddress("192.168.1.10", 8080))
        val interceptor = DynamicBaseUrlInterceptor(store)
        val absolute = "http://other.host:9000/health"
        val chain = RecordingChain(Request.Builder().url(absolute).build())

        interceptor.intercept(chain)

        assertEquals(absolute, chain.proceededUrl())
    }

    @Test
    fun throwsWhenServerAddressNotConfigured() {
        val store = authStoreWith(null)
        val interceptor = DynamicBaseUrlInterceptor(store)
        val chain = RecordingChain(
            Request.Builder().url("http://placeholder.invalid/authorizations").build(),
        )

        assertThrows(ServerAddressNotConfiguredException::class.java) {
            interceptor.intercept(chain)
        }
    }
}

/**
 * Minimal [Interceptor.Chain] that records the request passed to [proceed] and
 * returns a canned response. Avoids a MockWebServer dependency.
 */
class RecordingChain(
    private val request: Request,
    private val responseCode: Int = 200,
) : Interceptor.Chain {
    private var proceeded: Request? = null

    fun proceededUrl(): String = proceeded!!.url.toString()
    fun proceededRequest(): Request = proceeded!!

    override fun request(): Request = request

    override fun proceed(request: Request): Response {
        proceeded = request
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(responseCode)
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
