package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import android.content.Context
import com.google.gson.GsonBuilder
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTaskStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.V3CallbackAdapter
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.V3HttpException
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the REAL [V3TaskRepository] (Plan A / PA-10 + PA-15 fix).
 *
 * ★ PA-15 Leg-4 (data fidelity) — uses verbatim subsets of the CTO Bearer-token
 * captures as fixtures:
 *   - `.state/api-snapshots/sechinfo-cto-capture-2026-05-30.json`
 *     (6 schemes; 海王作息 + 日本作息 both projectstate=0, multi-active confirmed)
 *   - `.state/api-snapshots/sechetaskinfo-haiwang-cto-capture-2026-05-30.json`
 *     (14 per-task rows for 海王作息: 早读开始铃 / 第一节课上课铃 / ...)
 * Subsets are embedded as Kotlin strings so the test is self-contained and
 * CI-safe (does not depend on a runtime file). Same pattern as PA-14
 * V3TerminalRepositoryTest using terzone-cto-capture.
 *
 * Load-bearing regression guards:
 *   - refresh_success_groundTruth_haiwangHas14NamedTasks — if PA-15 ever
 *     regresses (someone refactors back to single-endpoint or loses /sechetaskinfo
 *     wiring), this assertion catches it by name.
 *   - refresh_success_multiActive_bothHaiwangAndRibenAreActive — 多 active 真
 *     现实 (CTO capture proved it); the per-scheme active flag MUST be true for
 *     both.
 *   - refresh_anyPerSchemeTaskFetchFails_isFailure_keepsPriorSnapshot — atomic
 *     publish discipline (V3TerminalRepository contract).
 *
 * Also covers the SSOT contract (empty start, in-flight dedup, malformed JSON
 * keeps snapshot) and setSchemeActive POST shape (sechename+state, I-3 re-fetch).
 */
class V3TaskRepositoryTest {

    private val context: Context = mockk(relaxed = true)
    private val adapter: V3CallbackAdapter = mockk()
    private val gson = GsonBuilder().setLenient().create() // mirrors NetworkModule.provideGson

    private val serverConfig = object : ServerConfig {
        private var url = "http://10.0.0.1:8080/api"
        override fun baseUrl() = url
        override fun setBaseUrl(value: String) { url = value }
    }
    private lateinit var repo: V3TaskRepository

    @Before
    fun setup() {
        val dispatcher = UnconfinedTestDispatcher()
        repo = V3TaskRepository(
            context, adapter, gson, serverConfig, CoroutineScope(dispatcher), dispatcher,
        )
    }

    // ─── CTO ground-truth fixtures (verbatim subsets) ─────────────────────

    /** /sechinfo capture: 6 schemes, 海王 + 日本 both projectstate=0 (multi-active). */
    private val SECHINFO_FIXTURE = """
        {"data":[
          {"all":6,"count":1,"start":1,"state":0,"taskid":73795,"taskstate":0,"taskcount":14,"startdate":"2026-01-18","enddate":"2039-01-31","sechename":"小学测试作息","projectstate":1},
          {"all":6,"count":1,"start":2,"state":0,"taskid":73823,"taskstate":0,"taskcount":14,"startdate":"2026-01-18","enddate":"2039-01-31","sechename":"最终测试作息","projectstate":1},
          {"all":6,"count":1,"start":3,"state":0,"taskid":73683,"taskstate":0,"taskcount":14,"startdate":"2026-01-18","enddate":"2039-01-31","sechename":"海王作息","projectstate":0},
          {"all":6,"count":1,"start":4,"state":0,"taskid":73711,"taskstate":0,"taskcount":14,"startdate":"2026-01-18","enddate":"2039-01-31","sechename":"日本作息","projectstate":0},
          {"all":6,"count":1,"start":5,"state":0,"taskid":73739,"taskstate":0,"taskcount":14,"startdate":"2026-01-18","enddate":"2039-01-31","sechename":"股市作息","projectstate":1},
          {"all":6,"count":1,"start":6,"state":0,"taskid":73767,"taskstate":0,"taskcount":14,"startdate":"2026-01-18","enddate":"2039-01-31","sechename":"中学测试作息","projectstate":1}
        ]}
    """.trimIndent()

