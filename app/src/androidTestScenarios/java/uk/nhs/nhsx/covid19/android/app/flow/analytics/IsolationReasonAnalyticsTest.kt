package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.RiskyContact
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import kotlin.test.assertTrue

class IsolationReasonAnalyticsTest : AnalyticsTest() {

    private var selfDiagnosis = SelfDiagnosis(this)
    private var riskyContact = RiskyContact(this)

    // hasSelfDiagnosedBackgroundTick
    // >0 if the app is aware that the user has completed the questionnaire with symptoms
    // this currently happens during an isolation and for the 14 days after isolation.
    @Test
    fun hasSelfDiagnosedBackgroundTickIsPresentWhenCompletedQuestionnaireAndFor14DaysAfterIsolation() = notReported {
        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 11th Jan
        selfDiagnosis.selfDiagnosePositiveAndPressBack()

        assertTrue { (testAppContext.getCurrentState() as Isolation).isIndexCaseOnly() }

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertEquals(1, Metrics::completedQuestionnaireAndStartedIsolation)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Dates: 4th-11th Jan -> Analytics packets for: 3rd-10th Jan
        assertOnFieldsForDateRange(4..11) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        assertTrue { testAppContext.getCurrentState() is Default }

        // Dates: 12th-25th Jan -> Analytics packets for: 11th-24th Jan
        assertOnFieldsForDateRange(12..25) {
            // Isolation is over, but isolation reason still stored for 14 days
            assertPresent(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Current date: 26th Jan -> Analytics packet for: 25th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun hasHadRiskyContactBackgroundTickIsPresentWhenIsolatingAndFor14DaysAfter() = notReported {
        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        // Starting state: App running normally, not in isolation
        assertAnalyticsPacketIsNormal()

        // Has risky contact on 2nd Jan
        // Isolation end date: 13th Jan
        riskyContact.trigger(this::advanceToNextBackgroundTaskExecution)

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to risky contact
            assertEquals(1, Metrics::receivedRiskyContactNotification)
            assertPresent(Metrics::hasHadRiskyContactBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForHadRiskyContactBackgroundTick)
        }

        // Dates: 4th-13th Jan -> Analytics packets for: 3rd-12th Jan
        assertOnFieldsForDateRange(4..13) {
            // Still in isolation
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
