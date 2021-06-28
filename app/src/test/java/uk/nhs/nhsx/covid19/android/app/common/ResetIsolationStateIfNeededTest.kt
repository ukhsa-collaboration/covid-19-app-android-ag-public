package uk.nhs.nhsx.covid19.android.app.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
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

class ResetIsolationStateIfNeededTest {
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T01:00:00.00Z"), ZoneOffset.UTC)
    private val durationDays = DurationDays()
    private val retentionPeriod = durationDays.pendingTasksRetentionPeriod
    private val mockIsolationStateMachine = mockk<IsolationStateMachine>(relaxUnitFun = true)
    private val mockUnacknowledgedTestResultsProvider = mockk<UnacknowledgedTestResultsProvider>(relaxUnitFun = true)
    private val mockIsolationConfigurationProvider = mockk<IsolationConfigurationProvider>()
    private val testSubject = ResetIsolationStateIfNeeded(
        mockIsolationStateMachine, mockUnacknowledgedTestResultsProvider, mockIsolationConfigurationProvider, fixedClock
    )

    @Before
    fun setUp() {
        every { mockIsolationConfigurationProvider.durationDays } returns durationDays
    }

    @Test
    fun `clears old test results when never in isolation`() = runBlocking {
        val testEndDate = LocalDate.now(fixedClock)
            .minusDays(retentionPeriod.toLong() + 1)

        setIsolationState(
            IsolationState(
                isolationConfiguration = durationDays,
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

        val retentionPeriod = mockIsolationConfigurationProvider.durationDays.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { mockUnacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify { mockIsolationStateMachine.reset() }
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

        val retentionPeriod = durationDays.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { mockUnacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify(exactly = 0) { mockIsolationStateMachine.reset() }
    }

    @Test
    fun `clears isolation and test results when isolation is expired and outdated`() = runBlocking {
        setIsolationState(expiredIsolation(isOutdated = true))

        testSubject()

        val retentionPeriod = durationDays.pendingTasksRetentionPeriod
        val expectedDate = LocalDate.now(fixedClock).minusDays(retentionPeriod.toLong())
        verify { mockUnacknowledgedTestResultsProvider.clearBefore(expectedDate) }
        verify { mockIsolationStateMachine.reset() }
    }

    private fun setIsolationState(isolationState: IsolationState) {
        every { mockIsolationStateMachine.readState() } returns isolationState
        every { mockIsolationStateMachine.readLogicalState() } returns isolationState.asLogical()
    }

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
}
