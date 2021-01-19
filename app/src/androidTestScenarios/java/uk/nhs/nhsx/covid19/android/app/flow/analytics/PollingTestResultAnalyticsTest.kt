package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.PollingTestResult
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.report.notReported

class PollingTestResultAnalyticsTest : AnalyticsTest() {

    private var selfDiagnosis = SelfDiagnosis(this)
    private var pollingTestResult = PollingTestResult(this)

    // hasTestedPositiveBackgroundTick - Polling
    // >0 if the app is aware that the user has received/entered a positive test
    // this currently happens during an isolation and for the 14 days after isolation
    @Test
    fun receivePositiveTestResultAfterSelfDiagnosis() = notReported {
        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        // Starting state: App running normally, not in isolation
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms and order test on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 10th Jan
        selfDiagnosis.selfDiagnosePositiveAndOrderTest()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertEquals(1, Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        pollingTestResult.receiveAndAcknowledgePositiveTestResult(this::advanceToNextBackgroundTaskExecution)

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Still in isolation, for both self-diagnosis and positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, Metrics::receivedPositiveTestResultViaPolling)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasTestedPositiveBackgroundTick)
        }

        // Dates: 5th-10th Jan -> Analytics packets for: 4th-9th Jan
        assertOnFieldsForDateRange(5..10) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasTestedPositiveBackgroundTick)
        }

        // Dates: 11th-24th Jan -> Analytics packets for: 10th-23rd Jan
        assertOnFieldsForDateRange(11..24) {
            // Isolation is over, but isolation reason still stored for 14 days
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasTestedPositiveBackgroundTick)
        }

        // Current date: 25th Jan -> Analytics packet for: 24th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun receiveNegativeTestResultAfterSelfDiagnosisAndEndIsolation() = notReported {
        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        // Starting state: App running normally, not in isolation
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms and order test on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 10th Jan
        selfDiagnosis.selfDiagnosePositiveAndOrderTest()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertEquals(1, Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        // Receive negative test result via polling
        pollingTestResult.receiveAndAcknowledgeNegativeTestResult(this::advanceToNextBackgroundTaskExecution)

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Isolation ends part way through analytics window due to negative test result
            assertEquals(1, Metrics::receivedNegativeTestResult)
            assertEquals(1, Metrics::receivedNegativeTestResultViaPolling)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertLessThanTotalBackgroundTasks(Metrics::isIsolatingBackgroundTick)
            assertLessThanTotalBackgroundTasks(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        // Original reason for isolation stored until 17th
        // Dates: 5th-17th Jan -> Analytics packets for: 4th-16th Jan
        assertOnFieldsForDateRange(5..17) {
            // Isolation is over, but isolation reason still stored for 14 days
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Current date: 18th Jan -> Analytics packet for: 17th Jan
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun receiveVoidTestResultAfterSelfDiagnosis() = notReported {
        selfDiagnosis.selfDiagnosePositiveAndOrderTest()

        assertOnFields {
            assertEquals(1, Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        pollingTestResult.receiveAndAcknowledgeVoidTestResult(this::advanceToNextBackgroundTaskExecution)

        assertOnFields {
            assertEquals(1, Metrics::receivedVoidTestResult)
            assertEquals(1, Metrics::receivedVoidTestResultViaPolling)
            ignore(
                Metrics::hasSelfDiagnosedBackgroundTick,
                Metrics::isIsolatingBackgroundTick,
                Metrics::isIsolatingForSelfDiagnosedBackgroundTick
            )
        }
    }
}
