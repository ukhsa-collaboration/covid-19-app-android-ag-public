package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_REPORTING
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveContinueIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.ExpectedScreenAfterPositiveTestResult.PositiveWillBeInIsolation
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.ManualTestResultEntry.SymptomsAndOnsetFlowConfiguration
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ManualTestEntryAnalyticsTest : AnalyticsTest() {

    private val manualTestResultEntry = ManualTestResultEntry(testAppContext)
    private val selfDiagnosis = SelfDiagnosis(this)

    @Test
    fun manuallyEnterPositivePCRTest_noSymptoms_thenGoIntoIsolation() = runWithFeature(SELF_REPORTING, enabled = false) {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            LAB_RESULT,
            symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                didHaveSymptoms = false,
                didRememberOnsetSymptomsDate = false
            ),
            requiresConfirmatoryTest = false,
            expectedScreenState = PositiveWillBeInIsolation(),
            country = WALES,
            Metrics::receivedPositiveTestResultEnteredManually,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositivePCRTest_noSymptoms_enterSymptomsLater() = runWithFeature(SELF_REPORTING, enabled = false) {
        manuallyEnterPositiveTestAndGoIntoIsolationThenEnterSymptoms(
            LAB_RESULT,
            symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                didHaveSymptoms = false,
                didRememberOnsetSymptomsDate = false
            ),
            requiresConfirmatoryTest = false,
            expectedScreenState = PositiveWillBeInIsolation(),
            Metrics::receivedPositiveTestResultEnteredManually,
            Metrics::isIsolatingForTestedPositiveBackgroundTick,
            Metrics::hasTestedPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositiveUnassistedLFDTest_enterSymptomsLater() = runWithFeature(SELF_REPORTING, enabled = false) {
        manuallyEnterPositiveTestAndGoIntoIsolationThenEnterSymptoms(
            RAPID_SELF_REPORTED,
            symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                didHaveSymptoms = false,
                didRememberOnsetSymptomsDate = false
            ),
            requiresConfirmatoryTest = false,
            expectedScreenState = PositiveWillBeInIsolation(),
            Metrics::receivedPositiveSelfRapidTestResultEnteredManually,
            Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick,
            Metrics::hasTestedSelfRapidPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositiveAssistedLFDTest_enterSymptomsLater() = runWithFeature(SELF_REPORTING, enabled = false) {
        manuallyEnterPositiveTestAndGoIntoIsolationThenEnterSymptoms(
            RAPID_RESULT,
            symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                didHaveSymptoms = false,
                didRememberOnsetSymptomsDate = false
            ),
            requiresConfirmatoryTest = false,
            expectedScreenState = PositiveWillBeInIsolation(),
            Metrics::receivedPositiveLFDTestResultEnteredManually,
            Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
            Metrics::hasTestedLFDPositiveBackgroundTick
        )
    }

    @Test
    fun manuallyEnterPositivePCRTest_confirmSymptoms_selectExplicitDate_thenGoIntoIsolation() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            manuallyEnterPositiveTestAndGoIntoIsolation(
                LAB_RESULT,
                symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                    didHaveSymptoms = true,
                    didRememberOnsetSymptomsDate = true
                ),
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveWillBeInIsolation(),
                country = WALES,
                Metrics::receivedPositiveTestResultEnteredManually,
                Metrics::isIsolatingForTestedPositiveBackgroundTick,
                Metrics::hasTestedPositiveBackgroundTick
            )
        }

    @Test
    fun manuallyEnterPositivePCRTest_confirmSymptoms_cannotRememberOnsetDate_thenGoIntoIsolation() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            manuallyEnterPositiveTestAndGoIntoIsolation(
                LAB_RESULT,
                symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                    didHaveSymptoms = true,
                    didRememberOnsetSymptomsDate = false
                ),
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveWillBeInIsolation(),
                country = WALES,
                Metrics::receivedPositiveTestResultEnteredManually,
                Metrics::isIsolatingForTestedPositiveBackgroundTick,
                Metrics::hasTestedPositiveBackgroundTick
            )
        }

    @Test
    fun manuallyEnterPositiveAssistedLFDTest_noSymptoms_thenGoIntoIsolationEngland() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            givenLocalAuthorityIsInEngland()
            manuallyEnterPositiveTestAndGoIntoIsolation(
                RAPID_RESULT,
                symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                    didHaveSymptoms = false,
                    didRememberOnsetSymptomsDate = false
                ),
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveWillBeInIsolation(),
                country = ENGLAND,
                Metrics::receivedPositiveLFDTestResultEnteredManually,
                Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
                Metrics::hasTestedLFDPositiveBackgroundTick
            )
        }

    @Test
    fun manuallyEnterPositiveAssistedLFDTest_noSymptoms_thenGoIntoIsolation_Wales() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            manuallyEnterPositiveTestAndGoIntoIsolation(
                RAPID_RESULT,
                symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                    didHaveSymptoms = false,
                    didRememberOnsetSymptomsDate = false
                ),
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveWillBeInIsolation(),
                country = WALES,
                Metrics::receivedPositiveLFDTestResultEnteredManually,
                Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
                Metrics::hasTestedLFDPositiveBackgroundTick
            )
        }

    @Test
    fun manuallyEnterPositiveAssistedLFDTest_confirmSymptoms_selectExplicitDate_thenGoIntoIsolation_Wales() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            manuallyEnterPositiveTestAndGoIntoIsolation(
                RAPID_RESULT,
                symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                    didHaveSymptoms = true,
                    didRememberOnsetSymptomsDate = true
                ),
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveWillBeInIsolation(),
                country = WALES,
                Metrics::receivedPositiveLFDTestResultEnteredManually,
                Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
                Metrics::hasTestedLFDPositiveBackgroundTick
            )
        }

    @Test
    fun manuallyEnterPositiveAssistedLFDTest_confirmSymptoms_cannotRememberOnsetDate_thenGoIntoIsolation_Wales() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            manuallyEnterPositiveTestAndGoIntoIsolation(
                RAPID_RESULT,
                symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                    didHaveSymptoms = true,
                    didRememberOnsetSymptomsDate = false
                ),
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveWillBeInIsolation(),
                country = WALES,
                Metrics::receivedPositiveLFDTestResultEnteredManually,
                Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
                Metrics::hasTestedLFDPositiveBackgroundTick
            )
        }

    @Test
    fun manuallyEnterPositiveUnassistedLFDTest_noSymptoms_thenGoIntoIsolation_England() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            givenLocalAuthorityIsInEngland()
            manuallyEnterPositiveTestAndGoIntoIsolation(
                RAPID_SELF_REPORTED,
                symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                    didHaveSymptoms = false,
                    didRememberOnsetSymptomsDate = false
                ),
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveWillBeInIsolation(),
                country = ENGLAND,
                Metrics::receivedPositiveSelfRapidTestResultEnteredManually,
                Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick,
                Metrics::hasTestedSelfRapidPositiveBackgroundTick
            )
        }

    @Test
    fun manuallyEnterPositiveUnassistedLFDTest_noSymptoms_thenGoIntoIsolation_Wales() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            manuallyEnterPositiveTestAndGoIntoIsolation(
                RAPID_SELF_REPORTED,
                symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                    didHaveSymptoms = false,
                    didRememberOnsetSymptomsDate = false
                ),
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveWillBeInIsolation(),
                country = WALES,
                Metrics::receivedPositiveSelfRapidTestResultEnteredManually,
                Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick,
                Metrics::hasTestedSelfRapidPositiveBackgroundTick
            )
        }

    @Test
    fun manuallyEnterPositiveUnassistedLFDTest_confirmSymptoms_selectExplicitDate_thenGoIntoIsolation_Wales() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            manuallyEnterPositiveTestAndGoIntoIsolation(
                RAPID_SELF_REPORTED,
                symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                    didHaveSymptoms = true,
                    didRememberOnsetSymptomsDate = true
                ),
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveWillBeInIsolation(),
                country = WALES,
                Metrics::receivedPositiveSelfRapidTestResultEnteredManually,
                Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick,
                Metrics::hasTestedSelfRapidPositiveBackgroundTick
            )
        }

    @Test
    fun manuallyEnterPositiveUnassistedLFDTest_confirmSymptoms_cannotRememberOnsetDate_thenGoIntoIsolation_Wales() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            manuallyEnterPositiveTestAndGoIntoIsolation(
                RAPID_SELF_REPORTED,
                symptomsAndOnsetFlowConfiguration = SymptomsAndOnsetFlowConfiguration(
                    didHaveSymptoms = true,
                    didRememberOnsetSymptomsDate = false
                ),
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveWillBeInIsolation(),
                country = WALES,
                Metrics::receivedPositiveSelfRapidTestResultEnteredManually,
                Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick,
                Metrics::hasTestedSelfRapidPositiveBackgroundTick
            )
        }

    @Test
    fun manuallyEnterPositiveUnconfirmedLFDTestAndGoIntoIsolation() = runWithFeature(SELF_REPORTING, enabled = false) {
        manuallyEnterPositiveTestAndGoIntoIsolation(
            RAPID_RESULT,
            symptomsAndOnsetFlowConfiguration = null,
            requiresConfirmatoryTest = true,
            expectedScreenState = PositiveWillBeInIsolation(),
            country = WALES,
            Metrics::receivedPositiveLFDTestResultEnteredManually,
            Metrics::isIsolatingForTestedLFDPositiveBackgroundTick,
            Metrics::hasTestedLFDPositiveBackgroundTick
        )
    }

    private fun manuallyEnterPositiveTestAndGoIntoIsolation(
        testKitType: VirologyTestKitType,
        symptomsAndOnsetFlowConfiguration: SymptomsAndOnsetFlowConfiguration?,
        requiresConfirmatoryTest: Boolean,
        expectedScreenState: ExpectedScreenAfterPositiveTestResult,
        country: PostCodeDistrict,
        receivedPositiveTestResultEnteredManuallyMetric: MetricsProperty,
        isIsolatingForTestedPositiveBackgroundTickMetric: MetricsProperty,
        hasTestedPositiveBackgroundTickMetric: MetricsProperty
    ) {
        startTestActivity<MainActivity>()

        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Enters positive test result on 2nd Jan
        // Isolation end date: 13th Jan
        manualTestResultEntry.enterPositive(
            testKitType,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            symptomsAndOnsetFlowConfiguration = symptomsAndOnsetFlowConfiguration,
            expectedScreenState = expectedScreenState,
            country = country
        )

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, receivedPositiveTestResultEnteredManuallyMetric)
            assertEquals(1, Metrics::startedIsolation)
            if (symptomsAndOnsetFlowConfiguration != null) {
                assertNull(Metrics::didAskForSymptomsOnPositiveTestEntry)
                if (symptomsAndOnsetFlowConfiguration.didHaveSymptoms) {
                    assertEquals(1, Metrics::didHaveSymptomsBeforeReceivedTestResult)
                }
                if (symptomsAndOnsetFlowConfiguration.didRememberOnsetSymptomsDate) {
                    assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
                    assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
                    assertEquals(1, Metrics::didRememberOnsetSymptomsDateBeforeReceivedTestResult)
                }
            }
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(hasTestedPositiveBackgroundTickMetric)
            if (requiresConfirmatoryTest) {
                assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
            if (expectedScreenState == PositiveContinueIsolation ||
                expectedScreenState == PositiveWillBeInIsolation()
            ) {
                assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::successfullySharedExposureKeys)
            }
        }

        // Dates: 4th-13th Jan -> Analytics packets for: 3rd-12th Jan
        assertOnFieldsForDateRange(4..13) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(hasTestedPositiveBackgroundTickMetric)
            if (requiresConfirmatoryTest) {
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
            if (symptomsAndOnsetFlowConfiguration?.didRememberOnsetSymptomsDate == true) {
                assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
                assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            }
        }

        // Dates: 14th-27th Jan -> Analytics packets for: 13th-26th Jan
        assertOnFieldsForDateRange(14..27) {
            // Isolation is over, and analytics are not kept
            assertNull(hasTestedPositiveBackgroundTickMetric)
            if (symptomsAndOnsetFlowConfiguration?.didRememberOnsetSymptomsDate == true) {
                assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            }
        }

        // Current date: 28th Jan -> Analytics packet for: 27th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    private fun manuallyEnterPositiveTestAndGoIntoIsolationThenEnterSymptoms(
        testKitType: VirologyTestKitType,
        symptomsAndOnsetFlowConfiguration: SymptomsAndOnsetFlowConfiguration?,
        requiresConfirmatoryTest: Boolean,
        expectedScreenState: ExpectedScreenAfterPositiveTestResult,
        receivedPositiveTestResultEnteredManuallyMetric: MetricsProperty,
        isIsolatingForTestedPositiveBackgroundTickMetric: MetricsProperty,
        hasTestedPositiveBackgroundTickMetric: MetricsProperty
    ) {
        startTestActivity<MainActivity>()

        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Enters positive LFD test result on 1st Jan
        // Isolation end date: 12th Jan
        manualTestResultEntry.enterPositive(
            testKitType,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            symptomsAndOnsetFlowConfiguration = symptomsAndOnsetFlowConfiguration,
            expectedScreenState = expectedScreenState
        )

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        assertOnFields {
            // Now in isolation due to positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, receivedPositiveTestResultEnteredManuallyMetric)
            assertEquals(1, Metrics::startedIsolation)
            if (symptomsAndOnsetFlowConfiguration != null) {
                assertNull(Metrics::didAskForSymptomsOnPositiveTestEntry)
                if (symptomsAndOnsetFlowConfiguration.didHaveSymptoms) {
                    assertEquals(1, Metrics::didHaveSymptomsBeforeReceivedTestResult)
                    assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
                    assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
                }
                if (symptomsAndOnsetFlowConfiguration.didRememberOnsetSymptomsDate) {
                    assertEquals(1, Metrics::didRememberOnsetSymptomsDateBeforeReceivedTestResult)
                }
            }
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(hasTestedPositiveBackgroundTickMetric)
            if (requiresConfirmatoryTest) {
                assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
            if (expectedScreenState == PositiveContinueIsolation ||
                expectedScreenState == PositiveWillBeInIsolation()
            ) {
                assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::successfullySharedExposureKeys)
            }
        }

        selfDiagnosis.selfDiagnosePositiveAndPressBack(ExplicitDate(LocalDate.now(testAppContext.clock)))

        // Dates: 3rd-13th Jan -> Analytics packets for: 2rd-12th Jan
        assertOnFieldsForDateRange(3..13) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(hasTestedPositiveBackgroundTickMetric)
            if (requiresConfirmatoryTest) {
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
            if (symptomsAndOnsetFlowConfiguration?.didHaveSymptoms == true) {
                assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            }
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Dates: 14th-27th Jan -> Analytics packets for: 13th-26th Jan
        assertOnFieldsForDateRange(14..27) {
            // Isolation is over, and analytics are not kept
            assertNull(hasTestedPositiveBackgroundTickMetric)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Current date: 28th Jan -> Analytics packet for: 27th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun manuallyEnterPositivePCRTestAfterSelfDiagnosisAndContinueIsolation() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            manuallyEnterPositiveTestAfterSelfDiagnosisAndContinueIsolation(
                LAB_RESULT,
                requiresConfirmatoryTest = false,
                expectedScreenState = PositiveContinueIsolation,
                Metrics::receivedPositiveTestResultEnteredManually,
                Metrics::isIsolatingForTestedPositiveBackgroundTick,
                Metrics::hasTestedPositiveBackgroundTick
            )
        }

    private fun manuallyEnterPositiveTestAfterSelfDiagnosisAndContinueIsolation(
        testKitType: VirologyTestKitType,
        requiresConfirmatoryTest: Boolean,
        expectedScreenState: ExpectedScreenAfterPositiveTestResult,
        receivedPositiveTestResultEnteredManuallyMetric: MetricsProperty,
        isIsolatingForTestedPositiveBackgroundTickMetric: MetricsProperty,
        hasTestedPositiveBackgroundTickMetric: MetricsProperty
    ) {
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

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertNull(Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Enters positive LFD test result on 3rd Jan
        // Isolation end date: 11th Jan
        manualTestResultEntry.enterPositive(
            testKitType,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            expectedScreenState = expectedScreenState
        )

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Now in isolation due to positive test result
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, receivedPositiveTestResultEnteredManuallyMetric)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(hasTestedPositiveBackgroundTickMetric)
            if (requiresConfirmatoryTest) {
                assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
            if (expectedScreenState == PositiveContinueIsolation ||
                expectedScreenState == PositiveWillBeInIsolation()
            ) {
                assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::successfullySharedExposureKeys)
            }
        }

        // Dates: 5th-11th Jan -> Analytics packets for: 4th-10th Jan
        assertOnFieldsForDateRange(5..11) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(hasTestedPositiveBackgroundTickMetric)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            if (requiresConfirmatoryTest) {
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
        }

        // Dates: 12th-25th Jan -> Analytics packets for: 11th-24th Jan
        assertOnFieldsForDateRange(12..25) {
            // Isolation is over, and analytics are not kept
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(hasTestedPositiveBackgroundTickMetric)
        }

        // Current date: 26th Jan -> Analytics packet for: 25th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun manuallyEnterOldPositivePCRTestAfterSelfDiagnosisAndContinueIsolation() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            manuallyEnterOldPositiveTestAfterSelfDiagnosisAndContinueIsolation(
                LAB_RESULT,
                requiresConfirmatoryTest = false,
                Metrics::receivedPositiveTestResultEnteredManually,
                Metrics::isIsolatingForTestedPositiveBackgroundTick,
                Metrics::hasTestedPositiveBackgroundTick
            )
        }

    private fun manuallyEnterOldPositiveTestAfterSelfDiagnosisAndContinueIsolation(
        testKitType: VirologyTestKitType,
        requiresConfirmatoryTest: Boolean,
        receivedPositiveTestResultEnteredManuallyMetric: MetricsProperty,
        isIsolatingForTestedPositiveBackgroundTickMetric: MetricsProperty,
        hasTestedPositiveBackgroundTickMetric: MetricsProperty
    ) {
        startTestActivity<MainActivity>()

        val startDate = testAppContext.clock.instant()

        // Current date: 1st Jan
        // Starting state: App running normally, not in isolation
        runBackgroundTasks()

        // Current date: 2nd Jan -> Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()

        // Complete questionnaire with risky symptoms on 2nd Jan
        // Symptom onset date: Don't remember
        // Isolation end date: 11th Jan
        selfDiagnosis.selfDiagnosePositiveAndPressBack()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            // Now in isolation due to self-diagnosis
            assertNull(Metrics::completedQuestionnaireAndStartedIsolation)
            assertEquals(1, Metrics::startedIsolation)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
        }

        // Enters positive LFD test result on 3rd Jan
        // The old test is added to the index case. No changes to isolation duration
        val testEndDate = startDate.minus(2, ChronoUnit.DAYS)
        manualTestResultEntry.enterPositive(
            testKitType,
            symptomsAndOnsetFlowConfiguration = null,
            requiresConfirmatoryTest = requiresConfirmatoryTest,
            testEndDate = testEndDate,
            expectedScreenState = PositiveContinueIsolation
        )

        // Current date: 4th Jan -> Analytics packet for: 3rd Jan
        assertOnFields {
            // Still in isolation due to self-diagnosis, now also with test
            assertEquals(1, Metrics::receivedPositiveTestResult)
            assertEquals(1, receivedPositiveTestResultEnteredManuallyMetric)
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(hasTestedPositiveBackgroundTickMetric)
            if (requiresConfirmatoryTest) {
                assertEquals(1, Metrics::receivedUnconfirmedPositiveTestResult)
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
            assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
            assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
            assertEquals(1, Metrics::successfullySharedExposureKeys)
        }

        // Dates: 5th-11th Jan -> Analytics packets for: 4th-10th Jan
        assertOnFieldsForDateRange(5..11) {
            // Still in isolation
            assertPresent(Metrics::isIsolatingBackgroundTick)
            assertNull(Metrics::isIsolatingForSelfDiagnosedBackgroundTick)
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertPresent(isIsolatingForTestedPositiveBackgroundTickMetric)
            assertNull(hasTestedPositiveBackgroundTickMetric)
            if (requiresConfirmatoryTest) {
                assertPresent(Metrics::isIsolatingForUnconfirmedTestBackgroundTick)
            }
        }

        // Dates: 12th-25th Jan -> Analytics packets for: 11th-24th Jan
        assertOnFieldsForDateRange(12..25) {
            // Isolation is over, and analytics are not kept
            assertNull(Metrics::hasSelfDiagnosedBackgroundTick)
            assertNull(hasTestedPositiveBackgroundTickMetric)
        }

        // Current date: 26th Jan -> Analytics packet for: 26th Jan
        // Previous isolation reason no longer stored
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun manuallyEnterNegativeTestResultSetsReceivedNegativeTestResultEnteredManually() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            startTestActivity<MainActivity>()
            manualTestResultEntry.enterNegative()

            assertOnFields {
                assertEquals(1, Metrics::receivedNegativeTestResult)
                assertEquals(1, Metrics::receivedNegativeTestResultEnteredManually)
            }
        }

    @Test
    fun manuallyEnterVoidTestResultSetsReceivedVoidTestResultEnteredManually() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            startTestActivity<MainActivity>()
            manualTestResultEntry.enterVoid()

            assertOnFields {
                assertEquals(1, Metrics::receivedVoidTestResult)
                assertEquals(1, Metrics::receivedVoidTestResultEnteredManually)
            }
        }
}
