package uk.nhs.nhsx.covid19.android.app.flow.analytics

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.report.notReported

class ManualTestEntryAnalyticsTest : AnalyticsTest() {

    private var manualTestResultEntry = ManualTestResultEntry(testAppContext)

    @Test
    fun manuallyEnterPositivePCRTestAndGoIntoIsolation() = notReported {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            LAB_RESULT,
            Metrics::receivedPositiveTestResultEnteredManually,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositiveAssistedLFDTestAndGoIntoIsolation() = notReported {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            RAPID_RESULT,
            Metrics::receivedPositiveLFDTestResultEnteredManually,
            Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
            Metrics::hasTestedLFDPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositiveUnassistedLFDTestAndGoIntoIsolation() = notReported {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            RAPID_SELF_REPORTED,
            Metrics::receivedPositiveLFDTestResultEnteredManually,
            Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
            Metrics::hasTestedLFDPositiveBackgroundTick
        )
    }

    // hasTestedPositiveBackgroundTick - Manual
    // >0 if the app is aware that the user has received/entered a positive test
    // this currently happens during an isolation and for the 14 days after isolation
    private fun manuallyEnterPositiveTestAndGoIntoIsolation(
        testKitType: VirologyTestKitType,
        receivedPositiveTestResultEnteredManuallyMetric: MetricsProperty,
        isIsolatingForTestedPositiveBackgroundTickMetric: MetricsProperty,
        hasTestedPositiveBackgroundTickMetric: MetricsProperty
    ) {
        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        assertOnFields {
            // Current date: 2nd Jan -> Analytics packet for: 1st Jan
            assertEquals(0, Metrics::hasSelfDiagnosedBackgroundTick)
            assertEquals(0, Metrics::isIsolatingBackgroundTick)
        }

        // Enters positive LFD test result on 2nd Jan
        // Isolation end date: 12th Jan
        manualTestResultEntry.enterPositive(testKitType)

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, receivedPositiveTestResultEnteredManuallyMetric)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
        }

        // Dates: 4th-12th Jan -> Analytics packets for: 3rd-11th Jan
        assertOnFieldsForDateRange(4..12) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertPresent(hasTestedPositiveBackgroundTickMetric)
        }

        // Dates: 13th-26th Jan -> Analytics packets for: 12th-25th Jan
        assertOnFieldsForDateRange(13..26) {
            // Isolation is over, but isolation reason still stored for 14 days
            assertPresent(hasTestedPositiveBackgroundTickMetric)
        }

        // Current date: 27th Jan -> Analytics packet for: 26th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun manuallyEnterNegativeTestResultSetsReceivedNegativeTestResultEnteredManually() = notReported {
        manualTestResultEntry.enterNegative()

        assertOnFields {
            assertEquals(1, Metrics::receivedNegativeTestResult)
            assertEquals(1, Metrics::receivedNegativeTestResultEnteredManually)
        }
    }

    @Test
    fun manuallyEnterVoidTestResultSetsReceivedVoidTestResultEnteredManually() = notReported {
        manualTestResultEntry.enterVoid()

        assertOnFields {
            assertEquals(1, Metrics::receivedVoidTestResult)
            assertEquals(1, Metrics::receivedVoidTestResultEnteredManually)
        }
    }
}
