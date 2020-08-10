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
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResultProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ClearOutdatedDataAndUpdateIsolationConfigurationTest {

    private val stateStorage = mockk<StateStorage>(relaxed = true)
    private val testResultProvider = mockk<LatestTestResultProvider>(relaxed = true)
    private val isolationConfigurationProvider =
        mockk<IsolationConfigurationProvider>(relaxed = true)
    private val isolationConfigurationApi = mockk<IsolationConfigurationApi>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)

    private val testSubject = ClearOutdatedDataAndUpdateIsolationConfiguration(
        stateStorage,
        testResultProvider,
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
    fun `updates the state history when state expiration date is outdated`() = runBlocking {
        every { stateStorage.getHistory() } returns listOf(getIndexCase(isOutdated = true))

        testSubject.doWork()

        verify { stateStorage.updateHistory(emptyList()) }
    }

    @Test
    fun `keeps the state history when state expiration date is not outdated`() = runBlocking {
        val initialHistory = listOf(getIndexCase(isOutdated = false))
        every { stateStorage.getHistory() } returns initialHistory

        testSubject.doWork()

        verify { stateStorage.updateHistory(initialHistory) }
    }

    @Test
    fun `clears test result when test end date is outdated`() = runBlocking {
        every { testResultProvider.latestTestResult } returns getLatestTestResult(isOutdated = true)

        testSubject.doWork()

        verify { testResultProvider.latestTestResult = null }
    }

    @Test
    fun `keeps test result when test end date is not outdated`() = runBlocking {
        every { testResultProvider.latestTestResult } returns getLatestTestResult(isOutdated = false)

        testSubject.doWork()

        verify(exactly = 0) { testResultProvider.latestTestResult = null }
    }

    @Test
    fun `verify isolation configuration is updated`() = runBlocking {
        testSubject.doWork()

        verify { isolationConfigurationProvider.durationDays = DurationDays() }
    }

    private fun getIndexCase(isOutdated: Boolean): Isolation =
        Isolation(
            isolationStart = Instant.now(fixedClock),
            expiryDate = LocalDate.now(fixedClock).minusDays(if (isOutdated) 15 else 7),
            indexCase = IndexCase(
                symptomsOnsetDate = LocalDate.now(fixedClock).minusDays(17)
            )
        )

    private fun getLatestTestResult(isOutdated: Boolean): LatestTestResult =
        LatestTestResult(
            diagnosisKeySubmissionToken = "token",
            testEndDate = if (isOutdated) Instant.parse("2020-07-12T01:00:00.00Z") else Instant.now(
                fixedClock
            ),
            testResult = POSITIVE
        )
}