    /** /sechetaskinfo[海王作息] capture: 14 rows. The 14 names + starttimes are
     *  the load-bearing regression guard. Per-task field set is the FULL DTO
     *  coverage per `field_inventory_full_dto_coverage_required` of the capture. */
    private val SECHETASKINFO_HAIWANG_FIXTURE = """
        {"data":[
          {"all":14,"count":1,"start":1,"state":0,"taskstate":0,"lengthtype":1,"execmode":62,"volume":80,"length":20,"starttime":"07:50:00","startdate":"2026-01-18","enddate":"2039-01-31","taskid":73657,"prepower":15,"priority":10,"info":"海王作息","name":"早读开始铃","mediaid":130,"medianame":"上课铃.mp3","enablestate":1,"offlinestate":0,"israndomplay":0,"datasendmodel":0},
          {"all":14,"count":1,"start":2,"state":0,"taskstate":0,"lengthtype":1,"execmode":62,"volume":80,"length":20,"starttime":"08:20:00","startdate":"2026-01-14","enddate":"2039-01-31","taskid":73659,"prepower":15,"priority":10,"info":"海王作息","name":"第一节课上课铃","mediaid":130,"medianame":"上课铃.mp3","enablestate":1,"offlinestate":0,"israndomplay":0,"datasendmodel":0},
          {"all":14,"count":1,"start":3,"taskid":73661,"name":"第一节课下课铃","starttime":"09:00:00","medianame":"下课铃.mp3","volume":80,"enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":4,"taskid":73663,"name":"第二节课上课铃","starttime":"09:10:00","medianame":"上课铃.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":5,"taskid":73665,"name":"大课间","starttime":"09:50:00","medianame":"大课间.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":6,"taskid":73667,"name":"第三节课上课铃","starttime":"10:10:00","medianame":"上课铃.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":7,"taskid":73669,"name":"第三节课下课铃","starttime":"10:50:00","medianame":"下课铃.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":8,"taskid":73671,"name":"第四节课上课铃","starttime":"11:00:00","medianame":"上课铃.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":9,"taskid":73673,"name":"第四节课下课铃","starttime":"11:40:00","medianame":"下课铃.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":10,"taskid":73675,"name":"午休结束铃","starttime":"13:50:00","medianame":"下课铃.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":11,"taskid":73677,"name":"下午第一节课上课","starttime":"14:25:00","medianame":"上课铃.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":12,"taskid":73679,"name":"下午第一节课下课","starttime":"15:05:00","medianame":"下课铃.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":13,"taskid":73681,"name":"下午第二节课上课","starttime":"15:15:00","medianame":"上课铃.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0},
          {"all":14,"count":1,"start":14,"taskid":73683,"name":"快乐放学季","starttime":"16:15:00","medianame":"下课铃.mp3","enablestate":1,"state":0,"taskstate":0,"offlinestate":0}
        ]}
    """.trimIndent()

    /** Empty per-task envelope for schemes other than 海王. */
    private val EMPTY_SCHEMETASKS = """{"data":[]}"""

    private fun stubSechinfo(json: String) {
        coEvery {
            adapter.get(match { it.endsWith("/task/sechinfo") })
        } returns Result.success(json)
    }

    /** Default: 海王 returns the full 14-task fixture, every other scheme returns
     *  empty `{data:[]}`. Tests that need different per-scheme responses override
     *  AFTER calling this. */
    private fun stubAllSechetaskinfo(haiwangJson: String = SECHETASKINFO_HAIWANG_FIXTURE) {
        coEvery {
            adapter.post(match { req ->
                req.url.endsWith("/task/sechetaskinfo") &&
                    req.bodyMap?.get("name") == "海王作息"
            })
        } returns Result.success(haiwangJson)
        coEvery {
            adapter.post(match { req ->
                req.url.endsWith("/task/sechetaskinfo") &&
                    req.bodyMap?.get("name") != "海王作息"
            })
        } returns Result.success(EMPTY_SCHEMETASKS)
    }

