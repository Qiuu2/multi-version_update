package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.google.gson.GsonBuilder
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.V3CallbackAdapter
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.V3HttpException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [V3TerminalRepository] (Plan A / TASK-PA-01).
 *
 * Verifies the SSOT contract carried over verbatim from AR-101 (Critic AC):
 * empty start, success fills + re-emits, failure keeps prior snapshot, and the
 * v3 raw-JSON → reverse-engineered DTO → existing Mapper → domain pipeline.
 */
class V3TerminalRepositoryTest {

    private val adapter: V3CallbackAdapter = mockk()
    private val gson = GsonBuilder().setLenient().create() // mirrors NetworkModule.provideGson

    // Fake ServerConfig: an in-memory base URL so the test never loads v3
    // `Constant` (whose static init touches Android → ExceptionInInitializerError
    // on plain JVM). Same injected-fake idiom as AuthStoreImplTest's KeyValueStore.
    private val serverConfig = object : ServerConfig {
        private var url = "http://10.0.0.1:8080/api"
        override fun baseUrl() = url
        override fun setBaseUrl(value: String) { url = value }
    }
    private lateinit var repo: V3TerminalRepository

    @Before
    fun setup() {
        val dispatcher = UnconfinedTestDispatcher()
        repo = V3TerminalRepository(adapter, gson, serverConfig, CoroutineScope(dispatcher), dispatcher)
    }

    private fun stubZones(json: String) =
        coEvery { adapter.get(match { it.endsWith("/terminal/terzone") }) } returns Result.success(json)

    private fun stubTerminals(json: String) =
        coEvery { adapter.get(match { it.endsWith("/terminal/terminalinfo") }) } returns Result.success(json)

    @Test
    fun observeZones_startsEmpty() = runTest {
        assertEquals(emptyList<Any>(), repo.observeZones().first())
    }

    @Test
    fun refresh_success_parsesV3Json_joinsTerminals_andReEmits() = runTest {
        stubZones("""{"data":[{"id":1,"name":"Z1"},{"id":2,"name":"Z2"}]}""")
        stubTerminals(
            """{"data":[
                {"id":10,"name":"T10","zone":1,"netstate":1},
                {"id":11,"name":"T11","zone":1,"netstate":0},
                {"id":12,"name":"T12","zone":2,"netstate":1}
            ]}""",
        )

        val result = repo.refresh()

        assertTrue(result.isSuccess)
        val zones = repo.observeZones().first()
        assertEquals(2, zones.size)
        assertEquals(listOf("T10", "T11"), zones.first { it.id == "1" }.terminals.map { it.name })
        assertEquals(listOf("T12"), zones.first { it.id == "2" }.terminals.map { it.name })
    }

    @Test
    fun observeTerminals_flattens() = runTest {
        stubZones("""{"data":[{"id":1,"name":"Z1"}]}""")
        stubTerminals("""{"data":[{"id":10,"name":"T10","zone":1,"netstate":1}]}""")
        repo.refresh()
        assertEquals(1, repo.observeTerminals().first().size)
    }

    @Test
    fun refresh_zonesFail_isFailure_keepsPriorSnapshot() = runTest {
        // Seed a good snapshot.
        stubZones("""{"data":[{"id":1,"name":"Z1"}]}""")
        stubTerminals("""{"data":[{"id":10,"name":"T10","zone":1,"netstate":1}]}""")
        repo.refresh()
        assertEquals(1, repo.observeZones().first().size)

        // Now zones fail → failure, prior snapshot retained.
        coEvery { adapter.get(match { it.endsWith("/terminal/terzone") }) } returns
            Result.failure(V3HttpException(500, "boom"))
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(1, repo.observeZones().first().size)
        assertEquals("Z1", repo.observeZones().first()[0].name)
    }

    @Test
    fun refresh_malformedJson_isFailure_keepsPriorSnapshot() = runTest {
        stubZones("""{"data":[{"id":1,"name":"Z1"}]}""")
        stubTerminals("""{"data":[{"id":10,"name":"T10","zone":1,"netstate":1}]}""")
        repo.refresh()

        // Garbage JSON from the v3 stack → parse throws → failure, snapshot kept.
        coEvery { adapter.get(match { it.endsWith("/terminal/terminalinfo") }) } returns
            Result.success("not json at all <<<")
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(1, repo.observeZones().first().size) // unchanged
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    @Test
    fun concurrentRefresh_sharesOneFetch() = runBlocking {
        // Use a REAL multi-threaded dispatcher (not runTest's eager virtual
        // scheduler, which collapses the in-flight window) so the Mutex + shared
        // Deferred behave as in production — mirrors AuthStoreImplTest's concurrency
        // idiom. The latch ensures all 3 callers are inside refresh() before the
        // barrier releases the one shared fetch.
        val dispatcher = newFixedThreadPoolContext(4, "v3-refresh-race")
        val zonesCalls = AtomicInteger(0)
        val allInFlight = java.util.concurrent.CountDownLatch(3)
        val barrier = CompletableDeferred<Unit>()
        coEvery { adapter.get(match { it.endsWith("/terminal/terzone") }) } coAnswers {
            zonesCalls.incrementAndGet()
            barrier.await()
            Result.success("""{"data":[{"id":1,"name":"Z1"}]}""")
        }
        stubTerminals("""{"data":[{"id":10,"name":"T10","zone":1,"netstate":1}]}""")

        val sharedRepo = V3TerminalRepository(
            adapter, gson, serverConfig, CoroutineScope(dispatcher), dispatcher,
        )

        val deferreds = (1..3).map {
            async(dispatcher) { allInFlight.countDown(); sharedRepo.refresh() }
        }
        allInFlight.await(2, java.util.concurrent.TimeUnit.SECONDS)
        barrier.complete(Unit)
        val results = deferreds.awaitAll()
        dispatcher.close()

        assertEquals(1, zonesCalls.get()) // one shared fetch, not three
        assertTrue(results.all { it.isSuccess })
        assertEquals(1, sharedRepo.observeZones().first().size)
    }

    @Test
    fun refresh_buildsAbsoluteV3Urls() = runTest {
        // Asserts the repo targets Constant.serveraddress + path (v3 URL shape).
        coEvery { adapter.get("http://10.0.0.1:8080/api/terminal/terzone") } returns
            Result.success("""{"data":[]}""")
        coEvery { adapter.get("http://10.0.0.1:8080/api/terminal/terminalinfo") } returns
            Result.success("""{"data":[]}""")

        val result = repo.refresh()
        assertTrue(result.isSuccess) // exact-URL stubs matched
    }
}
