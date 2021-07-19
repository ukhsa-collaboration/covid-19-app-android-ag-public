package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveLFDOutsideTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveLFDWithinTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveLabResultAfterPositiveLFD
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveLabResultAfterPositiveSelfRapidTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS

class TrackTestResultAnalyticsOnAcknowledgeTest {

    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2021-05-01T10:00:00Z"), ZoneOffset.UTC)
    private val isolationHelper = IsolationHelper(fixedClock)

    private val trackTestResultAnalyticsOnAcknowledge =
        TrackTestResultAnalyticsOnAcknowledge(analyticsEventProcessor, fixedClock)

    //region early return conditions

    @Test
    fun `do not track anything if currently not isolating`() = runBlocking {
        whenTrackingTestResultsAnalyticsOnAcknowledgement(
            currentState = isolationHelper.neverInIsolation().asLogical(),
            receivedTestResult = receivedTestResult(
                Instant.now(fixedClock),
                LAB_RESULT,
                POSITIVE
            )
        )

        thenNothingIsTracked()
    }

    @Test
    fun `do not track anything if isolating with self assessment without test`() = runBlocking {
        whenTrackingTestResultsAnalyticsOnAcknowledgement(
            currentState = IsolationState(
                isolationConfiguration = DurationDays(),
                indexInfo = isolationHelper.selfAssessment(
                    expired = false,
                    onsetDate = null,
                    testResult = null
                )
            ).asLogical(),
            receivedTestResult = receivedTestResult(
                Instant.now(fixedClock),
                LAB_RESULT,
                POSITIVE
            )
        )

        thenNothingIsTracked()
    }

    @Test
    fun `do not track anything if new test isolation would end before current isolation started`() = runBlocking {
        whenTrackingTestResultsAnalyticsOnAcknowledgement(
            currentState = positiveAcknowledgedTestResult(RAPID_RESULT, withinConfirmatoryDayLimit = true)
                .asIsolation(),
            receivedTestResult = receivedTestResult(
                testEndDate = Instant.now(fixedClock).minus(20, DAYS),
                testKitType = LAB_RESULT,
                testResult = POSITIVE
            )
        )

        thenNothingIsTracked()
    }

    @Test
    fun `do not track anything if receiving a positive indicative after symptoms`() = runBlocking {
        whenTrackingTestResultsAnalyticsOnAcknowledgement(
            currentState = IsolationState(
                isolationConfiguration = DurationDays(),
                indexInfo = isolationHelper.selfAssessment(
                    expired = false,
                    onsetDate = LocalDate.now(fixedClock).minusDays(3),
                    testResult = null
                )
            ).asLogical(),
            receivedTestResult = receivedTestResult(
                testEndDate = Instant.now(fixedClock),
                testKitType = RAPID_RESULT,
                testResult = POSITIVE
            )
        )

        thenNothingIsTracked()
    }

    @Test
    fun `do not track anything if stored test is negative`() = runBlocking {
        whenTrackingTestResultsAnalyticsOnAcknowledgement(
            currentState = acknowledgedTestResult(
                testEndDate = LocalDate.now(fixedClock).minusDays(1),
                testKitType = RAPID_RESULT,
                testResult = RelevantVirologyTestResult.NEGATIVE,
            ).asIsolation(),
            receivedTestResult = receivedTestResult(
                Instant.now(fixedClock),
                LAB_RESULT,
                POSITIVE
            )
        )

        thenNothingIsTracked()
    }

    @Test
    fun `do not track anything if stored test is LAB_RESULT`() = runBlocking {
        whenTrackingTestResultsAnalyticsOnAcknowledgement(
            currentState = acknowledgedTestResult(
                testEndDate = LocalDate.now(fixedClock).minusDays(1),
                testKitType = LAB_RESULT,
            ).asIsolation(),
            receivedTestResult = receivedTestResult(
                Instant.now(fixedClock),
                LAB_RESULT,
                POSITIVE
            )
        )

        thenNothingIsTracked()
    }

    @Test
    fun `do not track anything if received test result is RAPID_RESULT`() = runBlocking {
        whenTrackingTestResultsAnalyticsOnAcknowledgement(
            currentState = positiveAcknowledgedTestResult(RAPID_RESULT, withinConfirmatoryDayLimit = true)
                .asIsolation(),
            receivedTestResult = receivedTestResult(
                Instant.now(fixedClock),
                RAPID_RESULT,
                NEGATIVE
            )
        )

        thenNothingIsTracked()
    }