    // ─── SSOT contract ────────────────────────────────────────────────────

    @Test
    fun observeSchemes_startsEmpty() = runTest {
        assertEquals(emptyList<Any>(), repo.observeSchemes().first())
    }

    /** ★ PA-15 load-bearing regression guard: 海王作息 must surface exactly the
     *  14 named timeline tasks from the CTO capture. If this assertion fails,
     *  the BLOCKER from 2026-05-30 has regressed. */
    @Test
    fun refresh_success_groundTruth_haiwangHas14NamedTasks() = runTest {
        stubSechinfo(SECHINFO_FIXTURE)
        stubAllSechetaskinfo()

        val result = repo.refresh()

        assertTrue(result.isSuccess)
        val schemes = repo.observeSchemes().first()
        assertEquals(6, schemes.size)

        val haiwang = schemes.first { it.id == "海王作息" }
        assertTrue(haiwang.active)
        assertEquals(14, haiwang.tasks.size)

        val expectedNames = listOf(
            "早读开始铃", "第一节课上课铃", "第一节课下课铃", "第二节课上课铃",
            "大课间", "第三节课上课铃", "第三节课下课铃", "第四节课上课铃",
            "第四节课下课铃", "午休结束铃", "下午第一节课上课", "下午第一节课下课",
            "下午第二节课上课", "快乐放学季",
        )
        assertEquals(expectedNames, haiwang.tasks.map { it.name })

        // Spot-check fields on the first task (full row fixture).
        val first = haiwang.tasks[0]
        assertEquals("73657", first.id)
        assertEquals("早读开始铃", first.name)
        assertEquals("07:50:00", first.startTime)
        assertEquals("上课铃.mp3", first.mediaName)
        assertEquals(80, first.volume)
    }

    /** ★ PA-15: taskcount integer reconciles with /sechetaskinfo row count. */
    @Test
    fun refresh_success_taskCountMatchesTasksSize_forHaiwang() = runTest {
        stubSechinfo(SECHINFO_FIXTURE)
        stubAllSechetaskinfo()
        repo.refresh()

        val haiwang = repo.observeSchemes().first().first { it.id == "海王作息" }
        // sechinfo.taskcount for 海王 was 14; per-task fixture has 14 rows.
        assertEquals(14, haiwang.tasks.size)
    }

    /** ★ PA-15: MULTIPLE active schemes — CTO capture proved 海王 + 日本 both
     *  have projectstate=0 on the live wire. */
    @Test
    fun refresh_success_multiActive_bothHaiwangAndRibenAreActive() = runTest {
        stubSechinfo(SECHINFO_FIXTURE)
        stubAllSechetaskinfo()
        repo.refresh()

        val schemes = repo.observeSchemes().first()
        val activeSchemes = schemes.filter { it.active }
        assertEquals(2, activeSchemes.size)
        assertEquals(setOf("海王作息", "日本作息"), activeSchemes.map { it.id }.toSet())

        // Per-scheme active flag is correct for stopped schemes too.
        assertFalse(schemes.first { it.id == "小学测试作息" }.active)
    }

