package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
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
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asLogical
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ClearOutdatedDataAndUpdateIsolationConfigurationTest {

    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxUnitFun = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val isolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val isolationConfigurationApi = mockk<IsolationConfigurationApi>()
    private val riskyVenueConfigurationProvider = mockk<RiskyVenueConfigurationProvider>()
    private val riskyVenueConfigurationApi = mockk<RiskyVenueConfigurationApi>()
    @Suppress("DEPRECATION")
    private val exposureNotificationTokensProvider = mockk<ExposureNotificationTokensProvider>(relaxUnitFun = true)
    private val epidemiologyEventProvider = mockk<EpidemiologyEventProvider>(relaxUnitFun = true)
    private val lastVisitedBookTestTypeVenueDateProvider = mockk<LastVisitedBookTestTypeVenueDateProvider>(relaxUnitFun = true)
    private val clearOutdatedKeySharingInfo = mockk<ClearOutdatedKeySharingInfo>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)
    private val isolationHelper = IsolationHelper(fixedClock)

    private val retentionPeriod = DurationDays().pendingTasksRetentionPeriod

    private val testSubject = ClearOutdatedDataAndUpdateIsolationConfiguration(
        isolationStateMachine,
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

    @Test
    fun `clears old test results when never in isolation`() = runBlocking {
        val testEndDate = LocalDate.now(fixedClock)
            .minusDays(retentionPeriod.toLong() + 1)

        setIsolationState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                indexInfo = NegativeTest(
                    AcknowledgedTestResult(
                        testEndDate = testEndDate,
                        acknowledgedDate = testEndDate.plusDays(1),
                        testResult = NEGATIVE,
                        testKitType = LAB_RESULT,
                        requiresConfirmatoryTest = false,
                        confirmedDate = null
                    )
                )
            )
        )

        testSubject()

        val retentionPeriod = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { unacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify { isolationStateMachine.reset() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `keeps recent test results when in never in isolation`() = runBlocking {
        val testEndDate = LocalDate.now(fixedClock)
            .minusDays(retentionPeriod.toLong())

        setIsolationState(
            IsolationState(
                isolationConfiguration = DurationDays(),
                indexInfo = NegativeTest(
                    AcknowledgedTestResult(
                        testEndDate = testEndDate,
                        acknowledgedDate = testEndDate.plusDays(1),
                        testResult = NEGATIVE,
                        testKitType = LAB_RESULT,
                        requiresConfirmatoryTest = false,
                        confirmedDate = null
                    )
                )
            )
        )

        testSubject()

        val retentionPeriod = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { unacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify(exactly = 0) { isolationStateMachine.reset() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `keeps isolation and tests results when isolation is active`() = runBlocking {
        setIsolationState(activeIsolation)

        testSubject()

        verify(exactly = 0) { unacknowledgedTestResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.reset() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `keeps isolation and test results when isolation is expired but not outdated`() = runBlocking {
        setIsolationState(expiredIsolation(isOutdated = false))

        testSubject()

        verify(exactly = 0) { unacknowledgedTestResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.reset() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `clears isolation and test results when isolation is expired and outdated`() = runBlocking {
        setIsolationState(expiredIsolation(isOutdated = true))

        testSubject()

        val retentionPeriod = isolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { unacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify { isolationStateMachine.reset() }
        verify { exposureNotificationTokensProvider.clear() }
    }

    @Test
    fun `clears old epidemiology exposure windows`() =
        runBlocking {
            testSubject()

            verify {
                epidemiologyEventProvider.clearOnAndBefore(
                    LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
                )
            }
            verify { exposureNotificationTokensProvider.clear() }
        }

    @Test
    fun `clears last book test type risky venue when outside of accepted time window`() = runBlocking {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false

        testSubject()

        verify(exactly = 0) { unacknowledgedTestResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.reset() }
        verify { exposureNotificationTokensProvider.clear() }
        verify { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue = null }
    }

    @Test
    fun `keeps last book test type risky venue when within accepted time window`() = runBlocking {
        every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true

        testSubject()

        verify(exactly = 0) { unacknowledgedTestResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.reset() }
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

    private val activeIsolation: IsolationState =
        IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = isolationHelper.selfAssessment()
        )

    private fun expiredIsolation(isOutdated: Boolean): IsolationState {
        val expiryDate = LocalDate.now(fixedClock).minusDays(if (isOutdated) 15 else 7)
        val selfAssessmentDate = expiryDate.minusDays(9)
        return IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = IndexCase(
                isolationTrigger = SelfAssessment(selfAssessmentDate),
                expiryDate = expiryDate
            )
        )
    }

    private fun setIsolationState(isolationState: IsolationState) {
        every { isolationStateMachine.readState() } returns isolationState
        every { isolationStateMachine.readLogicalState() } returns isolationState.asLogical()
    }
}
