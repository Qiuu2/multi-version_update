package com.htgd.radiocontrol.aeroradiocontrol.di

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [LegacyNetworkModule] — the AR-004 / RISK-AUDIT-05 fix.
 *
 * The contract that matters: the legacy client shares the new-stack client's
 * connection pool + dispatcher (so there's one pool, not two), but carries NONE
 * of the new-stack interceptors (so legacy requests aren't rewritten/re-authed)
 * and keeps the legacy timeouts.
 */
class LegacyNetworkModuleTest {

    private val module = LegacyNetworkModule

    /** A new-stack-like client with two interceptors and 15s connect timeout. */
    private fun newStackClient(): OkHttpClient {
        val noop = Interceptor { chain -> chain.proceed(chain.request()) }
        return OkHttpClient.Builder()
            .addInterceptor(noop)
            .addInterceptor(noop)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Test
    fun legacyClient_sharesConnectionPoolAndDispatcher() {
        val newStack = newStackClient()
        val legacy = module.provideLegacyOkHttpClient(newStack)

        // Same pooled resources = the RISK-AUDIT-05 fix (one pool, not two).
        assertSame(newStack.connectionPool, legacy.connectionPool)
        assertSame(newStack.dispatcher, legacy.dispatcher)
    }

    @Test
    fun legacyClient_carriesNoNewStackInterceptors() {
        val newStack = newStackClient()
        val legacy = module.provideLegacyOkHttpClient(newStack)

        assertTrue(
            "legacy client must not inherit new-stack interceptors",
            legacy.interceptors.isEmpty(),
        )
        assertTrue(legacy.networkInterceptors.isEmpty())
    }

    @Test
    fun legacyClient_keepsLegacyTimeouts() {
        val newStack = newStackClient()
        val legacy = module.provideLegacyOkHttpClient(newStack)

        // 20s, the original RequestManger value — not the new stack's 15s.
        assertEquals(20_000, legacy.connectTimeoutMillis)
        assertEquals(20_000, legacy.readTimeoutMillis)
    }
}