    /** ★ PA-15 (atomic publish discipline): if /sechetaskinfo fails for ANY
     *  scheme, the whole refresh fails and the prior snapshot is retained. */
    @Test
    fun refresh_anyPerSchemeTaskFetchFails_isFailure_keepsPriorSnapshot() = runTest {
        // Seed a good snapshot.
        stubSechinfo(SECHINFO_FIXTURE)
        stubAllSechetaskinfo()
        repo.refresh()
        assertEquals(6, repo.observeSchemes().first().size)
        assertEquals(
            14,
            repo.observeSchemes().first().first { it.id == "海王作息" }.tasks.size,
        )

        // Now fail ONLY 海王's sechetaskinfo (other schemes still succeed).
        coEvery {
            adapter.post(match { req ->
                req.url.endsWith("/task/sechetaskinfo") &&
                    req.bodyMap?.get("name") == "海王作息"
            })
        } returns Result.failure(V3HttpException(500, "boom"))
        val result = repo.refresh()

        assertTrue(result.isFailure)
        // Prior snapshot retained.
        assertEquals(6, repo.observeSchemes().first().size)
        assertEquals(
            14,
            repo.observeSchemes().first().first { it.id == "海王作息" }.tasks.size,
        )
    }

    @Test
    fun refresh_sechinfoNetworkFail_isFailure_keepsPriorSnapshot() = runTest {
        stubSechinfo(SECHINFO_FIXTURE)
        stubAllSechetaskinfo()
        repo.refresh()
        assertEquals(6, repo.observeSchemes().first().size)

        coEvery { adapter.get(match { it.endsWith("/task/sechinfo") }) } returns
            Result.failure(V3HttpException(500, "boom"))
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(6, repo.observeSchemes().first().size) // unchanged
    }

    @Test
    fun refresh_malformedSechinfoJson_isFailure_keepsPriorSnapshot() = runTest {
        stubSechinfo(SECHINFO_FIXTURE)
        stubAllSechetaskinfo()
        repo.refresh()

        coEvery { adapter.get(match { it.endsWith("/task/sechinfo") }) } returns
            Result.success("not json at all <<<")
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(6, repo.observeSchemes().first().size) // unchanged
    }

    @Test
    fun refresh_malformedSechetaskinfoJson_isFailure_keepsPriorSnapshot() = runTest {
        stubSechinfo(SECHINFO_FIXTURE)
        stubAllSechetaskinfo()
        repo.refresh()

        coEvery {
            adapter.post(match { req ->
                req.url.endsWith("/task/sechetaskinfo") &&
                    req.bodyMap?.get("name") == "海王作息"
            })
        } returns Result.success("not json at all <<<")
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(6, repo.observeSchemes().first().size) // unchanged
        assertEquals(
            14,
            repo.observeSchemes().first().first { it.id == "海王作息" }.tasks.size,
        )
    }

    // ─── /sechetaskinfo POST body shape ───────────────────────────────────

    @Test
    fun refresh_sechetaskinfoPostsNameBodyKeyedBySechename() = runTest {
        stubSechinfo(SECHINFO_FIXTURE)
        // Capture EVERY sechetaskinfo POST body to verify the form key + value.
        val captured = mutableListOf<String?>()
        coEvery {
            adapter.post(match { req -> req.url.endsWith("/task/sechetaskinfo") })
        } coAnswers {
            val builder = firstArg<MyRequestBuilder>()
            captured.add(builder.bodyMap?.get("name"))
            Result.success(EMPTY_SCHEMETASKS)
        }

        repo.refresh()

        // Each of the 6 sechinfo schemes triggers a /sechetaskinfo POST keyed by name.
        assertEquals(6, captured.size)
        assertEquals(
            setOf("小学测试作息", "最终测试作息", "海王作息", "日本作息", "股市作息", "中学测试作息"),
            captured.toSet(),
        )
    }

    @Test
    fun refresh_buildsAbsoluteV3UrlsForBothEndpoints() = runTest {
        coEvery { adapter.get("http://10.0.0.1:8080/api/task/sechinfo") } returns
            Result.success("""{"data":[]}""")  // no schemes → no sechetaskinfo POSTs needed
        val result = repo.refresh()
        assertTrue(result.isSuccess) // exact-URL stub matched

        // And the post URL too — fire a quick second refresh with one scheme.
        stubSechinfo("""{"data":[{"sechename":"x","projectstate":0}]}""")
        coEvery {
            adapter.post(match { req ->
                req.url == "http://10.0.0.1:8080/api/task/sechetaskinfo"
            })
        } returns Result.success(EMPTY_SCHEMETASKS)
        assertTrue(repo.refresh().isSuccess)
    }

