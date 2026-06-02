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
 * Unit tests for [V3TerminalRepository] (Plan A / TASK-PA-01 / PA-14 fix).
 *
 * PA-14 Leg 4 (data) — uses the CTO Bearer-token ground-truth response as the
 * success fixture (subset, copied from
 * .state/api-snapshots/terzone-cto-capture-2026-05-30.json so the test stays
 * self-contained and we don't depend on a runtime file in CI). The load-bearing
 * assertion is `refresh_success_groundTruth_caochangHasExactly4NamedTerminals`:
 * if 操场 ever stops showing exactly 14/15/16/18 (right one/right two/营销大厅-2/
 * 网络功放2), the fix has regressed — same shape as the v4 BLOCKER.
 *
 * Also verifies the SSOT contract: empty start, success fills + re-emits,
 * failure keeps prior snapshot, and the single-endpoint nested-mapping pipeline
 * (no client-side groupBy on `terminal.zone` — that was the bug).
 */
class V3TerminalRepositoryTest {

    private val adapter: V3CallbackAdapter = mockk()
    private val gson = GsonBuilder().setLenient().create() // mirrors NetworkModule.provideGson

    // Fake ServerConfig: an in-memory base URL so the test never loads v3
    // `Constant` (whose static init touches Android → ExceptionInInitializerError
    // on plain JVM).
    private val serverConfig = object : ServerConfig {
        private var url = "http://10.0.0.1:8080/api"
        override fun baseUrl() = url
        override fun setBaseUrl(value: String) { url = value }
        override fun authToken() = "Bearer test-token"
        override fun setAuthToken(bearerToken: String) { /* no-op in tests */ }
    }
    private lateinit var repo: V3TerminalRepository

    @Before
    fun setup() {
        val dispatcher = UnconfinedTestDispatcher()
        repo = V3TerminalRepository(adapter, gson, serverConfig, CoroutineScope(dispatcher), dispatcher)
    }

    private fun stubTerzone(json: String) =
        coEvery { adapter.get(match { it.endsWith("/terminal/terzone") }) } returns Result.success(json)

    /** Verbatim subset of the CTO Bearer-token capture (operator gave us this) —
     *  6 zones, with 操场 carrying the 4 named terminals + the misleading
     *  terminal.zone values (0,0,0,8) that broke the old groupBy. */
    private val CTO_TERZONE_FIXTURE = """
        {"data":[
          {"all":"6","count":1,"start":1,"state":0,"id":1,"datetime":"2026-4-22 21:36:31","name":"操场","description":"","terminal":[
            {"id":14,"type":11,"taskstate":0,"devicestate":1,"netstate":1,"speechstate":0,"volume":50,"isinstancy":0,"zone":0,"name":"右一终端","ip":"192.168.3.26"},
            {"id":15,"type":11,"taskstate":0,"devicestate":1,"netstate":1,"speechstate":0,"volume":50,"isinstancy":0,"zone":0,"name":"右二终端","ip":"192.168.3.27"},
            {"id":16,"type":11,"taskstate":0,"devicestate":1,"netstate":1,"speechstate":0,"volume":50,"isinstancy":0,"zone":0,"name":"营销大厅-2","ip":"192.168.3.12"},
            {"id":18,"type":5,"taskstate":0,"devicestate":1,"netstate":1,"speechstate":0,"volume":70,"isinstancy":0,"zone":8,"name":"网络功放2","ip":"192.168.3.38"}
          ]},
          {"id":2,"name":"英语角分区","terminal":[{"id":12,"name":"营销大厅-1"},{"id":14,"name":"右一终端"}]},
          {"id":3,"name":"航天","terminal":[{"id":14,"name":"右一终端"}]},
          {"id":5,"name":"会议室","terminal":[{"id":14,"name":"右一终端"},{"id":15,"name":"右二终端"}]},
          {"id":8,"name":"海口","terminal":[{"id":25,"name":"海口运营中心","netstate":0}]},
          {"id":9,"name":"赣州角","terminal":[]}
        ]}
    """.trimIndent()

    @Test
    fun observeZones_startsEmpty() = runTest {
        assertEquals(emptyList<Any>(), repo.observeZones().first())
    }

    /** ★ PA-14 LOAD-BEARING: this is the regression guard for the 操场 BLOCKER. */
    @Test
    fun refresh_success_groundTruth_caochangHasExactly4NamedTerminals() = runTest {
        stubTerzone(CTO_TERZONE_FIXTURE)

        val result = repo.refresh()

        assertTrue(result.isSuccess)
        val zones = repo.observeZones().first()
        assertEquals(6, zones.size)

        val caochang = zones.first { it.id == "1" }
        assertEquals("操场", caochang.name)
        // EXACTLY the 4 names CTO observed — order preserved from the wire array.
        assertEquals(
            listOf("右一终端", "右二终端", "营销大厅-2", "网络功放2"),
            caochang.terminals.map { it.name },
        )
        // Membership comes from NESTING, not `terminal.zone`:
        // every terminal under 操场 has Terminal.zoneId == "1" regardless of its
        // wire `zone` value (0 or 8 — proving we did NOT groupBy that field).
        assertTrue(caochang.terminals.all { it.zoneId == "1" })
    }

    @Test
    fun refresh_success_manyToMany_terminal14AppearsInFourZones() = runTest {
        // 右一终端 (id=14) is nested under 操场, 英语角分区, 航天, 会议室 — keep all 4.
        stubTerzone(CTO_TERZONE_FIXTURE)
        repo.refresh()

        val zones = repo.observeZones().first()
        val zonesContaining14 = zones.filter { z -> z.terminals.any { it.id == "14" } }
        assertEquals(
            listOf("操场", "英语角分区", "航天", "会议室"),
            zonesContaining14.map { it.name },
        )
        // observeTerminals flattens with duplicates (one entry per nesting):
        val flat = repo.observeTerminals().first()
        assertEquals(4, flat.count { it.id == "14" })
    }

    @Test
    fun refresh_success_emptyZone_isPreserved() = runTest {
        // 赣州角 (id=9) has terminal:[] — must surface as an empty zone, not dropped.
        stubTerzone(CTO_TERZONE_FIXTURE)
        repo.refresh()

        val ganzhou = repo.observeZones().first().first { it.id == "9" }
        assertEquals(0, ganzhou.terminals.size)
    }

    @Test
    fun refresh_networkFail_isFailure_keepsPriorSnapshot() = runTest {
        stubTerzone(CTO_TERZONE_FIXTURE)
        repo.refresh()
        assertEquals(6, repo.observeZones().first().size)

        coEvery { adapter.get(match { it.endsWith("/terminal/terzone") }) } returns
            Result.failure(V3HttpException(500, "boom"))
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(6, repo.observeZones().first().size) // unchanged
        assertEquals("操场", repo.observeZones().first()[0].name)
    }

    @Test
    fun refresh_malformedJson_isFailure_keepsPriorSnapshot() = runTest {
        stubTerzone(CTO_TERZONE_FIXTURE)
        repo.refresh()

        coEvery { adapter.get(match { it.endsWith("/terminal/terzone") }) } returns
            Result.success("not json at all <<<")
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(6, repo.observeZones().first().size) // unchanged
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    @Test
    fun concurrentRefresh_sharesOneFetch() = runBlocking {
        val dispatcher = newFixedThreadPoolContext(4, "v3-refresh-race")
        val calls = AtomicInteger(0)
        val allInFlight = java.util.concurrent.CountDownLatch(3)
        val barrier = CompletableDeferred<Unit>()
        coEvery { adapter.get(match { it.endsWith("/terminal/terzone") }) } coAnswers {
            calls.incrementAndGet()
            barrier.await()
            Result.success("""{"data":[{"id":1,"name":"Z1","terminal":[]}]}""")
        }

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

        assertEquals(1, calls.get()) // one shared fetch, not three
        assertTrue(results.all { it.isSuccess })
        assertEquals(1, sharedRepo.observeZones().first().size)
    }

    @Test
    fun refresh_buildsAbsoluteV3Url_terzoneOnly() = runTest {
        // The fix uses ONLY /terminal/terzone — assert that exact URL.
        coEvery { adapter.get("http://10.0.0.1:8080/api/terminal/terzone") } returns
            Result.success("""{"data":[]}""")

        val result = repo.refresh()
        assertTrue(result.isSuccess) // exact-URL stub matched
    }
}
