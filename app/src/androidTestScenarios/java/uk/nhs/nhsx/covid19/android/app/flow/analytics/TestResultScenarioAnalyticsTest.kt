package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.report.notReported

class TestResultScenarioAnalyticsTest : AnalyticsTest() {

    private val manualTestResultEntry = ManualTestResultEntry(testAppContext)

    @Test
    fun enterPositiveLFDTest_isolateForUnconfirmed_confirmByPCRTest_isolateForConfirmed() = notReported {
        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Enters positive LFD test result on 2nd Jan
        // Isolation end date: 13th Jan
        manualTestResultEntry.enterPositive(
            RAPID_RESULT,
            requiresConfirmatoryTest = true,
            symptomsAndOnsetFlowConfiguration = null
        )

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to positive unconfirmed test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, Metrics::receivedPositiveLFDTestResultEnteredManually)
            assertEquals(1, Metrics::startedIsolation)
            assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::hasTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
        }

        // Dates: 4th-6th Jan -> Analytics packets for: 3rd-5th Jan
        assertOnFieldsForDateRange(4..6) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::hasTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
        }

        // Enters positive PCR test result on 6th Jan
        // Isolation end date still 13th Jan
        manualTestResultEntry.enterPositive(
            LAB_RESULT,
            requiresConfirmatoryTest = false,
            symptomsAndOnsetFlowConfiguration = null
        )

        // Current date: 7rd Jan -> Analytics packet for: 6nd Jan
        assertOnFields {
            // Now in isolation due to positive unconfirmed test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, Metrics::receivedPositiveTestResultEnteredManually)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::hasTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
        }

        // Dates: 8th-13th Jan -> Analytics packets for: 7rd-12th Jan
        assertOnFieldsForDateRange(8..13) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::hasTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
        }

        // Dates: 14th-27th Jan -> Analytics packets for: 13th-26th Jan
        assertOnFieldsForDateRange(14..27) {
            // Isolation is over, but isolation reason still stored for 14 days
            assertPresent(Metrics::hasTestedLFDPositiveBackgroundTick)
        }

        // Current date: 28th Jan -> Analytics packet for: 27th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }
}