    // ─── setSchemeActive (PA-10 contract — preserved) ─────────────────────

    @Test
    fun setSchemeActive_enable_postsSechenameState0_thenRefreshes() = runTest {
        // Capture the enable POST builder. After-toggle refresh hits sechinfo +
        // sechetaskinfo, so stub those too.
        val builderSlot = slot<MyRequestBuilder>()
        coEvery {
            adapter.post(match { it.url.endsWith("/task/sechenableordisable") })
        } coAnswers {
            builderSlot.captured = firstArg()
            Result.success("""{"data":[{"state":0}]}""")  // ChangeSucess
        }
        stubSechinfo(SECHINFO_FIXTURE)
        stubAllSechetaskinfo()

        val result = repo.setSchemeActive("海王作息", active = true)

        assertTrue(result.isSuccess)
        val body = builderSlot.captured.bodyMap
        assertEquals("海王作息", body?.get("sechename"))
        assertEquals("0", body?.get("state"))   // enable → state 0 (pinned sign)
        assertTrue(builderSlot.captured.url.endsWith("/task/sechenableordisable"))
        // I-3: SSOT reflects re-fetch, not a local flip.
        assertTrue(repo.observeSchemes().first().first { it.id == "海王作息" }.active)
    }

    @Test
    fun setSchemeActive_disable_postsState1() = runTest {
        val builderSlot = slot<MyRequestBuilder>()
        coEvery {
            adapter.post(match { it.url.endsWith("/task/sechenableordisable") })
        } coAnswers {
            builderSlot.captured = firstArg()
            Result.success("""{"data":[{"state":15}]}""") // TheStateIsSame also OK
        }
        stubSechinfo("""{"data":[]}""") // re-fetch returns no schemes → no sechetaskinfo POSTs

        val result = repo.setSchemeActive("日本作息", active = false)

        assertTrue(result.isSuccess)
        assertEquals("1", builderSlot.captured.bodyMap?.get("state")) // disable → state 1
    }

    @Test
    fun setSchemeActive_serverRejects_isFailure() = runTest {
        coEvery {
            adapter.post(match { it.url.endsWith("/task/sechenableordisable") })
        } returns Result.success("""{"data":[{"state":99}]}""")  // not 0/15 → failed

        val result = repo.setSchemeActive("海王作息", active = true)
        assertTrue(result.isFailure)
    }

    @Test
    fun setSchemeActive_postFails_isFailure() = runTest {
        coEvery {
            adapter.post(match { it.url.endsWith("/task/sechenableordisable") })
        } returns Result.failure(V3HttpException(500, "boom"))

        val result = repo.setSchemeActive("海王作息", active = true)
        assertTrue(result.isFailure)
    }

    // ─── Status derivation ────────────────────────────────────────────────

    @Test
    fun derivedStatus_haiwangActiveTasks_areIdle_underActiveScheme() = runTest {
        // CTO capture: all 14 海王 tasks have taskstate=0 + enablestate=1 + scheme
        // active → SchemeMapper.deriveTaskStatus → Idle (scheduled, not firing now).
        stubSechinfo(SECHINFO_FIXTURE)
        stubAllSechetaskinfo()
        repo.refresh()

        val haiwang = repo.observeSchemes().first().first { it.id == "海王作息" }
        assertTrue(haiwang.tasks.all { it.status == SchemeTaskStatus.Idle })
    }

    // ─── getExecutionLog (PA-10 contract — preserved) ─────────────────────

    @Test
    fun getExecutionLog_returnsEmpty_noServerLogSource() = runTest {
        val result = repo.getExecutionLog()
        assertTrue(result.isSuccess)
        assertEquals(emptyList<Any>(), result.getOrNull())
    }
}
