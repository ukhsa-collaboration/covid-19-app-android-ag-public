package uk.nhs.nhsx.covid19.android.app.localstats

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.LocalStatsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalStatsResponse
import kotlin.test.assertEquals

class FetchLocalStatsTest {

    private val localStatsApi = mockk<LocalStatsApi>()
    private val testSubject = FetchLocalStats(localStatsApi)

    @Test
    fun `calls network API`() = runBlocking {
        coEvery { localStatsApi.fetchLocalStats() } returns mockk()

        testSubject()

        coVerify(exactly = 1) { localStatsApi.fetchLocalStats() }
    }

    @Test
    fun `returns API response`() = runBlocking {
        val mockk = mockk<LocalStatsResponse>()
        coEvery { localStatsApi.fetchLocalStats() } returns mockk

        val result = testSubject()

        assertEquals(mockk, result)
    }
}
