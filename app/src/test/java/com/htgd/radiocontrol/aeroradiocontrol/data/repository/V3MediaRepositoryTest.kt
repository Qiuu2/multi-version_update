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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [V3MediaRepository] (Plan A / TASK-PA-07, LIST half).
 *
 * Verifies the SSOT contract mirrored from V3TerminalRepository (Critic AC): empty
 * start, success fills + joins media under folders + re-emits, failure keeps prior
 * snapshot, the v3 raw-JSON → reverse-engineered DTO → Mapper → domain pipeline,
 * and MediaDto/MediaFolderDto field fidelity (incl. root-folder parentId nulling).
 */
class V3MediaRepositoryTest {

    private val adapter: V3CallbackAdapter = mockk()
    private val gson = GsonBuilder().setLenient().create() // mirrors NetworkModule.provideGson

    // Fake ServerConfig: in-memory base URL so the test never loads v3 `Constant`.
    private val serverConfig = object : ServerConfig {
        private var url = "http://10.0.0.1:8080/api"
        override fun baseUrl() = url
        override fun setBaseUrl(value: String) { url = value }
        // Return a non-blank token so the NO_SESSION guard in V3CallbackAdapter
        // doesn't fire on happy-path tests (tests that verify failure inject
        // the adapter mock directly to return Result.failure, bypassing the guard).
        override fun authToken() = "Bearer test-token"
        override fun setAuthToken(bearerToken: String) { /* no-op in tests */ }
    }
    private lateinit var repo: V3MediaRepository

    @Before
    fun setup() {
        val dispatcher = UnconfinedTestDispatcher()
        repo = V3MediaRepository(adapter, gson, serverConfig, CoroutineScope(dispatcher), dispatcher)
    }

    private fun stubFolders(json: String) =
        coEvery { adapter.get(match { it.endsWith("/terminal/mediafolderinfo") }) } returns Result.success(json)

    private fun stubMedia(json: String) =
        coEvery { adapter.get(match { it.endsWith("/terminal/mediainfo") }) } returns Result.success(json)

    @Test
    fun observeFolders_startsEmpty() = runTest {
        assertEquals(emptyList<Any>(), repo.observeFolders().first())
    }

    @Test
    fun refresh_success_parsesV3Json_joinsMedia_mapsFields_andReEmits() = runTest {
        // parentid 3 (root) → null; parentid 10 → "10". Two folders, media joined by folderid.
        stubFolders(
            """{"data":[
                {"folderid":10,"parentid":3,"name":"Pop","count":2},
                {"folderid":11,"parentid":3,"name":"Class","count":1}
            ]}""",
        )
        stubMedia(
            """{"data":[
                {"mediaid":100,"folderid":10,"name":"a.mp3","format":"mp3","length":210,"size":3300000},
                {"mediaid":101,"folderid":10,"name":"b.mp3"},
                {"mediaid":102,"folderid":11,"name":"c.mp3"}
            ]}""",
        )

        val result = repo.refresh()

        assertTrue(result.isSuccess)
        val folders = repo.observeFolders().first()
        assertEquals(2, folders.size)
        val pop = folders.first { it.id == "10" }
        assertNull(pop.parentId) // parentid 3 (root) → null
        assertEquals(listOf("a.mp3", "b.mp3"), pop.media.map { it.name })
        assertEquals(listOf("c.mp3"), folders.first { it.id == "11" }.media.map { it.name })
        // Media field fidelity.
        val a = pop.media.first { it.id == "100" }
        assertEquals("mp3", a.format)
        assertEquals(210, a.durationSeconds)
        assertEquals(3300000, a.sizeBytes)
        assertEquals("10", a.folderId)
    }

    @Test
    fun observeMedia_flattens() = runTest {
        stubFolders("""{"data":[{"folderid":10,"parentid":3,"name":"Pop"}]}""")
        stubMedia("""{"data":[{"mediaid":100,"folderid":10,"name":"a.mp3"}]}""")
        repo.refresh()
        assertEquals(1, repo.observeMedia().first().size)
    }

    @Test
    fun refresh_foldersFail_isFailure_keepsPriorSnapshot() = runTest {
        // Seed a good snapshot.
        stubFolders("""{"data":[{"folderid":10,"parentid":3,"name":"Pop"}]}""")
        stubMedia("""{"data":[{"mediaid":100,"folderid":10,"name":"a.mp3"}]}""")
        repo.refresh()
        assertEquals(1, repo.observeFolders().first().size)

        // Now folders fail → failure, prior snapshot retained.
        coEvery { adapter.get(match { it.endsWith("/terminal/mediafolderinfo") }) } returns
            Result.failure(V3HttpException(500, "boom"))
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(1, repo.observeFolders().first().size)
        assertEquals("Pop", repo.observeFolders().first()[0].name)
    }

    @Test
    fun refresh_malformedJson_isFailure_keepsPriorSnapshot() = runTest {
        stubFolders("""{"data":[{"folderid":10,"parentid":3,"name":"Pop"}]}""")
        stubMedia("""{"data":[{"mediaid":100,"folderid":10,"name":"a.mp3"}]}""")
        repo.refresh()

        // Garbage media JSON → parse throws → failure, snapshot kept.
        coEvery { adapter.get(match { it.endsWith("/terminal/mediainfo") }) } returns
            Result.success("not json at all <<<")
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(1, repo.observeFolders().first().size) // unchanged
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    @Test
    fun concurrentRefresh_sharesOneFetch() = runBlocking {
        // REAL multi-threaded dispatcher (not runTest's eager virtual scheduler) so
        // the Mutex + shared Deferred behave as in production — mirrors
        // V3TerminalRepositoryTest's idiom.
        val dispatcher = newFixedThreadPoolContext(4, "v3-media-race")
        val folderCalls = AtomicInteger(0)
        val allInFlight = java.util.concurrent.CountDownLatch(3)
        val barrier = CompletableDeferred<Unit>()
        coEvery { adapter.get(match { it.endsWith("/terminal/mediafolderinfo") }) } coAnswers {
            folderCalls.incrementAndGet()
            barrier.await()
            Result.success("""{"data":[{"folderid":10,"parentid":3,"name":"Pop"}]}""")
        }
        stubMedia("""{"data":[{"mediaid":100,"folderid":10,"name":"a.mp3"}]}""")

        val sharedRepo = V3MediaRepository(
            adapter, gson, serverConfig, CoroutineScope(dispatcher), dispatcher,
        )

        val deferreds = (1..3).map {
            async(dispatcher) { allInFlight.countDown(); sharedRepo.refresh() }
        }
        allInFlight.await(2, java.util.concurrent.TimeUnit.SECONDS)
        barrier.complete(Unit)
        val results = deferreds.awaitAll()
        dispatcher.close()

        assertEquals(1, folderCalls.get()) // one shared fetch, not three
        assertTrue(results.all { it.isSuccess })
        assertEquals(1, sharedRepo.observeFolders().first().size)
    }

    @Test
    fun refresh_buildsAbsoluteV3Urls() = runTest {
        // Asserts the repo targets ServerConfig.baseUrl() + path (v3 URL shape).
        coEvery { adapter.get("http://10.0.0.1:8080/api/terminal/mediafolderinfo") } returns
            Result.success("""{"data":[]}""")
        coEvery { adapter.get("http://10.0.0.1:8080/api/terminal/mediainfo") } returns
            Result.success("""{"data":[]}""")

        val result = repo.refresh()
        assertTrue(result.isSuccess) // exact-URL stubs matched
    }
}
