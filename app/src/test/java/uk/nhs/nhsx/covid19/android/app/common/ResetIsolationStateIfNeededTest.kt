package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.testordering.UnacknowledgedTestResultsProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ResetIsolationStateIfNeededTest {
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)
    private val isolationConfiguration = IsolationConfiguration()
    private val configuration = CountrySpecificConfiguration(
        contactCase = 11,
        indexCaseSinceSelfDiagnosisOnset = 11,
        indexCaseSinceSelfDiagnosisUnknownOnset = 9,
        maxIsolation = 21,
        indexCaseSinceTestResultEndDate = 11,
        pendingTasksRetentionPeriod = 14,
        testResultPollingTokenRetentionPeriod = 28
    )
    private val retentionPeriod = isolationConfiguration.pendingTasksRetentionPeriod
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val unacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxUnitFun = true)
    private val getLatestConfiguration = mockk<GetLatestConfiguration>()
    private val isolationLogicalHelper = IsolationLogicalHelper(fixedClock, isolationConfiguration)

    private val testSubject = ResetIsolationStateIfNeeded(
        isolationStateMachine,
        unacknowledgedTestResultsProvider,
        getLatestConfiguration,
        fixedClock
    )

    @Before
    fun setUp() {
        every { getLatestConfiguration() } returns configuration
    }

    @Test
    fun `clears old test results when never in isolation`() = runBlocking {
        val testEndDate = LocalDate.now(fixedClock)
            .minusDays(retentionPeriod.toLong() + 1)

        setIsolationState(
            NeverIsolating(
                isolationConfiguration = isolationConfiguration,
                negativeTest = NegativeTest(
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

        val retentionPeriod = getLatestConfiguration().pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { unacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify { isolationStateMachine.reset() }
    }

    @Test
    fun `keeps recent test results when in never in isolation`() = runBlocking {
        val testEndDate = LocalDate.now(fixedClock)
            .minusDays(retentionPeriod.toLong())

        setIsolationState(
            NeverIsolating(
                isolationConfiguration = isolationConfiguration,
                negativeTest = NegativeTest(
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

        val retentionPeriod = isolationConfiguration.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { unacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify(exactly = 0) { isolationStateMachine.reset() }
    }

    @Test
    fun `keeps isolation and tests results when isolation is active`() = runBlocking {
        setIsolationState(isolationLogicalHelper.selfAssessment().asIsolation())

        testSubject()

        verify(exactly = 0) { unacknowledgedTestResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.reset() }
    }

    @Test
    fun `keeps isolation and test results when isolation is expired but not outdated`() = runBlocking {
        setIsolationState(expiredIsolation(isOutdated = false))

        testSubject()

        verify(exactly = 0) { unacknowledgedTestResultsProvider.clearBefore(any()) }
        verify(exactly = 0) { isolationStateMachine.reset() }
    }

    @Test
    fun `clears isolation and test results when isolation is expired and outdated`() = runBlocking {
        setIsolationState(expiredIsolation(isOutdated = true))

        testSubject()

        val retentionPeriod = isolationConfiguration.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { unacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify { isolationStateMachine.reset() }
    }

    private fun setIsolationState(isolationState: IsolationLogicalState) {
        every { isolationStateMachine.readLogicalState() } returns isolationState
    }

    private fun expiredIsolation(isOutdated: Boolean): IsolationLogicalState {
        val expiryDate = LocalDate.now(fixedClock).minusDays(if (isOutdated) 15 else 7)
        val selfAssessmentDate = expiryDate.minusDays(9)
        return isolationLogicalHelper.selfAssessment(selfAssessmentDate).asIsolation()
    }
}
