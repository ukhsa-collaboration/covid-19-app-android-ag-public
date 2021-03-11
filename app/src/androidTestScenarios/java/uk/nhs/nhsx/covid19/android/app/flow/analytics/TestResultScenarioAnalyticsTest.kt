package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveWillBeInIsolationAndOrderTest
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveWontBeInIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.OrderTest
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.PollingTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import java.time.temporal.ChronoUnit

class TestResultScenarioAnalyticsTest : AnalyticsTest() {

    private val manualTestResultEntry = ManualTestResultEntry(testAppContext)
    private val pollingTestResult = PollingTestResult(this)
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val orderTest = OrderTest(this)

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
            symptomsAndOnsetFlowConfiguration = null,
            expectedScreenState = PositiveWillBeInIsolationAndOrderTest
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
            symptomsAndOnsetFlowConfiguration = null,
            expectedScreenState = PositiveContinueIsolation
        )

        // Current date: 7th Jan -> Analytics packet for: 6th Jan
        assertOnFields {
            // Now in isolation due to positive unconfirmed test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, Metrics::receivedPositiveTestResultEnteredManually)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::hasTestedLFDPositiveBackgroundTick)
        }

        // Dates: 8th-13th Jan -> Analytics packets for: 7th-12th Jan
        assertOnFieldsForDateRange(8..13) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::hasTestedLFDPositiveBackgroundTick)
            assertPresent(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
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

    @Test
    fun enterNegativePCRTest_receiveOldPositiveLFDTest_DoNothing() = notReported {
        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Enters negative PCR test result on 2nd Jan
        manualTestResultEntry.enterNegative()

        waitFor { testResultRobot.checkActivityDisplaysNegativeNotInIsolation() }
        testResultRobot.clickGoodNewsActionButton()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            assertEquals(1, Metrics::receivedNegativeTestResult)
            assertEquals(1, Metrics::receivedNegativeTestResultEnteredManually)
        }

        // Enters positive LFD self reported with test result end date on 31st Dec
        manualTestResultEntry.enterPositive(
            RAPID_SELF_REPORTED,
            requiresConfirmatoryTest = true,
            testEndDate = testAppContext.clock.instant().minus(3, ChronoUnit.DAYS),
            expectedScreenState = PositiveWontBeInIsolation
        )

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            assertEquals(0, Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick)
            assertEquals(1, Metrics::receivedPositiveSelfRapidTestResultEnteredManually)
            assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(0, Metrics::startedIsolation)
            assertEquals(0, Metrics::isIsolatingBackgroundTick)
            assertEquals(0, Metrics::hasTestedSelfRapidPositiveBackgroundTick)
        }
    }
}
