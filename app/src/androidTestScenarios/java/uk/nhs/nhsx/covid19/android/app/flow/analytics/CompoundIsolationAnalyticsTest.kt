package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.RiskyContact
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.report.notReported

class CompoundIsolationAnalyticsTest : AnalyticsTest() {
    private var selfDiagnosis = SelfDiagnosis(this)
    private var riskyContact = RiskyContact(this)

    @Test
    fun selfDiagnose_thenRiskyContact_isolatingForBothReasons() = notReported {
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
        riskyContact.acknowledge()

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

        // Dates: 5th-14th Jan -> Analytics packets for: 3rd-13th Jan
        assertOnFieldsForDateRange(5..14) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::isIsolatingForHadRiskyContactBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
            assertPresent(Metrics::haveActiveIpcTokenBackgroundTick)
        }
    }
}
