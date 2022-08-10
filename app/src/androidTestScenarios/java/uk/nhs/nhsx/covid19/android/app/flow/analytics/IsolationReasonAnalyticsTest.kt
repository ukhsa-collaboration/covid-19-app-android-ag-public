package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_WALES_CONTACT_CASE_FLOW
import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.RiskyContact
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatures
import uk.nhs.nhsx.covid19.android.app.util.IsolationChecker

class IsolationReasonAnalyticsTest : AnalyticsTest() {

    private val selfDiagnosis = SelfDiagnosis(this)
    private val riskyContact = RiskyContact(this)
    private val isolationChecker = IsolationChecker(testAppContext)

    @Test
    fun hasSelfDiagnosedBackgroundTickIsNotPresentWhenCompletedQuestionnaireAndFor14DaysAfterIsolationWhenSelfIsolationForWalesIsEnabled() {
        startTestActivity<MainActivity>()

        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 11th Jan
        selfDiagnosis.selfDiagnosePositiveAndPressBack()

        isolationChecker.assertActiveIndexNoContact()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertNull(Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Dates: 4th-11th Jan -> Analytics packets for: 3rd-10th Jan
        assertOnFieldsForDateRange(4..11) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        isolationChecker.assertExpiredIndexNoContact()

        // Dates: 12th-25th Jan -> Analytics packets for: 11th-24th Jan
        assertOnFieldsForDateRange(12..25) {
            // Isolation is over, and analytics are not kept
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Current date: 26th Jan -> Analytics packet for: 25th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()

        isolationChecker.assertNeverIsolating()
    }

    @Test
    fun hasSelfDiagnosedBackgroundTickIsNotPresentWhenCompletedQuestionnaireWhenSelfIsolationForWalesIsDisabled() {
        startTestActivity<MainActivity>()

        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms on 2nd Jan
        // Symptom onset date: Don't remember
        selfDiagnosis.selfDiagnosePositiveAndPressBackIsolationDisabled()

        isolationChecker.assertNeverIsolating()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Not in isolation due to self-diagnosis
            assertNull(Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(0, Metrics::startedIsolation)
            assertEquals(0, Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertEquals(1, Metrics::completedV3SymptomsQuestionnaireAndHasSymptoms)
        }
    }

    @Test
    fun hasHadRiskyContactBackgroundTickIsPresentWhenIsolatingAndFor14DaysAfter() {
        runWithFeatures(listOf(OLD_WALES_CONTACT_CASE_FLOW, SELF_ISOLATION_HOME_SCREEN_BUTTON_WALES), true) {
            givenLocalAuthorityIsInWales()
            startTestActivity<MainActivity>()

            // Current date: 2nd Jan -> Analytics packet for: 1st Jan
            // Starting state: App running normally, not in isolation
            assertAnalyticsPacketIsNormal()

            // Has risky contact on 2nd Jan
            // Isolation end date: 13th Jan
            riskyContact.triggerViaCircuitBreaker(this::advanceToNextBackgroundTaskExecution)
            riskyContact.acknowledgeIsolatingViaNotMinorNotVaccinatedForContactQuestionnaireJourney(country = WALES)

            // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
            assertOnFields {
                // Now in isolation due to risky contact
                assertEquals(1, Metrics::receivedRiskyContactNotification)
                assertEquals(1, Metrics::receivedActiveIpcToken)
                assertEquals(1, Metrics::startedIsolation)
                assertEquals(1, Metrics::acknowledgedStartOfIsolationDueToRiskyContact)
                assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
                assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForHadRiskyContactBackgroundTick)
            }

            // Dates: 4th Jan -> Analytics packet for: 3rd Jan
            assertOnFields {
                // Still in isolation
                assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
                assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForHadRiskyContactBackgroundTick)
            }

            // Dates: 5th-13th Jan -> Analytics packets for: 4th-12th Jan
            assertOnFieldsForDateRange(5..13) {
                // Still in isolation
                assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
                assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForHadRiskyContactBackgroundTick)
            }

            // Dates: 14th-27th Jan -> Analytics packets for: 13th-26th Jan
            assertOnFieldsForDateRange(14..27) {
                // Isolation is over, but isolation reason still stored for 14 days
                assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
            }

            // Current date: 28th Jan -> Analytics packet for: 27th Jan
            // Previous isolation reason no longer stored
            assertAnalyticsPacketIsNormal()
        }
    }
}
