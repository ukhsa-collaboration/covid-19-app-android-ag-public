package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.Test
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
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertTrue

class TestResultActivityTest : EspressoTest() {

    private val testResultRobot = TestResultRobot()

    private val isolationStateIndexCaseOnly = Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = DurationDays(),
        indexCase = IndexCase(LocalDate.now(), expiryDate = LocalDate.now().plusDays(1))
    )

    private val isolationStateContactCaseOnly = Isolation(
        isolationStart = Instant.now(),
        isolationConfiguration = DurationDays(),
        contactCase = ContactCase(Instant.now(), LocalDate.now().plusDays(1))
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
        testResultRobot.checkGoodNewsActionButtonShowsBackHome()

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

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        assertTrue { testAppContext.getCurrentState() is Isolation }
    }

    @Test
    fun showIsolationScreenWhenReceivingNegativeAndThenPositiveTestResult() = notReported {
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

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

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
}
