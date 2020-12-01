package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ClearOutdatedDataAndUpdateIsolationConfigurationTest {

    private val stateStorage = mockk<StateStorage>(relaxed = true)
    private val testResultsProvider = mockk<TestResultsProvider>(relaxed = true)
    private val isolationConfigurationProvider =
        mockk<IsolationConfigurationProvider>(relaxed = true)
    private val isolationConfigurationApi = mockk<IsolationConfigurationApi>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)

    private val testSubject = ClearOutdatedDataAndUpdateIsolationConfiguration(
        stateStorage,
        testResultsProvider,
        isolationConfigurationProvider,
        isolationConfigurationApi,
        fixedClock
    )

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns DurationDays(
            pendingTasksRetentionPeriod = 14
        )
        coEvery { isolationConfigurationApi.getIsolationConfiguration() } returns IsolationConfigurationResponse(
            DurationDays()
        )
    }

    @Test
    fun `clears old test results when in default state without previous isolation`() = runBlocking {
        every { stateStorage.state } returns Default()

        testSubject.doWork()

        val retentionPeriod = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { testResultsProvider.clearBefore(expectedDate) }
    }

    @Test
    fun `keeps test result when state expiration date is outdated`() = runBlocking {
        every { stateStorage.state } returns getIndexCase(isOutdated = true)

        testSubject.doWork()

        verify(exactly = 0) { testResultsProvider.clearBefore(any()) }
    }

    @Test
    fun `keeps test result when previous state expiration date in default state is not outdated`() = runBlocking {
        every { stateStorage.state } returns getDefaultState(false)

        testSubject.doWork()

        verify(exactly = 0) { testResultsProvider.clearBefore(any()) }
    }

    @Test
    fun `clears test result when in default state and previous isolation state is outdated`() = runBlocking {
        val state = getDefaultState(true)
        every { stateStorage.state } returns state

        testSubject.doWork()

        verify { testResultsProvider.clearBefore(state.previousIsolation!!.expiryDate) }
    }

    @Test
    fun `verify isolation configuration is updated`() = runBlocking {
        testSubject.doWork()

        verify { isolationConfigurationProvider.durationDays = DurationDays() }
    }

    private fun getIndexCase(isOutdated: Boolean): Isolation =
        Isolation(
            isolationStart = Instant.now(fixedClock),
            isolationConfiguration = DurationDays(),
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.now(fixedClock).minusDays(17),
                expiryDate = LocalDate.now(fixedClock).minusDays(if (isOutdated) 15 else 7),
                selfAssessment = true
            )
        )

    private fun getDefaultState(isOutdated: Boolean): Default =
        Default(previousIsolation = getIndexCase(isOutdated))
}
