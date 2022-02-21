package uk.nhs.nhsx.covid19.android.app.testordering

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.exposure.setTemporaryExposureKeyHistoryResolutionRequired
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.addTestResult
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithIntents
import uk.nhs.nhsx.covid19.android.app.util.IsolationChecker
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class TestResultActivityTest(override val configuration: TestConfiguration) : EspressoTest() {

    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val statusRobot = StatusRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)
    private val isolationChecker = IsolationChecker(testAppContext)

    @Test
    @Reported
    fun showContinueToSelfIsolateScreenOnPositiveConfirmatory() = reporter(
        scenario = "Test result",
        title = "Positive confirmatory in isolation",
        description = "User receives positive confirmatory test result while in isolation",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveContinueIsolation()
        testResultRobot.checkExposureLinkIsDisplayed()
        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        step(
            stepName = "Positive in isolation",
            stepDescription = "User receives positive test result while in isolation"
        )

        waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    @Reported
    fun showContinueToSelfIsolateScreenOnNegativeConfirmatoryAndNotIndexCaseOnly() = reporter(
        scenario = "Test result",
        title = "Negative confirmatory in isolation when not at index case only",
        description = "User receives negative confirmatory test result while in isolation when at index case only",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeWillBeInIsolation()
        testResultRobot.checkExposureLinkIsNotDisplayed()
        testResultRobot.checkIsolationActionButtonShowsBackHome()

        testResultRobot.clickIsolationActionButton()

        isolationChecker.assertActiveContactNoIndex()

        step(
            stepName = "Negative confirmatory in isolation when not at index case only",
            stepDescription = "User receives negative confirmatory test result while in isolation"
        )
    }

    @Test
    @Reported
    fun showDoNotHaveToSelfIsolateScreenOnNegativeConfirmatoryAndIndexCaseOnly() = reporter(
        scenario = "Test result",
        title = "Negative confirmatory in isolation at index case only",
        description = "User receives negative confirmatory test result while in isolation when at index case only",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation()
        testResultRobot.checkGoodNewsActionButtonShowsContinue()

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertExpiredIndexNoContact()

        step(
            stepName = "Negative in isolation",
            stepDescription = "User receives negative test result while in isolation"
        )
    }

    @Test
    fun showIsolationScreenWhenReceivingPositiveConfirmatoryAndThenNegativeConfirmatoryTestResult() {
        testAppContext.setState(
            isolationHelper.selfAssessment().asIsolation()
                .addTestResult(
                    testResult = AcknowledgedTestResult(
                        testEndDate = LocalDate.now(),
                        testResult = RelevantVirologyTestResult.POSITIVE,
                        testKitType = LAB_RESULT,
                        requiresConfirmatoryTest = false,
                        acknowledgedDate = LocalDate.now()
                    )
                )
        )

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation()

        testResultRobot.checkExposureLinkIsNotDisplayed()

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        isolationChecker.assertActiveIndexNoContact()
    }

    @RetryFlakyTest
    @Test
    fun showIsolationScreenWhenReceivingNegativeConfirmatoryAndThenPositiveConfirmatoryTestResultAndSharingKeys() {
        testAppContext.setState(
            AcknowledgedTestResult(
                testEndDate = LocalDate.now(),
                testResult = RelevantVirologyTestResult.NEGATIVE,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = false,
                acknowledgedDate = LocalDate.now()
            ).asIsolation()
        )

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation()

        testResultRobot.checkExposureLinkIsDisplayed()

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    @RetryFlakyTest
    @Test
    fun showIsolationScreenWhenReceivingNegativeConfirmatoryAndThenPositiveConfirmatoryTestResultAndRefusingToShareKeys() {
        testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, false)

        testAppContext.setState(isolationHelper.neverInIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation()

        testResultRobot.checkExposureLinkIsDisplayed()

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    fun showDoNotHaveToSelfIsolateScreenOnPositiveConfirmatory() {
        testAppContext.setState(isolationHelper.selfAssessment(expired = true).asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveWontBeInIsolation()

        testResultRobot.checkGoodNewsActionButtonShowsContinue()

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertExpiredIndexNoContact()
    }

    @Test
    fun showDoNotHaveToSelfIsolateScreenOnNegativeConfirmatory() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation()

        testResultRobot.checkGoodNewsActionButtonShowsContinue()

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertNeverIsolating()
    }

    @Test
    @Reported
    fun showContinueToSelfIsolateScreenOnVoidConfirmatory() = reporter(
        scenario = "Test result",
        title = "Void confirmatory in isolation",
        description = "User receives void confirmatory test result while in isolation",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = VOID,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysVoidWillBeInIsolation()
        testResultRobot.checkExposureLinkIsNotDisplayed()
        testResultRobot.checkIsolationActionButtonShowsBookFreeTest()

        testResultRobot.clickIsolationActionButton()

        isolationChecker.assertActiveContactNoIndex()

        step(
            stepName = "Void confirmatory in isolation",
            stepDescription = "User receives void confirmatory test result while in isolation"
        )
    }

    @Test
    fun showAreNotIsolatingScreenOnNegativeConfirmatory() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation()
        testResultRobot.checkGoodNewsActionButtonShowsContinue()

        isolationChecker.assertNeverIsolating()
    }

    @Test
    fun showAreNotIsolatingScreenOnVoidConfirmatory() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = VOID,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysVoidNotInIsolation()

        testResultRobot.checkGoodNewsActionButtonShowsOrderFreeTest()

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertNeverIsolating()
    }

    @Test
    fun showPlodScreenOnPlodResult() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = PLOD,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPlodScreen()

        testResultRobot.checkGoodNewsActionButtonShowsBackHome()

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertNeverIsolating()
    }

    @Test
    fun onActivityResultTestOrderingOk_navigateToStatus() {
        runWithIntents {
            testAppContext.setState(isolationHelper.contact().asIsolation())

            testAppContext.getUnacknowledgedTestResultsProvider().add(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "a",
                    testEndDate = Instant.now(),
                    testResult = VOID,
                    testKitType = LAB_RESULT,
                    diagnosisKeySubmissionSupported = false,
                    requiresConfirmatoryTest = false
                )
            )

            val result = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
            intending(hasComponent(TestOrderingActivity::class.qualifiedName))
                .respondWith(result)

            startTestActivity<TestResultActivity>()
            testResultRobot.clickIsolationActionButton()

            waitFor { statusRobot.checkActivityIsDisplayed() }
        }
    }

    @Test
    fun onActivityResultTestOrderingNotOk_finish() {
        runWithIntents {
            testAppContext.setState(isolationHelper.contact().asIsolation())

            testAppContext.getUnacknowledgedTestResultsProvider().add(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "a",
                    testEndDate = Instant.now(),
                    testResult = VOID,
                    testKitType = LAB_RESULT,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )

            val result = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, Intent())
            intending(hasComponent(TestOrderingActivity::class.qualifiedName))
                .respondWith(result)

            val activity = startTestActivity<TestResultActivity>()
            testResultRobot.clickIsolationActionButton()

            waitFor { assertTrue(activity!!.isDestroyed) }
        }
    }

    @Test
    fun onBackPressed_navigate() {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = VOID,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        val activity = startTestActivity<TestResultActivity>()

        testAppContext.device.pressBack()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }
}
