package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Ignore
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.PollingTestResult
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED

@Ignore // see COV-17660 for explanation
class PollingTestResultAnalyticsTest : AnalyticsTest() {

    private var selfDiagnosis = SelfDiagnosis(this)
    private var pollingTestResult = PollingTestResult(testAppContext)

    // hasTestedPositiveBackgroundTick - Polling
    // >0 if the app is aware that the user has received/entered a positive PCR test
    // this currently happens during an isolation
    @Test
    fun receivePositivePCRTestResultAfterSelfDiagnosis() {
        receivePositiveTestResultAfterSelfDiagnosis(
            LAB_RESULT,
            Metrics::receivedPositiveTestResultViaPolling,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick
        )
    }

    @Test
    fun receivePositiveAssistedLFDTestResultAfterSelfDiagnosis() {
        receivePositiveTestResultAfterSelfDiagnosis(
            RAPID_RESULT,
            receivedPositiveTestResultViaPollingMetric = null,
            Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
            Metrics::hasTestedLFDPositiveBackgroundTick
        )
    }

    @Test
    fun receivePositiveUnassistedLFDTestResultAfterSelfDiagnosis() {
        receivePositiveTestResultAfterSelfDiagnosis(
            RAPID_SELF_REPORTED,
            receivedPositiveTestResultViaPollingMetric = null,
            Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick,
            Metrics::hasTestedSelfRapidPositiveBackgroundTick
        )
    }

    // No consent to key sharing
    // Will not have consentedToShareExposureKeysInTheInitialFlow
    @Test
    fun receivePositivePCRTestResultAfterSelfDiagnosisWithNoConsentToKeySharing() {
        receivePositiveTestResultAfterSelfDiagnosis(
            LAB_RESULT,
            Metrics::receivedPositiveTestResultViaPolling,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick,
            shouldConsentToKeySharing = false
        )
    }

    // Key sharing fails
    // Will not have successfullySharedExposureKeys, but on the next day totalShareExposureKeyReminderNotifications
    @Test
    fun receivePositivePCRTestResultAfterSelfDiagnosisWithKeySharingFailure() {
        receivePositiveTestResultAfterSelfDiagnosis(
            LAB_RESULT,
            Metrics::receivedPositiveTestResultViaPolling,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick,
            keySharingSucceeds = false
        )
    }

    private fun receivePositiveTestResultAfterSelfDiagnosis(
        testKitType: VirologyTestKitType,
        receivedPositiveTestResultViaPollingMetric: MetricsProperty?,
        isIsolatingForTestedPositiveBackgroundTickMetric: MetricsProperty,
        hasTestedPositiveBackgroundTickMetric: MetricsProperty,
        shouldConsentToKeySharing: Boolean = true,
        keySharingSucceeds: Boolean = true
    ) {
        startTestActivity<MainActivity>()

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        // Starting state: App running normally, not in isolation
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms and order test on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 10th Jan
        selfDiagnosis.selfDiagnosePositiveAndOrderTest(receiveResultImmediately = false)

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertNull(Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertEquals(1, Metrics::launchedTestOrdering)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        if (shouldConsentToKeySharing) {
            pollingTestResult.receiveAndAcknowledgePositiveTestResult(
                testKitType,
                this::advanceToNextBackgroundTaskExecution,
                keySharingSucceeds = keySharingSucceeds
            )
        } else {
            pollingTestResult.receiveAndAcknowledgePositiveTestResultAndDeclineKeySharing(
                testKitType,
                this::advanceToNextBackgroundTaskExecution
            )
        }

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Still in isolation, for both self-diagnosis and positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            receivedPositiveTestResultViaPollingMetric?.let {
                assertEquals(1, it)
            }
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(hasTestedPositiveBackgroundTickMetric)
            assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
            if (shouldConsentToKeySharing) {
                assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
                if (keySharingSucceeds) {
                    assertEquals(1, Metrics::successfullySharedExposureKeys)
                }
            }
        }

        // Current date: 5th Jan -> Analytics packet for: 4rd Jan
        assertOnFields {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(hasTestedPositiveBackgroundTickMetric)
            if (!shouldConsentToKeySharing || !keySharingSucceeds) {
                assertEquals(1, Metrics::totalShareExposureKeysReminderNotifications)
            }
        }

        // Dates: 6th-11th Jan -> Analytics packets for: 5th-10th Jan
        assertOnFieldsForDateRange(6..11) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(hasTestedPositiveBackgroundTickMetric)
        }

