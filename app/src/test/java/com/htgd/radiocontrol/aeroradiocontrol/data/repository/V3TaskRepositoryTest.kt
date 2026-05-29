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
 * Unit tests for the REAL [V3TaskRepository] (Plan A / TASK-PA-10).
 *
 * Verifies the SSOT contract mirrored from V3TerminalRepository (Critic AC): empty
 * start, success groups flat /task/sechinfo rows into nested schemes + re-emits,
 * failure keeps prior snapshot; the projectstate→active + taskstate→status
 * derivations (pinned against real v3, incl. the 0==running sign); and
 * setSchemeActive POSTing sechename+state then re-fetching (I-3).
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

    private fun stubSchemes(json: String) =
        coEvery { adapter.get(match { it.endsWith("/task/sechinfo") }) } returns Result.success(json)

    @Test
    fun observeSchemes_startsEmpty() = runTest {
        assertEquals(emptyList<Any>(), repo.observeSchemes().first())
    }

    @Test
    fun refresh_success_groupsRowsBySechename_derivesStatus_andReEmits() = runTest {
        // Scheme "Morning" running (projectstate 0): two tasks (one firing, one idle).
        // Scheme "Night" stopped (projectstate 1): one task → Disabled.
        stubSchemes(
            """{"data":[
                {"taskid":"1","taskname":"bell","sechename":"Morning","projectstate":0,"taskstate":1,"starttime":"08:00","medianame":"a.mp3","volume":60},
                {"taskid":"2","taskname":"music","sechename":"Morning","projectstate":0,"taskstate":0},
                {"taskid":"3","taskname":"lights","sechename":"Night","projectstate":1,"taskstate":0}
            ]}""",
        )

        val result = repo.refresh()

        assertTrue(result.isSuccess)
        val schemes = repo.observeSchemes().first()
        assertEquals(2, schemes.size)

        val morning = schemes.first { it.id == "Morning" }
        assertTrue(morning.active)              // projectstate 0 → active (pinned: 0==running)
        assertEquals(2, morning.tasks.size)
        assertEquals(SchemeTaskStatus.Running, morning.tasks.first { it.id == "1" }.status)
        assertEquals(SchemeTaskStatus.Idle, morning.tasks.first { it.id == "2" }.status)
        // Field fidelity on the firing task.
        val bell = morning.tasks.first { it.id == "1" }
        assertEquals("bell", bell.name)
        assertEquals("08:00", bell.startTime)
        assertEquals("a.mp3", bell.mediaName)
        assertEquals(60, bell.volume)

        val night = schemes.first { it.id == "Night" }
        assertFalse(night.active)               // projectstate 1 → not active
        assertEquals(SchemeTaskStatus.Disabled, night.tasks.first().status)
    }

    @Test
    fun refresh_networkFail_isFailure_keepsPriorSnapshot() = runTest {
        stubSchemes("""{"data":[{"taskid":"1","taskname":"bell","sechename":"Morning","projectstate":0}]}""")
        repo.refresh()
        assertEquals(1, repo.observeSchemes().first().size)

        coEvery { adapter.get(match { it.endsWith("/task/sechinfo") }) } returns
            Result.failure(V3HttpException(500, "boom"))
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(1, repo.observeSchemes().first().size)
        assertEquals("Morning", repo.observeSchemes().first()[0].name)
    }

    @Test
    fun refresh_malformedJson_isFailure_keepsPriorSnapshot() = runTest {
        stubSchemes("""{"data":[{"taskid":"1","taskname":"bell","sechename":"Morning","projectstate":0}]}""")
        repo.refresh()

        coEvery { adapter.get(match { it.endsWith("/task/sechinfo") }) } returns
            Result.success("not json at all <<<")
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(1, repo.observeSchemes().first().size) // unchanged
    }

    @Test
    fun setSchemeActive_enable_postsSechenameState0_thenRefreshes() = runTest {
        // Capture the POST builder to assert the wire (sechename + state).
        val builderSlot = slot<MyRequestBuilder>()
        coEvery { adapter.post(capture(builderSlot)) } returns
            Result.success("""{"data":[{"state":0}]}""")  // ChangeSucess
        // After-toggle re-fetch (I-3) returns the now-active scheme.
        stubSchemes("""{"data":[{"taskid":"1","taskname":"bell","sechename":"Morning","projectstate":0}]}""")

        val result = repo.setSchemeActive("Morning", active = true)

        assertTrue(result.isSuccess)
        val body = builderSlot.captured.bodyMap
        assertEquals("Morning", body["sechename"])
        assertEquals("0", body["state"])              // enable → state 0 (pinned sign)
        assertTrue(builderSlot.captured.url.endsWith("/task/sechenableordisable"))
        // I-3: SSOT reflects the server re-fetch, not a local flip.
        assertTrue(repo.observeSchemes().first().first { it.id == "Morning" }.active)
    }

    @Test
    fun setSchemeActive_disable_postsState1() = runTest {
        val builderSlot = slot<MyRequestBuilder>()
        coEvery { adapter.post(capture(builderSlot)) } returns
            Result.success("""{"data":[{"state":15}]}""")  // TheStateIsSame also OK
        stubSchemes("""{"data":[]}""")

        val result = repo.setSchemeActive("Night", active = false)

        assertTrue(result.isSuccess)
        assertEquals("1", builderSlot.captured.bodyMap["state"]) // disable → state 1
    }

    @Test
    fun setSchemeActive_serverRejects_isFailure() = runTest {
        coEvery { adapter.post(any()) } returns
            Result.success("""{"data":[{"state":99}]}""")  // not 0/15 → failed

        val result = repo.setSchemeActive("Morning", active = true)
        assertTrue(result.isFailure)
    }

    @Test
    fun setSchemeActive_postFails_isFailure() = runTest {
        coEvery { adapter.post(any()) } returns Result.failure(V3HttpException(500, "boom"))

        val result = repo.setSchemeActive("Morning", active = true)
        assertTrue(result.isFailure)
    }

    @Test
    fun getExecutionLog_returnsEmpty_noServerLogSource() = runTest {
        // documented-assumption: v3 has no execution-log REST endpoint → empty success.
        val result = repo.getExecutionLog()
        assertTrue(result.isSuccess)
        assertEquals(emptyList<Any>(), result.getOrNull())
    }

    @Test
    fun refresh_buildsAbsoluteV3Url() = runTest {
        coEvery { adapter.get("http://10.0.0.1:8080/api/task/sechinfo") } returns
            Result.success("""{"data":[]}""")
        val result = repo.refresh()
        assertTrue(result.isSuccess) // exact-URL stub matched
    }
}
