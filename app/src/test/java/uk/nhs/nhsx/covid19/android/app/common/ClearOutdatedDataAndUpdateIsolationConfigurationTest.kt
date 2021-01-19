package uk.nhs.nhsx.covid19.android.app.common

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfoProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationTokensProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider

class ClearOutdatedDataAndUpdateIsolationConfigurationTest {

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val testResultsProvider = mockk<TestResultsProvider>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>(relaxed = true)
    private val isolationConfigurationApi = mockk<IsolationConfigurationApi>(relaxed = true)
    private val exposureNotificationTokensProvider = mockk<ExposureNotificationTokensProvider>(relaxed = true)
    private val exposureCircuitBreakerInfoProvider = mockk<ExposureCircuitBreakerInfoProvider>(relaxed = true)
    private val epidemiologyEventProvider = mockk<EpidemiologyEventProvider>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)

    private val testSubject = ClearOutdatedDataAndUpdateIsolationConfiguration(
        isolationStateMachine,
        testResultsProvider,
        isolationConfigurationProvider,
        isolationConfigurationApi,
        exposureNotificationTokensProvider,
        exposureCircuitBreakerInfoProvider,
        epidemiologyEventProvider,
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

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `clears old test results when in default state without previous isolation`() = runBlocking {
        every { isolationStateMachine.readState() } returns Default()

        testSubject()

        val retentionPeriod = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { testResultsProvider.clearBefore(expectedDate) }
        verify(exactly = 0) { isolationStateMachine.clearPreviousIsolation() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `keeps test result when state expiration date is outdated`() = runBlocking {
        every { isolationStateMachine.readState() } returns getIndexCase(isOutdated = true)

        testSubject()

        verify(exactly = 0) { testResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.clearPreviousIsolation() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `keeps test result when previous state expiration date in default state is not outdated`() = runBlocking {
        every { isolationStateMachine.readState() } returns getDefaultState(false)

        testSubject()

        verify(exactly = 0) { testResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.clearPreviousIsolation() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `clears test result when in default state and previous isolation state is outdated`() = runBlocking {
        val state = getDefaultState(true)
        every { isolationStateMachine.readState() } returns state

        testSubject()

        verify { testResultsProvider.clearBefore(state.previousIsolation!!.expiryDate) }
        verify { isolationStateMachine.clearPreviousIsolation() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `clears old epidemiology exposure windows when in default state and circuit breaker queue is empty with feature flag enabled`() =
        runBlocking {
            FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.STORE_EXPOSURE_WINDOWS)

            every { isolationStateMachine.readState() } returns Default()
            every { exposureCircuitBreakerInfoProvider.info.isEmpty() } returns true

            testSubject()

            verify { epidemiologyEventProvider.clear() }
            verify { exposureNotificationTokensProvider.clear() }
        }

    @Test
    fun `keeps epidemiology exposure windows when in default state and circuit breaker queue filled with feature flag enabled`() =
        runBlocking {
            FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.STORE_EXPOSURE_WINDOWS)

            every { isolationStateMachine.readState() } returns Default()
            every { exposureCircuitBreakerInfoProvider.info.isEmpty() } returns false

            testSubject()

            verify(exactly = 0) { epidemiologyEventProvider.clear() }
            verify { exposureNotificationTokensProvider.clear() }
        }

    @Test
    fun `keeps epidemiology exposure windows when in isolation with feature flag enabled`() = runBlocking {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.STORE_EXPOSURE_WINDOWS)

        every { isolationStateMachine.readState() } returns getIndexCase(isOutdated = false)

        testSubject()

        verify(exactly = 0) { epidemiologyEventProvider.clear() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `keeps epidemiology exposure windows when in default state and circuit breaker queue is empty with feature flag disabled`() =
        runBlocking {
            FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.STORE_EXPOSURE_WINDOWS)

            every { isolationStateMachine.readState() } returns Default()
            every { exposureCircuitBreakerInfoProvider.info.isEmpty() } returns true

            testSubject()

            verify(exactly = 0) { epidemiologyEventProvider.clear() }
            verify { exposureNotificationTokensProvider.clear() }
        }

    @Test
    fun `verify isolation configuration is updated`() = runBlocking {
        testSubject()

        verify { isolationConfigurationProvider.durationDays = DurationDays() }
        verify { exposureNotificationTokensProvider.clear() }
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
