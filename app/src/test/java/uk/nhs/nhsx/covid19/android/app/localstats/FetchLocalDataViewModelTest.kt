package uk.nhs.nhsx.covid19.android.app.localstats

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Lce.Error
import uk.nhs.nhsx.covid19.android.app.common.Lce.Loading
import uk.nhs.nhsx.covid19.android.app.common.Lce.Success
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalStatsResponse
import java.io.IOException
import java.lang.IllegalStateException

@ExperimentalCoroutinesApi
class FetchLocalDataViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)
    private val localStatsObserver = mockk<Observer<Lce<LocalStats>>>(relaxUnitFun = true)
    private val fetchLocalStats = mockk<FetchLocalStats>()
    private val localStatsMapper = mockk<LocalStatsMapper>()
    private val testSubject = FetchLocalDataViewModel(testScope, fetchLocalStats, localStatsMapper)

    @Before
    fun setUp() {
        testSubject.localStats().observeForever(localStatsObserver)
        coEvery { fetchLocalStats() } returns mockk()
    }

    @After
    fun tearDown() {
        testScope.cleanupTestCoroutines()
    }

    @Test
    fun `successfully fetch data`() = runBlocking {
        val localStatsResponse = mockk<LocalStatsResponse>()
        coEvery { fetchLocalStats() } returns localStatsResponse

        val localStats = mockk<LocalStats>()
        coEvery { localStatsMapper.map(localStatsResponse) } returns localStats

        testSubject.loadData()

        coVerify(exactly = 1) { fetchLocalStats() }

        verifyOrder {
            localStatsObserver.onChanged(Loading)
            localStatsObserver.onChanged(Success(localStats))
        }
    }

    @Test
    fun `error on fetching data`() = runBlocking {
        val ioException = IOException()
        coEvery { fetchLocalStats() } throws ioException

        testSubject.loadData()

        coVerify(exactly = 1) { fetchLocalStats() }

        verifyOrder {
            localStatsObserver.onChanged(Loading)
            localStatsObserver.onChanged(Error(ioException))
        }
    }

    @Test
    fun `error on mapping data`() = runBlocking {
        val localStatsResponse = mockk<LocalStatsResponse>()
        coEvery { fetchLocalStats() } returns localStatsResponse

        val illegalStateException = IllegalStateException()
        coEvery { localStatsMapper.map(localStatsResponse) } throws illegalStateException

        testSubject.loadData()

        coVerify(exactly = 1) { fetchLocalStats() }

        verifyOrder {
            localStatsObserver.onChanged(Loading)
            localStatsObserver.onChanged(Error(illegalStateException))
        }
    }
}
