package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_REPORTING
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.DAYS
import kotlin.reflect.KMutableProperty1

class TestResultScenarioAnalyticsTest : AnalyticsTest() {

    private val manualTestResultEntry = ManualTestResultEntry(testAppContext)
    private val testResultRobot = TestResultRobot(testAppContext.app)

    @Test
    fun enterPositiveLFDTest_isolateForUnconfirmed_confirmByPCRTest_isolateForConfirmed() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            startTestActivity<MainActivity>()

            // Current date: 1st Jan
            // Starting state: App running normally, not in isolation
            runBackgroundTasks()

            // Enters positive LFD test result on 1st Jan
            // Isolation end date: 12th Jan
            manualTestResultEntry.enterPositive(
                RAPID_RESULT,
                requiresConfirmatoryTest = true,
                symptomsAndOnsetFlowConfiguration = null,
                expectedScreenState = PositiveWillBeInIsolation()
            )

            // Current date: 2nd Jan -> Analytics packet for: 1st Jan
            assertOnFields {
                // Now in isolation due to positive unconfirmed test result
                assertEquals(1, Metrics::receivedPositiveTestResult)
                assertEquals(1, Metrics::receivedPositiveLFDTestResultEnteredManually)
                assertEquals(1, Metrics::startedIsolation)
                assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
                assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::successfullySharedExposureKeys)
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForTestedLFDPositiveBackgroundTick)
                assertNull(Metrics::hasTestedLFDPositiveBackgroundTick)
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }

            // Dates: 3rd-5th Jan -> Analytics packets for: 2nd-4th Jan
            assertOnFieldsForDateRange(3..5) {
                // Still in isolation
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForTestedLFDPositiveBackgroundTick)
                assertNull(Metrics::hasTestedLFDPositiveBackgroundTick)
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }

            // Enters positive PCR test result on 5th Jan
            // Isolation end date still 12th Jan
            manualTestResultEntry.enterPositive(
                LAB_RESULT,
                requiresConfirmatoryTest = false,
                symptomsAndOnsetFlowConfiguration = null,
                expectedScreenState = PositiveContinueIsolation
            )

            // Current date: 6th Jan -> Analytics packet for: 5th Jan
            assertOnFields {
                // Now in isolation due to positive unconfirmed test result
                assertEquals(1, Metrics::receivedPositiveTestResult)
                assertEquals(1, Metrics::receivedPositiveTestResultEnteredManually)
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
                assertPresent(Metrics::isIsolatingForTestedLFDPositiveBackgroundTick)
                assertNull(Metrics::hasTestedLFDPositiveBackgroundTick)
                assertEquals(1, Metrics::positiveLabResultAfterPositiveLFD)
                assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::successfullySharedExposureKeys)
            }

            // Dates: 7th-12th Jan -> Analytics packets for: 6th-11th Jan
            assertOnFieldsForDateRange(7..12) {
                // Still in isolation
                assertPresent(Metrics::isIsolatingBackgroundTick)
                assertPresent(Metrics::isIsolatingForTestedLFDPositiveBackgroundTick)
                assertNull(Metrics::hasTestedLFDPositiveBackgroundTick)
            }

            // Dates: 13th-26th Jan -> Analytics packets for: 12th-25th Jan
            assertOnFieldsForDateRange(13..26) {
                // Isolation is over, and analytics are not kept
                assertNull(Metrics::hasTestedLFDPositiveBackgroundTick)
            }

            // Current date: 27th Jan -> Analytics packet for: 26th Jan
            // Previous isolation reason no longer stored
            assertAnalyticsPacketIsNormal()
        }

    @Test
    fun enterNegativePCRTest_receiveOldPositiveLFDTest_isolateAndShareKeys() = runWithFeature(SELF_REPORTING, enabled = false) {
        startTestActivity<MainActivity>()

        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Enters negative PCR test result on 1st Jan
        manualTestResultEntry.enterNegative()

        waitFor { testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation(WALES) }
        testResultRobot.clickGoodNewsActionButton()

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        assertOnFields {
            assertEquals(1, Metrics::receivedNegativeTestResult)
            assertEquals(1, Metrics::receivedNegativeTestResultEnteredManually)
        }

        // Enters positive LFD self reported with test result end date on 30th Dec
        manualTestResultEntry.enterPositive(
            RAPID_SELF_REPORTED,
            requiresConfirmatoryTest = true,
            testEndDate = testAppContext.clock.instant().minus(3, ChronoUnit.DAYS),
            expectedScreenState = PositiveWillBeInIsolation(includeBookATestFlow = false)
        )

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            assertEquals(1, Metrics::receivedPositiveSelfRapidTestResultEnteredManually)
            assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, Metrics::startedIsolation)
            assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
            assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
            assertEquals(1, Metrics::successfullySharedExposureKeys)
            assertEquals(1, Metrics::negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit)
            assertNull(Metrics::hasTestedSelfRapidPositiveBackgroundTick)
            assertNull(Metrics::hasTestedSelfRapidPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick)
            assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
        }
    }

    private fun enterPositiveLFD_receivePCR_sendAnalyticsEvent(
        lfdType: VirologyTestKitType,
        pcrResult: VirologyTestResult,
        isConfirmatoryTestOutsideDayLimit: Boolean = false,
        expectedField: KMutableProperty1<Metrics, Int>
    ) {
        startTestActivity<MainActivity>()

        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        // Enters positive LFD test result
        manualTestResultEntry.enterPositive(
            lfdType,
            requiresConfirmatoryTest = true,
            symptomsAndOnsetFlowConfiguration = null,
            expectedScreenState = PositiveWillBeInIsolation(),
            testEndDate = if (isConfirmatoryTestOutsideDayLimit) {
                Instant.now(testAppContext.clock).minus(3, DAYS)
            } else {
                Instant.now(testAppContext.clock)
            }
        )

        advanceToEndOfAnalyticsWindow()

        // Current date: 2nd Jan
        // Enters PCR test
        when (pcrResult) {
            POSITIVE -> manualTestResultEntry.enterPositive(
                LAB_RESULT,
                expectedScreenState = PositiveContinueIsolation,
                requiresConfirmatoryTest = false
            )
            NEGATIVE -> {
                manualTestResultEntry.enterNegative()
                testAppContext.device.pressBack() // To acknowledge the test result
            }
            else -> {
            }
        }

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields(implicitlyAssertNotPresent = false) {
            assertEquals(1, expectedField)
        }

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields(implicitlyAssertNotPresent = false) {
            assertEquals(0, expectedField)
        }
    }

    @Test
    fun enterPositiveLFD_receiveNegativePCRWithinTimeLimit_sendNegativeLabResultAfterPositiveLFDWithinTimeLimit() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            enterPositiveLFD_receivePCR_sendAnalyticsEvent(
                lfdType = RAPID_RESULT,
                pcrResult = NEGATIVE,
                expectedField = Metrics::negativeLabResultAfterPositiveLFDWithinTimeLimit
            )
        }

    @Test
    fun enterPositiveLFD_receiveNegativePCROutsideTimeLimit_sendNegativeLabResultAfterPositiveLFDOutsideTimeLimit() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            enterPositiveLFD_receivePCR_sendAnalyticsEvent(
                lfdType = RAPID_RESULT,
                pcrResult = NEGATIVE,
                isConfirmatoryTestOutsideDayLimit = true,
                expectedField = Metrics::negativeLabResultAfterPositiveLFDOutsideTimeLimit
            )
        }

    @Test
    fun enterPositiveLFD_receivePositivePCR_sendPositiveLabResultAfterPositiveLFD() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            enterPositiveLFD_receivePCR_sendAnalyticsEvent(
                lfdType = RAPID_RESULT,
                pcrResult = POSITIVE,
                expectedField = Metrics::positiveLabResultAfterPositiveLFD
            )
        }

    @Test
    fun enterPositiveSelfReportLFD_receiveNegativePCRWithinTimeLimit_sendNegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            enterPositiveLFD_receivePCR_sendAnalyticsEvent(
                lfdType = RAPID_SELF_REPORTED,
                pcrResult = NEGATIVE,
                expectedField = Metrics::negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit
            )
        }

    @Test
    fun enterPositiveSelfReportLFD_receiveNegativePCROutsideTimeLimit_sendNegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            enterPositiveLFD_receivePCR_sendAnalyticsEvent(
                lfdType = RAPID_SELF_REPORTED,
                pcrResult = NEGATIVE,
                isConfirmatoryTestOutsideDayLimit = true,
                expectedField = Metrics::negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit
            )
        }

    @Test
    fun enterPositiveSelfReportLFD_receivePositivePCR_sendPositiveLabResultAfterPositiveSelfRapidTest() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            enterPositiveLFD_receivePCR_sendAnalyticsEvent(
                lfdType = RAPID_SELF_REPORTED,
                pcrResult = POSITIVE,
                expectedField = Metrics::positiveLabResultAfterPositiveSelfRapidTest
            )
        }
}
