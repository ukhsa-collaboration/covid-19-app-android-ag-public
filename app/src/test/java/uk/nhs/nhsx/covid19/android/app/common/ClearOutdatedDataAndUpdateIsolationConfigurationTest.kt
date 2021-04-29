package uk.nhs.nhsx.covid19.android.app.common

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureNotificationTokensProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenueConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenueConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationConfigurationResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantTestResultProvider
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class ClearOutdatedDataAndUpdateIsolationConfigurationTest {

    private val relevantTestResultProvider = mockk<RelevantTestResultProvider>(relaxed = true)
    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxUnitFun = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val isolationConfigurationApi = mockk<IsolationConfigurationApi>()
    private val riskyVenueConfigurationProvider = mockk<RiskyVenueConfigurationProvider>()
    private val riskyVenueConfigurationApi = mockk<RiskyVenueConfigurationApi>()
    private val exposureNotificationTokensProvider = mockk<ExposureNotificationTokensProvider>(relaxUnitFun = true)
    private val epidemiologyEventProvider = mockk<EpidemiologyEventProvider>(relaxUnitFun = true)
    private val lastVisitedBookTestTypeVenueDateProvider = mockk<LastVisitedBookTestTypeVenueDateProvider>(relaxUnitFun = true)
    private val clearOutdatedKeySharingInfo = mockk<ClearOutdatedKeySharingInfo>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)

    private val retentionPeriod = 14

    private val testSubject = ClearOutdatedDataAndUpdateIsolationConfiguration(
        isolationStateMachine,
        relevantTestResultProvider,
        unacknowledgedTestResultsProvider,
        isolationConfigurationProvider,
        isolationConfigurationApi,
        riskyVenueConfigurationProvider,
        riskyVenueConfigurationApi,
        exposureNotificationTokensProvider,
        epidemiologyEventProvider,
        lastVisitedBookTestTypeVenueDateProvider,
        clearOutdatedKeySharingInfo,
        fixedClock
    )

    @Before
    fun setUp() {
        every { isolationConfigurationProvider.durationDays } returns DurationDays(
            pendingTasksRetentionPeriod = retentionPeriod
        )
        coEvery { isolationConfigurationApi.getIsolationConfiguration() } returns IsolationConfigurationResponse(
            DurationDays()
        )
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `clears old test results when in default state without previous isolation`() = runBlocking {
        every { isolationStateMachine.readState() } returns Default()
        every { relevantTestResultProvider.testResult } returns AcknowledgedTestResult(
            diagnosisKeySubmissionToken = "token1",
            testEndDate = Instant.now(fixedClock)
                .minus(retentionPeriod.toLong(), ChronoUnit.DAYS)
                .minus(1, ChronoUnit.SECONDS),
            acknowledgedDate = Instant.now(fixedClock)
                .minus(13, ChronoUnit.DAYS),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = false,
            confirmedDate = null
        )

        testSubject()

        val retentionPeriod = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { relevantTestResultProvider.clear() }
        verify { unacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify(exactly = 0) { isolationStateMachine.clearPreviousIsolation() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `does not clear recent test results when in default state without previous isolation`() = runBlocking {
        every { isolationStateMachine.readState() } returns Default()
        every { relevantTestResultProvider.testResult } returns AcknowledgedTestResult(
            diagnosisKeySubmissionToken = "token1",
            testEndDate = Instant.now(fixedClock).minus(retentionPeriod.toLong(), ChronoUnit.DAYS),
            acknowledgedDate = Instant.now(fixedClock).minus(9, ChronoUnit.DAYS),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            requiresConfirmatoryTest = false,
            confirmedDate = null
        )

        testSubject()

        val retentionPeriod = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify(exactly = 0) { relevantTestResultProvider.clear() }
        verify { unacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify(exactly = 0) { isolationStateMachine.clearPreviousIsolation() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `keeps test result when state expiration date is outdated`() = runBlocking {
        every { isolationStateMachine.readState() } returns getIndexCase(isOutdated = true)

        testSubject()

        verify(exactly = 0) { relevantTestResultProvider.clear() }
        verify(exactly = 0) { unacknowledgedTestResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.clearPreviousIsolation() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `keeps test result when previous state expiration date in default state is not outdated`() = runBlocking {
        every { isolationStateMachine.readState() } returns getDefaultState(false)

        testSubject()

        verify(exactly = 0) { relevantTestResultProvider.clear() }
        verify(exactly = 0) { unacknowledgedTestResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.clearPreviousIsolation() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `clears test result and previous isolation when in default state and previous isolation state is outdated`() =
        runBlocking {
            val state = getDefaultState(true)
            every { isolationStateMachine.readState() } returns state

            testSubject()

            val retentionPeriod = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
            val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
            verify { relevantTestResultProvider.clear() }
            verify { unacknowledgedTestResultsProvider.clearBefore(expectedDate) }
            verify { isolationStateMachine.clearPreviousIsolation() }
            verify { exposureNotificationTokensProvider.clear() }
        }

    @Test
    fun `clears old epidemiology exposure windows when feature flag enabled`() =
        runBlocking {
            FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.STORE_EXPOSURE_WINDOWS)

            testSubject()

            verify {
                epidemiologyEventProvider.clearOnAndBefore(
                    LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
                )
            }
            verify { exposureNotificationTokensProvider.clear() }
        }

    @Test
    fun `keeps epidemiology exposure windows when feature flag disabled`() =
        runBlocking {
            FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.STORE_EXPOSURE_WINDOWS)

            testSubject()

            verify(exactly = 0) { epidemiologyEventProvider.clearOnAndBefore(any()) }
            verify { exposureNotificationTokensProvider.clear() }
        }

    @Test
    fun `clears last book test type risky venue when outside of accepted time window`() = runBlocking {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false

        testSubject()

        verify(exactly = 0) { relevantTestResultProvider.clear() }
        verify(exactly = 0) { unacknowledgedTestResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.clearPreviousIsolation() }
        verify { exposureNotificationTokensProvider.clear() }
        verify { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = null }
    }

    @Test
    fun `keeps last book test type risky venue when within accepted time window`() = runBlocking {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true

        testSubject()

        verify(exactly = 0) { relevantTestResultProvider.clear() }
        verify(exactly = 0) { unacknowledgedTestResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.clearPreviousIsolation() }
        verify { exposureNotificationTokensProvider.clear() }
        verify(exactly = 0) { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = any() }
    }

    @Test
    fun `verify isolation configuration is updated`() = runBlocking {
        testSubject()

        verify { isolationConfigurationProvider.durationDays = DurationDays() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `calls ClearKeySharing info`() = runBlocking {
        testSubject()

        verify { clearOutdatedKeySharingInfo.invoke() }
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
