package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveLFDOutsideTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveLFDWithinTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveLabResultAfterPositiveLFD
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveLabResultAfterPositiveSelfRapidTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResult
import java.time.Clock
import javax.inject.Inject

class TrackTestResultAnalyticsOnAcknowledge @Inject constructor(
    private val analyticsEventProcessor: AnalyticsEventProcessor,
    private val wouldTestIsolationEndBeforeOrOnStartOfExistingIsolation: WouldTestIsolationEndBeforeOrOnStartOfExistingIsolation,
    private val clock: Clock,
) {

    operator fun invoke(
        currentState: IsolationLogicalState,
        receivedTestResult: ReceivedTestResult,
    ) {
        val existingTestResult = currentState.getTestResult() ?: return

        // Abort if we are not in an active index case (unless we would have been isolating if the results were in the right order)
        if (!currentState.isActiveIndexCase(clock) &&
            !isReceivedPositiveIndicativeResultOlderThanExistingNegativeResult(existingTestResult, receivedTestResult)
        ) return

        // Abort for received test results that would have been ignored even if they arrived in the right order
        if (wouldTestIsolationEndBeforeOrOnStartOfExistingIsolation(receivedTestResult, currentState) ||
            isReceivedPositiveIndicativeResultNewerThanSymptoms(receivedTestResult, currentState)
        ) return

        val chronologicallySortedResults =
            listOf(receivedTestResult, existingTestResult)
                .sortedBy { it.testEndDate(clock) }
        val oldTest: TestResult = chronologicallySortedResults.firstOrNull() ?: return
        val newTest: TestResult = chronologicallySortedResults.lastOrNull() ?: return

        val oldTestIsNegative = oldTest.isNegative()
        val oldTestIsLabResult = oldTest.testKitType == LAB_RESULT
        val newTestIsNoLabResult = newTest.testKitType != LAB_RESULT
        if (oldTestIsNegative || oldTestIsLabResult || newTestIsNoLabResult) return

        trackAnalytics(oldTest, newTest)
    }

    private fun isReceivedPositiveIndicativeResultOlderThanExistingNegativeResult(
        existingTestResult: AcknowledgedTestResult,
        receivedTestResult: ReceivedTestResult
    ): Boolean =
        existingTestResult.isNegative() &&
            receivedTestResult.isPositive() &&
            receivedTestResult.requiresConfirmatoryTest &&
            receivedTestResult.isOlderThan(existingTestResult, clock)

    private fun isReceivedPositiveIndicativeResultNewerThanSymptoms(
        receivedTestResult: ReceivedTestResult,
        currentState: IsolationLogicalState
    ): Boolean {
        val symptomsOnsetDate = currentState.getIndexCase()?.getSelfAssessmentOnsetDate()
        return receivedTestResult.isPositive() &&
            receivedTestResult.requiresConfirmatoryTest &&
            symptomsOnsetDate != null &&
            receivedTestResult.testEndDate(clock).isAfter(symptomsOnsetDate)
    }

    private fun trackAnalytics(
        oldTest: TestResult,
        newTest: TestResult
    ) {
        val analyticsEvent = when {
            newTest.isPositive() && oldTest.testKitType == RAPID_RESULT ->
                PositiveLabResultAfterPositiveLFD

            newTest.isPositive() && oldTest.testKitType == RAPID_SELF_REPORTED ->
                PositiveLabResultAfterPositiveSelfRapidTest

            newTest.isNegative() && oldTest.testKitType == RAPID_RESULT ->
                if (oldTest.isDateWithinConfirmatoryDayLimit(newTest.testEndDate(clock), clock)) {
                    NegativeLabResultAfterPositiveLFDWithinTimeLimit
                } else {
                    NegativeLabResultAfterPositiveLFDOutsideTimeLimit
                }

            newTest.isNegative() && oldTest.testKitType == RAPID_SELF_REPORTED ->
                if (oldTest.isDateWithinConfirmatoryDayLimit(newTest.testEndDate(clock), clock)) {
                    NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit
                } else {
                    NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit
                }

            else -> null
        }
        if (analyticsEvent != null) {
            analyticsEventProcessor.track(analyticsEvent)
        }
    }
}