    private fun thenNothingIsTracked() {
        verify(exactly = 0) { analyticsEventProcessor.track(any()) }
    }

    //endregion

    //region PositiveLabResultAfterPositiveLFD

    @Test
    fun `receive positive lab result within day limit after LFD tracks PositiveLabResultAfterPositiveLFD`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = positiveAcknowledgedTestResult(RAPID_RESULT, withinConfirmatoryDayLimit = true)
                    .asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock),
                    LAB_RESULT,
                    POSITIVE
                )
            )

            thenTrackPositiveLabResultAfterPositiveLFD()
        }

    @Test
    fun `out of order receive positive lab result within day limit after LFD tracks PositiveLabResultAfterPositiveLFD`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock),
                    LAB_RESULT,
                    RelevantVirologyTestResult.POSITIVE
                ).asIsolation(),
                receivedTestResult = positiveReceivedTestResult(RAPID_RESULT, withinConfirmatoryDayLimit = true)
            )

            thenTrackPositiveLabResultAfterPositiveLFD()
        }

    @Test
    fun `receive positive lab result outside day limit after LFD tracks PositiveLabResultAfterPositiveLFD`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = positiveAcknowledgedTestResult(RAPID_RESULT, withinConfirmatoryDayLimit = false)
                    .asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock),
                    LAB_RESULT,
                    POSITIVE
                )
            )

            thenTrackPositiveLabResultAfterPositiveLFD()
        }

    @Test
    fun `out of order receive positive lab result outside day limit after LFD tracks PositiveLabResultAfterPositiveLFD`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock),
                    LAB_RESULT,
                    RelevantVirologyTestResult.POSITIVE
                ).asIsolation(),
                receivedTestResult = positiveReceivedTestResult(RAPID_RESULT, withinConfirmatoryDayLimit = false)
            )

            thenTrackPositiveLabResultAfterPositiveLFD()
        }

    private fun thenTrackPositiveLabResultAfterPositiveLFD() {
        verify { analyticsEventProcessor.track(PositiveLabResultAfterPositiveLFD) }
    }

    //endregion

    //region PositiveLabResultAfterPositiveSelfRapidTest

    @Test
    fun `receive positive lab result within day limit after self rapid tracks PositiveLabResultAfterPositiveSelfRapidTest`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = positiveAcknowledgedTestResult(
                    RAPID_SELF_REPORTED,
                    withinConfirmatoryDayLimit = true
                ).asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock),
                    LAB_RESULT,
                    POSITIVE
                )
            )

            thenTrackPositiveLabResultAfterPositiveSelfRapidTest()
        }

    @Test
    fun `out of order receive positive lab result within day limit after self rapid tracks PositiveLabResultAfterPositiveSelfRapidTest`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock),
                    LAB_RESULT,
                    RelevantVirologyTestResult.POSITIVE
                ).asIsolation(),
                receivedTestResult = positiveReceivedTestResult(
                    RAPID_SELF_REPORTED,
                    withinConfirmatoryDayLimit = true
                )
            )

            thenTrackPositiveLabResultAfterPositiveSelfRapidTest()
        }

    @Test
    fun `receive positive lab result outside day limit after self rapid tracks PositiveLabResultAfterPositiveSelfRapidTest`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = positiveAcknowledgedTestResult(
                    RAPID_SELF_REPORTED,
                    withinConfirmatoryDayLimit = false
                ).asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock),
                    LAB_RESULT,
                    POSITIVE
                )
            )

            thenTrackPositiveLabResultAfterPositiveSelfRapidTest()
        }

    @Test
    fun `out of order receive positive lab result outside day limit after self rapid tracks PositiveLabResultAfterPositiveSelfRapidTest`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock),
                    LAB_RESULT,
                    RelevantVirologyTestResult.POSITIVE
                ).asIsolation(),
                receivedTestResult = positiveReceivedTestResult(
                    RAPID_SELF_REPORTED,
                    withinConfirmatoryDayLimit = false
                )
            )

            thenTrackPositiveLabResultAfterPositiveSelfRapidTest()
        }

    private fun thenTrackPositiveLabResultAfterPositiveSelfRapidTest() {
        verify { analyticsEventProcessor.track(PositiveLabResultAfterPositiveSelfRapidTest) }
    }

    //endregion

    //region NegativeLabResultAfterPositiveLFDWithinTimeLimit

    @Test
    fun `receive negative lab result within day limit after self rapid tracks NegativeLabResultAfterPositiveLFDWithinTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = positiveAcknowledgedTestResult(RAPID_RESULT, withinConfirmatoryDayLimit = true)
                    .asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock),
                    LAB_RESULT,
                    NEGATIVE
                )
            )

            thenTrackNegativeLabResultAfterPositiveLFDWithinTimeLimit()
        }

    @Test
    fun `out of order receive negative lab result within day limit after self rapid tracks NegativeLabResultAfterPositiveLFDWithinTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock),
                    LAB_RESULT,
                    RelevantVirologyTestResult.NEGATIVE
                ).asIsolation(),
                receivedTestResult = positiveReceivedTestResult(RAPID_RESULT, withinConfirmatoryDayLimit = true)
            )

            thenTrackNegativeLabResultAfterPositiveLFDWithinTimeLimit()
        }

    private fun thenTrackNegativeLabResultAfterPositiveLFDWithinTimeLimit() {
        verify { analyticsEventProcessor.track(NegativeLabResultAfterPositiveLFDWithinTimeLimit) }
    }

    //endregion

    //region NegativeLabResultAfterPositiveLFDOutsideTimeLimit

    @Test
    fun `receive negative lab result when there is negative day limit after self rapid tracks NegativeLabResultAfterPositiveLFDOutsideTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock).minusDays(3L),
                    RAPID_RESULT,
                    testResult = RelevantVirologyTestResult.POSITIVE,
                    confirmatoryDayLimit = -1
                ).asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock),
                    LAB_RESULT,
                    NEGATIVE
                )
            )

            thenTrackNegativeLabResultAfterPositiveLFDOutsideTimeLimit()
        }

    @Test
    fun `out of order receive negative lab result when there is negative day limit after self rapid tracks NegativeLabResultAfterPositiveLFDOutsideTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock),
                    LAB_RESULT,
                    RelevantVirologyTestResult.NEGATIVE
                ).asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock).minus(3, DAYS),
                    RAPID_RESULT,
                    testResult = POSITIVE,
                    confirmatoryDayLimit = -1
                )
            )

            thenTrackNegativeLabResultAfterPositiveLFDOutsideTimeLimit()
        }

    @Test
    fun `receive negative lab result outside day limit after self rapid tracks NegativeLabResultAfterPositiveLFDOutsideTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = positiveAcknowledgedTestResult(RAPID_RESULT, withinConfirmatoryDayLimit = false)
                    .asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock),
                    LAB_RESULT,
                    NEGATIVE
                )
            )

            thenTrackNegativeLabResultAfterPositiveLFDOutsideTimeLimit()
        }

    @Test
    fun `out of order receive negative lab result outside day limit after self rapid tracks NegativeLabResultAfterPositiveLFDOutsideTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock),
                    LAB_RESULT,
                    RelevantVirologyTestResult.NEGATIVE
                ).asIsolation(),
                receivedTestResult = positiveReceivedTestResult(RAPID_RESULT, withinConfirmatoryDayLimit = false)
            )

            thenTrackNegativeLabResultAfterPositiveLFDOutsideTimeLimit()
        }

    private fun thenTrackNegativeLabResultAfterPositiveLFDOutsideTimeLimit() {
        verify { analyticsEventProcessor.track(NegativeLabResultAfterPositiveLFDOutsideTimeLimit) }
    }

    //endregion

    //region NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit

    @Test
    fun `receive negative lab result within day limit after self rapid tracks NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = positiveAcknowledgedTestResult(
                    RAPID_SELF_REPORTED,
                    withinConfirmatoryDayLimit = true
                ).asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock),
                    LAB_RESULT,
                    NEGATIVE
                )
            )

            thenTrackNegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit()
        }

    @Test
    fun `out of order receive negative lab result within day limit after self rapid tracks NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock),
                    LAB_RESULT,
                    RelevantVirologyTestResult.NEGATIVE
                ).asIsolation(),
                receivedTestResult = positiveReceivedTestResult(
                    RAPID_SELF_REPORTED,
                    withinConfirmatoryDayLimit = true
                )
            )

            thenTrackNegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit()
        }

    private fun thenTrackNegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit() {
        verify { analyticsEventProcessor.track(NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit) }
    }

    //endregion

    //region NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit

    @Test
    fun `receive negative lab result when there is negative day limit after self rapid tracks NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock).minusDays(3L),
                    RAPID_SELF_REPORTED,
                    testResult = RelevantVirologyTestResult.POSITIVE,
                    confirmatoryDayLimit = -1
                ).asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock),
                    LAB_RESULT,
                    NEGATIVE
                )
            )

            thenTrackNegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit()
        }

    @Test
    fun `out of order receive negative lab result when there is negative day limit after self rapid tracks NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock),
                    LAB_RESULT,
                    RelevantVirologyTestResult.NEGATIVE
                ).asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock).minus(3, DAYS),
                    RAPID_SELF_REPORTED,
                    testResult = POSITIVE,
                    confirmatoryDayLimit = -1
                )
            )

            thenTrackNegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit()
        }

    @Test
    fun `receive negative lab result outside day limit after self rapid tracks NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = positiveAcknowledgedTestResult(
                    RAPID_SELF_REPORTED,
                    withinConfirmatoryDayLimit = false
                ).asIsolation(),
                receivedTestResult = receivedTestResult(
                    Instant.now(fixedClock),
                    LAB_RESULT,
                    NEGATIVE
                )
            )

            thenTrackNegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit()
        }

    @Test
    fun `out of order receive negative lab result outside day limit after self rapid tracks NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit`() =
        runBlocking {
            whenTrackingTestResultsAnalyticsOnAcknowledgement(
                currentState = acknowledgedTestResult(
                    LocalDate.now(fixedClock),
                    LAB_RESULT,
                    RelevantVirologyTestResult.NEGATIVE
                ).asIsolation(),
                receivedTestResult = positiveReceivedTestResult(
                    RAPID_SELF_REPORTED,
                    withinConfirmatoryDayLimit = false
                )
            )

            thenTrackNegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit()
        }

    private fun thenTrackNegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit() {
        verify { analyticsEventProcessor.track(NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit) }
    }

    //endregion

    //region helpers

    private fun whenTrackingTestResultsAnalyticsOnAcknowledgement(
        currentState: IsolationLogicalState,
        receivedTestResult: ReceivedTestResult
    ) {
        trackTestResultAnalyticsOnAcknowledge(currentState, receivedTestResult)
    }

    private fun AcknowledgedTestResult.asIsolation(): IsolationLogicalState {
        val indexInfo =
            if (isPositive()) isolationHelper.positiveTest(this)
            else isolationHelper.negativeTest(this)

        return IsolationState(
            isolationConfiguration = DurationDays(),
            indexInfo = indexInfo
        ).asLogical()
    }

    private fun positiveAcknowledgedTestResult(
        testKitType: VirologyTestKitType,
        withinConfirmatoryDayLimit: Boolean
    ): AcknowledgedTestResult {
        val confirmatoryDayLimit = 2
        val testEndDate = LocalDate.now(fixedClock)
            .minusDays(
                if (withinConfirmatoryDayLimit) confirmatoryDayLimit.toLong()
                else (confirmatoryDayLimit + 1L)
            )
        return acknowledgedTestResult(
            testEndDate,
            testKitType,
            RelevantVirologyTestResult.POSITIVE,
            confirmatoryDayLimit
        )
    }

    private fun positiveReceivedTestResult(
        testKitType: VirologyTestKitType,
        withinConfirmatoryDayLimit: Boolean
    ): ReceivedTestResult {
        val confirmatoryDayLimit = 2
        val testEndDate = Instant.now(fixedClock)
            .minus(
                if (withinConfirmatoryDayLimit) confirmatoryDayLimit.toLong()
                else (confirmatoryDayLimit + 1L),
                DAYS
            )
        return receivedTestResult(
            testEndDate,
            testKitType,
            POSITIVE,
            confirmatoryDayLimit
        )
    }

    private fun acknowledgedTestResult(
        testEndDate: LocalDate,
        testKitType: VirologyTestKitType,
        testResult: RelevantVirologyTestResult = RelevantVirologyTestResult.POSITIVE,
        confirmatoryDayLimit: Int? = null
    ) = AcknowledgedTestResult(
        testEndDate = testEndDate,
        testResult = testResult,
        testKitType = testKitType,
        acknowledgedDate = LocalDate.now(fixedClock).minusDays(1),
        requiresConfirmatoryTest = testKitType != LAB_RESULT,
        confirmatoryDayLimit = confirmatoryDayLimit,
    )

    private fun receivedTestResult(
        testEndDate: Instant,
        testKitType: VirologyTestKitType,
        testResult: VirologyTestResult,
        confirmatoryDayLimit: Int? = null,
    ) = ReceivedTestResult(
        testEndDate = testEndDate,
        testResult = testResult,
        testKitType = testKitType,
        requiresConfirmatoryTest = testKitType != LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        diagnosisKeySubmissionToken = "",
        confirmatoryDayLimit = confirmatoryDayLimit
    )

    //endregion
}
