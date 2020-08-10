package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import java.time.Instant
import java.time.LocalDate

class TestResultActivityTest : EspressoTest() {

    private val testResultRobot = TestResultRobot()

    @Test
    fun showContinueToSelfIsolationScreenOnPositive() = notReported {
        testAppContext.setState(
            Isolation(
                Instant.now(), LocalDate.now().plusDays(1),
                contactCase = ContactCase(Instant.now())
            )
        )

        testAppContext.setLatestTestResultProvider(
            LatestTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveAndContinueSelfIsolation()
    }

    @Test
    fun showContinueToSelfIsolationScreenOnNegative() = notReported {
        testAppContext.setState(
            Isolation(
                Instant.now(), LocalDate.now().plusDays(1),
                contactCase = ContactCase(Instant.now())
            )
        )

        testAppContext.setLatestTestResultProvider(
            LatestTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeAndContinueSelfIsolation()
    }

    @Test
    fun showDoNotHaveToSelfIsolateScreenOnPositive() = notReported {
        testAppContext.setState(Default())

        testAppContext.setLatestTestResultProvider(
            LatestTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveAndFinishIsolation()
    }

    @Test
    fun showDoNotHaveToSelfIsolateScreenOnNegative() = notReported {
        testAppContext.setState(Default())

        testAppContext.setLatestTestResultProvider(
            LatestTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeAndFinishIsolation()
    }
}