        // Dates: 12th-25th Jan -> Analytics packets for: 11th-24rd Jan
        assertOnFieldsForDateRange(12..25) {
            // Isolation is over, and analytics are not kept
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(hasTestedPositiveBackgroundTickMetric)
        }

        // Current date: 25th Jan -> Analytics packet for: 24th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun receiveNegativePCRTestResultAfterSelfDiagnosisAndEndIsolation() {
        receiveNegativeTestResultAfterSelfDiagnosisAndEndIsolation(
            LAB_RESULT,
            Metrics::receivedNegativeTestResultViaPolling
        )
    }

    @Test
    fun receiveNegativeAssistedLFDTestResultAfterSelfDiagnosisAndEndIsolation() {
        receiveNegativeTestResultAfterSelfDiagnosisAndEndIsolation(
            RAPID_RESULT,
            receivedNegativeTestResultViaPollingMetric = null
        )
    }

    @Test
    fun receiveNegativeUnassistedLFDTestResultAfterSelfDiagnosisAndEndIsolation() {
        receiveNegativeTestResultAfterSelfDiagnosisAndEndIsolation(
            RAPID_SELF_REPORTED,
            receivedNegativeTestResultViaPollingMetric = null
        )
    }

    private fun receiveNegativeTestResultAfterSelfDiagnosisAndEndIsolation(
        testKitType: VirologyTestKitType,
        receivedNegativeTestResultViaPollingMetric: MetricsProperty?
    ) {
        startTestActivity<MainActivity>()

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        // Starting state: App running normally, not in isolation
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms and order test on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 10th Jan
        selfDiagnosis.selfDiagnosePositiveAndOrderTest(receiveResultImmediately = false)

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertNull(Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertEquals(1, Metrics::launchedTestOrdering)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        // Receive negative test result via polling
        pollingTestResult.receiveAndAcknowledgeNegativeTestResult(
            testKitType,
            this::advanceToNextBackgroundTaskExecution
        )

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Isolation ends part way through analytics window due to negative test result
            assertEquals(1, Metrics::receivedNegativeTestResult)
            receivedNegativeTestResultViaPollingMetric?.let {
                assertEquals(1, it)
            }
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertLessThanTotalBackgroundTasks(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }
        // Dates: 5th-17th Jan -> Analytics packets for: 4th-16th Jan
        assertOnFieldsForDateRange(5..17) {
            // Isolation is over, and analytics are not kept
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Current date: 18th Jan -> Analytics packet for: 17th Jan
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun receiveVoidPCRTestResultAfterSelfDiagnosis() {
        receiveVoidTestResultAfterSelfDiagnosis(
            LAB_RESULT,
            Metrics::receivedVoidTestResultViaPolling
        )
    }

    @Test
    fun receiveVoidAssistedLFDTestResultAfterSelfDiagnosis() {
        receiveVoidTestResultAfterSelfDiagnosis(
            RAPID_RESULT,
            receivedVoidTestResultViaPollingMetric = null
        )
    }

    @Test
    fun receiveVoidUnassistedLFDTestResultAfterSelfDiagnosis() {
        receiveVoidTestResultAfterSelfDiagnosis(
            RAPID_SELF_REPORTED,
            receivedVoidTestResultViaPollingMetric = null
        )
    }

    private fun receiveVoidTestResultAfterSelfDiagnosis(
        testKitType: VirologyTestKitType,
        receivedVoidTestResultViaPollingMetric: MetricsProperty?
    ) {
        startTestActivity<MainActivity>()

        selfDiagnosis.selfDiagnosePositiveAndOrderTest(receiveResultImmediately = false)

        assertOnFields {
            assertNull(Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertEquals(1, Metrics::launchedTestOrdering)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        pollingTestResult.receiveAndAcknowledgeVoidTestResult(
            testKitType,
            this::advanceToNextBackgroundTaskExecution
        )

        assertOnFields {
            assertEquals(1, Metrics::receivedVoidTestResult)
            receivedVoidTestResultViaPollingMetric?.let {
                assertEquals(1, it)
            }
            ignore(
                Metrics::hasSelfDiagnosedBackgroundTick,
                Metrics::isIsolatingBackgroundTick,
                Metrics::isIsolatingForSelfDiagnosedBackgroundTick
            )
        }
    }
}
