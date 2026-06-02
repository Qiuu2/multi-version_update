package com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge

import com.htgd.radiocontrol.aeroradiocontrol.data.network.ApiException
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [V3CallbackAdapter]: the v3 one-shot callback → suspend Result
 * bridge. Uses mockkStatic on the static RequestManger.get to drive onSucess /
 * onFailed and assert the continuation resumes correctly.
 *
 * ★ NEXT-3 (2026-06-01) — [ServerConfig] is now injected for the NO_SESSION
 * fail-fast guard. Tests that exercise the happy path inject a serverConfig with
 * a valid token + baseUrl; tests that verify NO_SESSION inject one with blank
 * values and assert [RequestManger] is never called.
 */
class V3CallbackAdapterTest {

    // ── Fake ServerConfig ──────────────────────────────────────────────────

    private fun serverConfigWith(token: String, url: String): ServerConfig =
        object : ServerConfig {
            private var _url = url
            private var _token = token
            override fun baseUrl() = _url
            override fun setBaseUrl(value: String) { _url = value }
            override fun authToken() = _token
            override fun setAuthToken(bearerToken: String) { _token = bearerToken }
        }

    /** ServerConfig with a valid token + baseUrl — happy-path tests use this. */
    private val validConfig = serverConfigWith("Bearer test-token", "http://h/api")

    /** ServerConfig with blank token — NO_SESSION guard tests use this. */
    private val blankTokenConfig = serverConfigWith("", "http://h/api")

    /** ServerConfig with blank baseUrl — NO_SESSION guard tests use this. */
    private val blankUrlConfig = serverConfigWith("Bearer test-token", "")

    private val adapter = V3CallbackAdapter(validConfig)

    @Before
    fun setup() {
        mockkStatic(RequestManger::class)
        every { RequestManger.getInstance() } returns mockk<RequestManger>(relaxed = true)
    }

    @After
    fun teardown() = unmockkStatic(RequestManger::class)

    // ── Happy path ─────────────────────────────────────────────────────────

    @Test
    fun get_onSucess_resumesWithSuccess() = runTest {
        val listenerSlot = slot<onRequestLister>()
        every { RequestManger.get("http://h/api/x", capture(listenerSlot)) } answers {
            listenerSlot.captured.onSucess(200, """{"ok":true}""")
        }

        val result = adapter.get("http://h/api/x")

        assertTrue(result.isSuccess)
        assertEquals("""{"ok":true}""", result.getOrNull())
    }

    @Test
    fun get_onFailed_nonHtmlBody_resumesWithApiException_HttpError() = runTest {
        // ★ NEXT-3: non-200 responses are wrapped as ApiException (not V3HttpException)
        // so the ViewModel can map error types to safe user-facing messages.
        val listenerSlot = slot<onRequestLister>()
        every { RequestManger.get(any(), capture(listenerSlot)) } answers {
            listenerSlot.captured.onFailed(500, "server error")
        }

        val result = adapter.get("http://h/api/x")

        assertTrue(result.isFailure)
        val e = result.exceptionOrNull()
        assertTrue(e is ApiException)
        assertEquals(ApiException.Kind.HTTP_ERROR, (e as ApiException).kind)
        assertEquals(500, e.httpCode)
    }

    @Test
    fun get_onFailed_htmlBody_resumesWithApiException_NonJson() = runTest {
        // HTML error pages (e.g. 500 gateway errors) must surface as NON_JSON,
        // not HTTP_ERROR, so the ViewModel shows "服务器返回了非预期格式" not raw HTML.
        val listenerSlot = slot<onRequestLister>()
        every { RequestManger.get(any(), capture(listenerSlot)) } answers {
            listenerSlot.captured.onFailed(500, "<html><body>Internal Server Error</body></html>")
        }

        val result = adapter.get("http://h/api/x")

        assertTrue(result.isFailure)
        val e = result.exceptionOrNull()
        assertTrue(e is ApiException)
        assertEquals(ApiException.Kind.NON_JSON, (e as ApiException).kind)
        assertEquals(500, e.httpCode)
    }

    @Test
    fun get_synchronousThrow_resumesWithFailure() = runTest {
        // RequestManger.get throws synchronously (e.g. bad URL) before any
        // callback → adapter must resume with failure, not hang.
        every { RequestManger.get(any(), any<onRequestLister>()) } throws RuntimeException("boom")

        val result = adapter.get("bad")

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    // ── ★ NEXT-3: NO_SESSION fail-fast on the live v3 path ─────────────────

    @Test
    fun get_blankToken_returnsNoSession_withoutCallingRequestManger() = runTest {
        // CTO hard requirement: "缺 token 绝不发任何业务请求". This test proves the
        // socket is never touched — RequestManger.get is never called.
        val adapterNoToken = V3CallbackAdapter(blankTokenConfig)

        val result = adapterNoToken.get("http://h/api/terminal/terzone")

        assertTrue(result.isFailure)
        val e = result.exceptionOrNull()
        assertTrue(e is ApiException)
        assertEquals(ApiException.Kind.NO_SESSION, (e as ApiException).kind)
        // ★ Prove socket not touched: RequestManger.get must NOT have been called.
        verify(exactly = 0) { RequestManger.get(any(), any()) }
    }

    @Test
    fun get_blankBaseUrl_returnsNoSession_withoutCallingRequestManger() = runTest {
        val adapterNoUrl = V3CallbackAdapter(blankUrlConfig)

        val result = adapterNoUrl.get("http://h/api/terminal/terzone")

        assertTrue(result.isFailure)
        assertEquals(ApiException.Kind.NO_SESSION, (result.exceptionOrNull() as? ApiException)?.kind)
        verify(exactly = 0) { RequestManger.get(any(), any()) }
    }

    @Test
    fun post_needTokenTrue_blankToken_returnsNoSession_withoutCallingRequestManger() = runTest {
        // Token-bearing POSTs (repo refresh calls) are guarded — blank token → NO_SESSION.
        val adapterNoToken = V3CallbackAdapter(blankTokenConfig)
        val builder = mockk<MyRequestBuilder>(relaxed = true) {
            every { isNeedToken } returns true
        }

        val result = adapterNoToken.post(builder)

        assertTrue(result.isFailure)
        assertEquals(ApiException.Kind.NO_SESSION, (result.exceptionOrNull() as? ApiException)?.kind)
        // Prove socket not touched.
        verify(exactly = 0) { RequestManger.postHashMap(any(), any()) }
    }

    @Test
    fun post_needTokenFalse_blankToken_proceeds_loginExempt() = runTest {
        // Login POST has needToken=false — must NOT be blocked by absent token,
        // because it's how you GET a token in the first place.
        val adapterNoToken = V3CallbackAdapter(blankTokenConfig)
        val listenerSlot = slot<onRequestLister>()
        val builder = mockk<MyRequestBuilder>(relaxed = true) {
            every { isNeedToken } returns false
        }
        every { RequestManger.postHashMap(any(), capture(listenerSlot)) } answers {
            listenerSlot.captured.onSucess(200, """{"data":[{"token":"tok"}]}""")
        }

        val result = adapterNoToken.post(builder)

        // Login call goes through despite blank token.
        assertFalse(result.isFailure)
        verify(exactly = 1) { RequestManger.postHashMap(any(), any()) }
    }
}
