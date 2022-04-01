package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.OLD_WALES_CONTACT_CASE_FLOW
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.RiskyContact
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled

class CompoundIsolationAnalyticsTest : AnalyticsTest() {
    private var selfDiagnosis = SelfDiagnosis(this)
    private var riskyContact = RiskyContact(this)

    @Test
    fun selfDiagnose_thenRiskyContact_isolatingForBothReasons() {
        runWithFeatureEnabled(OLD_WALES_CONTACT_CASE_FLOW) {
            givenLocalAuthorityIsInWales()
            startTestActivity<MainActivity>()
            // Current date: 1st Jan
            // Starting state: App running normally, not in isolation
            // Current date: 2nd Jan -> Analytics packet for: 1st Jan
            assertAnalyticsPacketIsNormal()

            // Complete questionnaire with risky symptoms on 2nd Jan
            // Symptom onset date: Don't remember
            // Isolation end date: 11th Jan
            selfDiagnosis.selfDiagnosePositiveAndPressBack()

            // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
            assertOnFields {
                // Now in isolation due to self-diagnosis
                assertEquals(1, Metrics::completedQuestionnaireAndStartedIsolation)
                assertEquals(1, Metrics::startedIsolation)
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
                assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
                assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            }

            // Has risky contact on 3nd Jan
            // Isolation end date: 14th Jan
            riskyContact.triggerViaCircuitBreaker(this::advanceToNextBackgroundTaskExecution)
            riskyContact.acknowledgeIsolatingViaNotMinorNotVaccinatedForContactQuestionnaireJourney(
                alreadyIsolating = true,
                country = WALES
            )

            // Current date: 4th Jan -> Analytics packet for: 3rd Jan
            assertOnFields {
                // Now also in isolation because of risky contact
                assertEquals(1, Metrics::receivedRiskyContactNotification)
                assertEquals(1, Metrics::acknowledgedStartOfIsolationDueToRiskyContact)
                assertEquals(1, Metrics::receivedActiveIpcToken)
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
                assertPresent(Metrics::isIsolatingForHadRiskyContactBackgroundTick)
                assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
                assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
                assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
                assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
            }

            // Dates: 5th-11th Jan -> Analytics packets for: 3rd-10th Jan
            assertOnFieldsForDateRange(5..11) {
                // Still in isolation for both reasons
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
                assertPresent(Metrics::isIsolatingForHadRiskyContactBackgroundTick)
                assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
                assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
                assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
                assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
            }

            // Dates: 12-14th Jan -> Analytics packets for: 11th-13th Jan
            assertOnFieldsForDateRange(12..14) {
                // Still in isolation because of the risky contact; no longer index case
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForHadRiskyContactBackgroundTick)
                assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
                assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
                assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
                assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
            }

            // Dates: 15th-28th Jan -> Analytics packets for: 14th-27th Jan
            assertOnFieldsForDateRange(15..28) {
                // Isolation is over, but isolation reason still stored for 14 days
                assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
                assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
                assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
            }

            // Current date: 29th Jan -> Analytics packet for: 28th Jan
            // Previous isolation reason no longer stored
            assertAnalyticsPacketIsNormal()
        }
    }
}
