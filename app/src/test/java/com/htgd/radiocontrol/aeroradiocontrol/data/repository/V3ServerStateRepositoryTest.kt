package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.google.gson.GsonBuilder
import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerHealth
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [V3ServerStateRepository] (Plan A / TASK-PA-05).
 *
 * Verifies the SSOT contract mirrored from V3TerminalRepository (Critic AC): null
 * start, success publishes + re-emits, failure (network / parse / empty-data)
 * keeps the prior snapshot, concurrent refresh shares one fetch, absolute v3 URL,
 * and ServerStateDto → ServerState mapper field fidelity.
 */
class V3ServerStateRepositoryTest {

    private val adapter: V3CallbackAdapter = mockk()
    private val gson = GsonBuilder().setLenient().create() // mirrors NetworkModule.provideGson

    // Fake ServerConfig: in-memory base URL so the test never loads v3 `Constant`
    // (static init touches Android → ExceptionInInitializerError on plain JVM).
    private val serverConfig = object : ServerConfig {
        private var url = "http://10.0.0.1:8080/api"
        override fun baseUrl() = url
        override fun setBaseUrl(value: String) { url = value }
    }
    private lateinit var repo: V3ServerStateRepository

    @Before
    fun setup() {
        val dispatcher = UnconfinedTestDispatcher()
        repo = V3ServerStateRepository(adapter, gson, serverConfig, CoroutineScope(dispatcher), dispatcher)
    }

    private fun stubState(json: String) =
        coEvery { adapter.get(match { it.endsWith("/server/serverstate") }) } returns Result.success(json)

    @Test
    fun observeServerState_startsNull() = runTest {
        assertNull(repo.observeServerState().first())
    }

    @Test
    fun refresh_success_parsesV3Json_mapsAllFields_andReEmits() = runTest {
        stubState(
            """{"data":[{
                "state":1,"connection":7,"taskcount":3,"bandwidth":120,
                "maxconnection":256,"ctrlport":4520,"dataport":4521,
                "name":"campus-A","ip":"10.0.0.9","gate":"10.0.0.1"
            }]}""",
        )

        val result = repo.refresh()

        assertTrue(result.isSuccess)
        val state = repo.observeServerState().first()!!
        // Mapper field fidelity: every SeverStateModel field carried 1:1.
        assertEquals(ServerHealth.Online, state.health)
        assertEquals(7, state.connection)
        assertEquals(3, state.taskCount)
        assertEquals(120, state.bandwidth)
        assertEquals(256L, state.maxConnection)
        assertEquals(4520, state.ctrlPort)
        assertEquals(4521, state.dataPort)
        assertEquals("campus-A", state.name)
        assertEquals("10.0.0.9", state.ip)
        assertEquals("10.0.0.1", state.gate)
    }

    @Test
    fun refresh_state0_mapsToOffline() = runTest {
        stubState("""{"data":[{"state":0,"name":"down"}]}""")
        repo.refresh()
        assertEquals(ServerHealth.Offline, repo.observeServerState().first()!!.health)
    }

    @Test
    fun refresh_networkFail_isFailure_keepsPriorSnapshot() = runTest {
        // Seed a good snapshot.
        stubState("""{"data":[{"state":1,"name":"up"}]}""")
        repo.refresh()
        assertEquals("up", repo.observeServerState().first()!!.name)

        // Now the fetch fails → failure, prior snapshot retained.
        coEvery { adapter.get(match { it.endsWith("/server/serverstate") }) } returns
            Result.failure(V3HttpException(500, "boom"))
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals("up", repo.observeServerState().first()!!.name) // unchanged
    }

    @Test
    fun refresh_malformedJson_isFailure_keepsPriorSnapshot() = runTest {
        stubState("""{"data":[{"state":1,"name":"up"}]}""")
        repo.refresh()

        // Garbage JSON → parse throws → failure, snapshot kept.
        coEvery { adapter.get(match { it.endsWith("/server/serverstate") }) } returns
            Result.success("not json at all <<<")
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals("up", repo.observeServerState().first()!!.name) // unchanged
    }

    @Test
    fun refresh_emptyData_isFailure_keepsPriorSnapshot() = runTest {
        stubState("""{"data":[{"state":1,"name":"up"}]}""")
        repo.refresh()

        // Empty data array → no first element → failure, snapshot kept.
        coEvery { adapter.get(match { it.endsWith("/server/serverstate") }) } returns
            Result.success("""{"data":[]}""")
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals("up", repo.observeServerState().first()!!.name) // unchanged
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    @Test
    fun concurrentRefresh_sharesOneFetch() = runBlocking {
        // REAL multi-threaded dispatcher (not runTest's eager virtual scheduler,
        // which collapses the in-flight window) so the Mutex + shared Deferred
        // behave as in production — mirrors V3TerminalRepositoryTest's idiom.
        val dispatcher = newFixedThreadPoolContext(4, "v3-state-race")
        val calls = AtomicInteger(0)
        val allInFlight = java.util.concurrent.CountDownLatch(3)
        val barrier = CompletableDeferred<Unit>()
        coEvery { adapter.get(match { it.endsWith("/server/serverstate") }) } coAnswers {
            calls.incrementAndGet()
            barrier.await()
            Result.success("""{"data":[{"state":1,"name":"up"}]}""")
        }

        val sharedRepo = V3ServerStateRepository(
            adapter, gson, serverConfig, CoroutineScope(dispatcher), dispatcher,
        )

        val deferreds = (1..3).map {
            async(dispatcher) { allInFlight.countDown(); sharedRepo.refresh() }
        }
        allInFlight.await(2, java.util.concurrent.TimeUnit.SECONDS)
        barrier.complete(Unit)
        val results = deferreds.awaitAll()
        dispatcher.close()

        assertEquals(1, calls.get()) // one shared fetch, not three
        assertTrue(results.all { it.isSuccess })
        assertEquals("up", sharedRepo.observeServerState().first()!!.name)
    }

    @Test
    fun refresh_buildsAbsoluteV3Url() = runTest {
        // Asserts the repo targets ServerConfig.baseUrl() + path (v3 URL shape).
        coEvery { adapter.get("http://10.0.0.1:8080/api/server/serverstate") } returns
            Result.success("""{"data":[{"state":1}]}""")

        val result = repo.refresh()
        assertTrue(result.isSuccess) // exact-URL stub matched
    }
}
