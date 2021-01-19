package uk.nhs.nhsx.covid19.android.app.testordering

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.setTemporaryExposureKeyHistoryResolutionRequired
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithIntents
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertTrue

class TestResultActivityTest : EspressoTest() {

    private val testResultRobot = TestResultRobot()
    private val statusRobot = StatusRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()

    private val isolationStateIndexCaseOnly = Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = DurationDays(),
        indexCase = IndexCase(LocalDate.now(), expiryDate = LocalDate.now().plusDays(1), selfAssessment = false)
    )

    private val isolationStateContactCaseOnly = Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(Instant.now(), null, LocalDate.now().plusDays(1))
    )

    @Test
    fun showContinueToSelfIsolationScreenOnPositive() = reporter(
        scenario = "Test result",
        title = "Positive in isolation",
        description = "User receives positive test result while in isolation",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationStateContactCaseOnly)

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveAndContinueSelfIsolation()
        testResultRobot.checkExposureLinkIsDisplayed()
        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }

        step(
            stepName = "Positive in isolation",
            stepDescription = "User receives positive test result while in isolation"
        )
    }

    @Test
    fun showContinueToSelfIsolationScreenOnNegativeAndNotIndexCaseOnly() = reporter(
        scenario = "Test result",
        title = "Negative in isolation when not at index case only",
        description = "User receives negative test result while in isolation when at index case only",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationStateContactCaseOnly)

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeAndContinueSelfIsolation()
        testResultRobot.checkExposureLinkIsNotDisplayed()
        testResultRobot.checkIsolationActionButtonShowsBackHome()

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }

        step(
            stepName = "Negative in isolation when not at index case only",
            stepDescription = "User receives negative test result while in isolation"
        )
    }

    @Test
    fun showDoNotHaveToSelfIsolateScreenOnNegativeAndIndexCaseOnly() = reporter(
        scenario = "Test result",
        title = "Negative in isolation at index case only",
        description = "User receives negative test result while in isolation when at index case only",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationStateIndexCaseOnly)

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeAndFinishIsolation()
        testResultRobot.checkGoodNewsActionButtonShowsContinue()

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }

        step(
            stepName = "Negative in isolation",
            stepDescription = "User receives negative test result while in isolation"
        )
    }

    @Test
    fun showIsolationScreenWhenReceivingPositiveAndThenNegativeTestResult() = notReported {
        testAppContext.setState(isolationStateIndexCaseOnly)

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                acknowledgedDate = Instant.now()
            )
        )

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = NEGATIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveThenNegativeAndStayInIsolation()

        testResultRobot.checkExposureLinkIsNotDisplayed()

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @RetryFlakyTest
    @Test
    fun showIsolationScreenWhenReceivingNegativeAndThenPositiveTestResultAndSharingKeys() = notReported {
        testAppContext.setState(Default())

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                acknowledgedDate = Instant.now()
            )
        )

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = POSITIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveAndSelfIsolate()

        testResultRobot.checkExposureLinkIsDisplayed()

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @RetryFlakyTest
    @Test
    fun showIsolationScreenWhenReceivingNegativeAndThenPositiveTestResultAndRefusingToShareKeys() = notReported {
        testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, false)

        testAppContext.setState(Default())

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                acknowledgedDate = Instant.now()
            )
        )

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = POSITIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveAndSelfIsolate()

        testResultRobot.checkExposureLinkIsDisplayed()

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickIUnderstandButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun showDoNotHaveToSelfIsolateScreenOnPositive() = notReported {
        testAppContext.setState(Default(previousIsolation = isolationStateIndexCaseOnly))

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveAndFinishIsolation()

        testResultRobot.checkGoodNewsActionButtonShowsContinue()

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun showDoNotHaveToSelfIsolateScreenOnNegative() = notReported {
        testAppContext.setState(Default())

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeAndAlreadyFinishedIsolation()

        testResultRobot.checkGoodNewsActionButtonShowsContinue()

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun showContinueToSelfIsolationScreenOnVoid() = reporter(
        scenario = "Test result",
        title = "Void in isolation",
        description = "User receives void test result while in isolation",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationStateContactCaseOnly)

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = VOID
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysVoidAndContinueSelfIsolation()
        testResultRobot.checkExposureLinkIsNotDisplayed()
        testResultRobot.checkIsolationActionButtonShowsBookFreeTest()

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }

        step(
            stepName = "Void in isolation",
            stepDescription = "User receives void test result while in isolation"
        )
    }

    @Test
    fun showAreNotIsolatingScreenOnNegative() = notReported {
        testAppContext.setState(Default())

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeAndAlreadyFinishedIsolation()
        testResultRobot.checkGoodNewsActionButtonShowsContinue()
    }

    @Test
    fun showAreNotIsolatingScreenOnVoid() = notReported {
        testAppContext.setState(Default())

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = VOID
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysVoidAndNoIsolate()

        testResultRobot.checkGoodNewsActionButtonShowsBookFreeTest()

        testResultRobot.clickGoodNewsActionButton()

        assertTrue { testAppContext.getCurrentState() is Default }
    }

    @Test
    fun onActivityResultTestOrderingOk_navigateToStatus() = notReported {
        runWithIntents {
            testAppContext.setState(isolationStateContactCaseOnly)

            testAppContext.getTestResultsProvider().add(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "a",
                    testEndDate = Instant.now(),
                    testResult = VOID
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
    fun onActivityResultTestOrderingNotOk_finish() = notReported {
        runWithIntents {
            testAppContext.setState(isolationStateContactCaseOnly)

            testAppContext.getTestResultsProvider().add(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "a",
                    testEndDate = Instant.now(),
                    testResult = VOID
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
    fun onBackPressed_navigate() = notReported {
        testAppContext.setState(isolationStateContactCaseOnly)

        testAppContext.getTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = VOID
            )
        )

        val activity = startTestActivity<TestResultActivity>()

        testAppContext.device.pressBack()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }
}
