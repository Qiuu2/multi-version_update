package com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge

import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [V3CallbackAdapter]: the v3 one-shot callback → suspend Result
 * bridge. Uses mockkStatic on the static RequestManger.get to drive onSucess /
 * onFailed and assert the continuation resumes correctly.
 */
class V3CallbackAdapterTest {

    private val adapter = V3CallbackAdapter()

    @Before
    fun setup() {
        mockkStatic(RequestManger::class)
        every { RequestManger.getInstance() } returns mockkInstance()
    }

    @After
    fun teardown() = unmockkStatic(RequestManger::class)

    private fun mockkInstance() = io.mockk.mockk<RequestManger>(relaxed = true)

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
    fun get_onFailed_resumesWithV3HttpException() = runTest {
        val listenerSlot = slot<onRequestLister>()
        every { RequestManger.get(any(), capture(listenerSlot)) } answers {
            listenerSlot.captured.onFailed(500, "server error")
        }

        val result = adapter.get("http://h/api/x")

        assertTrue(result.isFailure)
        val e = result.exceptionOrNull()
        assertTrue(e is V3HttpException)
        assertEquals(500, (e as V3HttpException).code)
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
}
