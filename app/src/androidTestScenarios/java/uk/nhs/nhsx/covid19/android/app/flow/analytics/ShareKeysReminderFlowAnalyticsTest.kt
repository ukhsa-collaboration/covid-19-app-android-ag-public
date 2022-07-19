package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.flow.analytics.ShareKeysReminderFlowAnalyticsTest.KeySharingReminderTestFlow.CONSENT_AND_SUCCESS
import uk.nhs.nhsx.covid19.android.app.flow.analytics.ShareKeysReminderFlowAnalyticsTest.KeySharingReminderTestFlow.CONSENT_BUT_FAILURE
import uk.nhs.nhsx.covid19.android.app.flow.analytics.ShareKeysReminderFlowAnalyticsTest.KeySharingReminderTestFlow.NO_CONSENT
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ShareKeysReminder
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics

class ShareKeysReminderFlowAnalyticsTest : AnalyticsTest() {

    private var selfDiagnosis = SelfDiagnosis(this)
    private var manuelTestResultEntry = ManualTestResultEntry(testAppContext)
    private var shareKeysReminder = ShareKeysReminder(this.testAppContext)

    private enum class KeySharingReminderTestFlow {
        NO_CONSENT,
        CONSENT_BUT_FAILURE,
        CONSENT_AND_SUCCESS
    }

    @Test
    fun selfDiagnosis_receivePositivePCR_declineKeySharingInInitialFlow_DeclineKeySharingInReminderFlow() {
        receivePositiveTestResultAfterSelfDiagnosisAndDeclineSharingKeysInitially(NO_CONSENT)
    }

    @Test
    fun selfDiagnosis_receivePositivePCR_declineKeySharingInInitialFlow_ConsentToKeySharingInReminderFlow_Failure() {
        receivePositiveTestResultAfterSelfDiagnosisAndDeclineSharingKeysInitially(CONSENT_BUT_FAILURE)
    }

    @Test
    fun selfDiagnosis_receivePositivePCR_declineKeySharingInInitialFlow_ConsentToKeySharingInReminderFlow_Success() {
        receivePositiveTestResultAfterSelfDiagnosisAndDeclineSharingKeysInitially(CONSENT_AND_SUCCESS)
    }

    private fun receivePositiveTestResultAfterSelfDiagnosisAndDeclineSharingKeysInitially(reminderFlow: KeySharingReminderTestFlow) {
        startTestActivity<MainActivity>()

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        // Starting state: App running normally, not in isolation
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms and order test on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 10th Jan
        selfDiagnosis.selfDiagnosePositiveAndPressBack()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertNull(Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertEquals(0, Metrics::launchedTestOrdering)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
        }

        manuelTestResultEntry.enterPositivePCRTestResultAndDeclineExposureKeySharing(this::advanceToNextBackgroundTaskExecution)

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Still in isolation, for both self-diagnosis and positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(0, Metrics::receivedPositiveTestResultViaPolling)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedPositiveBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasTestedPositiveBackgroundTick)
            assertPresent(Metrics::receivedPositiveTestResultEnteredManually)
            assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
        }

        // Current date: 5th Jan -> Analytics packet for: 4rd Jan
        assertOnFields {
            // Still in isolation, for both self-diagnosis and positive test result and with exposureKeyReminderNotification
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedPositiveBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasTestedPositiveBackgroundTick)
            assertEquals(1, Metrics::totalShareExposureKeysReminderNotifications)
        }

        when (reminderFlow) {
            NO_CONSENT -> shareKeysReminder(shouldConsentToShareKeys = false, keySharingFinishesSuccessfully = false)
            CONSENT_BUT_FAILURE -> shareKeysReminder(shouldConsentToShareKeys = true, keySharingFinishesSuccessfully = false)
            CONSENT_AND_SUCCESS -> shareKeysReminder(shouldConsentToShareKeys = true, keySharingFinishesSuccessfully = true)
        }

        // Current date: 6th Jan -> Analytics packet for: 5rd Jan
        assertOnFields {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedPositiveBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasTestedPositiveBackgroundTick)
            when (reminderFlow) {
                NO_CONSENT -> { }
                CONSENT_BUT_FAILURE -> {
                    assertEquals(1, Metrics::consentedToShareExposureKeysInReminderScreen)
                }
                CONSENT_AND_SUCCESS -> {
                    assertEquals(1, Metrics::consentedToShareExposureKeysInReminderScreen)
                    assertEquals(1, Metrics::successfullySharedExposureKeys)
                }
            }
        }

        // Dates: 7th-11th Jan -> Analytics packets for: 6th-10th Jan
        assertOnFieldsForDateRange(7..11) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedPositiveBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasTestedPositiveBackgroundTick)
        }

        // Dates: 12th-25th Jan -> Analytics packets for: 11th-24rd Jan
        assertOnFieldsForDateRange(12..25) {
            // Isolation is over, and analytics are not kept
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasTestedPositiveBackgroundTick)
        }

        // Current date: 25th Jan -> Analytics packet for: 24th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }
}
