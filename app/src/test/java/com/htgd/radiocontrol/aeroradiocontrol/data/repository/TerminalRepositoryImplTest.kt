package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.api.TerminalApi
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TerminalDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TerminalEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.ZoneDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.ZoneEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.network.HttpStatusSuccessPolicy
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for [TerminalRepositoryImpl]: refresh joins terminals into zones,
 * observeZones/observeTerminals reflect the SSOT, failure keeps the prior
 * snapshot.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TerminalRepositoryImplTest {

    private val api: TerminalApi = mockk()
    private val repo = TerminalRepositoryImpl(
        terminalApi = api,
        successPolicy = HttpStatusSuccessPolicy(),
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private fun <T> err(code: Int) =
        Response.error<T>(code, "".toResponseBody("application/json".toMediaType()))

    private fun zonesOk(vararg z: ZoneDto) = Response.success(ZoneEnvelopeDto(z.toList()))
    private fun terminalsOk(vararg t: TerminalDto) =
        Response.success(TerminalEnvelopeDto(t.toList()))

    @Test
    fun refresh_joinsTerminalsIntoZones_andDropsIdless() = runTest {
        coEvery { api.getZones() } returns zonesOk(
            ZoneDto(id = 1, name = "Z1"),
            ZoneDto(id = 2, name = "Z2"),
            ZoneDto(id = null, name = "ghostZone"), // dropped
        )
        coEvery { api.getTerminals() } returns terminalsOk(
            TerminalDto(id = 10, name = "T10", zone = 1, netState = 1),
            TerminalDto(id = 11, name = "T11", zone = 1, netState = 0),
            TerminalDto(id = 12, name = "T12", zone = 2, netState = 1),
            TerminalDto(id = null, name = "ghostT"), // dropped
        )

        val result = repo.refresh()

        assertTrue(result.isSuccess)
        val zones = repo.observeZones().first()
        assertEquals(2, zones.size) // ghost zone dropped
        assertEquals(listOf("T10", "T11"), zones.first { it.id == "1" }.terminals.map { it.name })
        assertEquals(listOf("T12"), zones.first { it.id == "2" }.terminals.map { it.name })
    }

    @Test
    fun observeTerminals_flattensAcrossZones() = runTest {
        coEvery { api.getZones() } returns zonesOk(ZoneDto(id = 1, name = "Z1"))
        coEvery { api.getTerminals() } returns terminalsOk(
            TerminalDto(id = 10, name = "T10", zone = 1, netState = 1),
            TerminalDto(id = 11, name = "T11", zone = 1, netState = 1),
        )

        repo.refresh()

        assertEquals(2, repo.observeTerminals().first().size)
    }

    @Test
    fun refresh_zonesNon2xx_isFailure_andKeepsPriorSnapshot() = runTest {
        // First a good refresh to seed the SSOT.
        coEvery { api.getZones() } returns zonesOk(ZoneDto(id = 1, name = "Z1"))
        coEvery { api.getTerminals() } returns terminalsOk(
            TerminalDto(id = 10, name = "T10", zone = 1, netState = 1),
        )
        repo.refresh()
        assertEquals(1, repo.observeZones().first().size)

        // Now zones fail; the previous snapshot must remain.
        coEvery { api.getZones() } returns err(500)
        val result = repo.refresh()

        assertTrue(result.isFailure)
        assertEquals(1, repo.observeZones().first().size) // unchanged
        assertEquals("Z1", repo.observeZones().first()[0].name)
    }

    @Test
    fun refresh_terminalsNon2xx_isFailure() = runTest {
        coEvery { api.getZones() } returns zonesOk(ZoneDto(id = 1, name = "Z1"))
        coEvery { api.getTerminals() } returns err(503)
        assertTrue(repo.refresh().isFailure)
    }

    @Test
    fun initialObserve_isEmptyBeforeRefresh() = runTest {
        assertEquals(emptyList<Any>(), repo.observeZones().first())
    }
}
