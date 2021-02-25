package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultOnsetDateRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

class LinkTestResultOnsetDateActivityTest : EspressoTest() {

    private val linkTestResultOnsetDateRobot = LinkTestResultOnsetDateRobot()

    @Test
    fun selectWrongData_showError_clickContinue_keepActivity() = notReported {
        val activity = startTestActivity<LinkTestResultOnsetDateActivity> {
            putExtra(LinkTestResultOnsetDateActivity.EXTRA_TEST_RESULT, testResult)
        }

        linkTestResultOnsetDateRobot.checkActivityIsDisplayed()

        linkTestResultOnsetDateRobot.confirmErrorIsNotShown()

        linkTestResultOnsetDateRobot.clickContinueButton()

        linkTestResultOnsetDateRobot.confirmErrorIsShown()

        linkTestResultOnsetDateRobot.clickContinueButton()

        waitFor { assertFalse(activity!!.isDestroyed) }
    }

    @Test
    fun selectDoNotRememberDate_clickContinue_closeActivity() = notReported {
        val activity = startTestActivity<LinkTestResultOnsetDateActivity> {
            putExtra(LinkTestResultOnsetDateActivity.EXTRA_TEST_RESULT, testResult)
        }

        linkTestResultOnsetDateRobot.checkActivityIsDisplayed()

        linkTestResultOnsetDateRobot.checkDoNotRememberDateIsNotChecked()

        linkTestResultOnsetDateRobot.selectCannotRememberDate()

        linkTestResultOnsetDateRobot.checkDoNotRememberDateIsChecked()

        linkTestResultOnsetDateRobot.confirmErrorIsNotShown()

        linkTestResultOnsetDateRobot.clickContinueButton()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun selectDoNotRememberDate_selectDate_clickContinue_closeActivity() = notReported {
        val activity = startTestActivity<LinkTestResultOnsetDateActivity> {
            putExtra(LinkTestResultOnsetDateActivity.EXTRA_TEST_RESULT, testResult)
        }

        linkTestResultOnsetDateRobot.checkActivityIsDisplayed()

        linkTestResultOnsetDateRobot.checkDoNotRememberDateIsNotChecked()

        linkTestResultOnsetDateRobot.selectCannotRememberDate()

        linkTestResultOnsetDateRobot.checkDoNotRememberDateIsChecked()

        linkTestResultOnsetDateRobot.confirmErrorIsNotShown()

        linkTestResultOnsetDateRobot.clickSelectDate()

        linkTestResultOnsetDateRobot.selectDayOfMonth(LocalDate.now().dayOfMonth)

        linkTestResultOnsetDateRobot.checkDoNotRememberDateIsNotChecked()

        linkTestResultOnsetDateRobot.confirmErrorIsNotShown()

        linkTestResultOnsetDateRobot.clickContinueButton()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun backPressed_keepActivity() = notReported {
        val activity = startTestActivity<LinkTestResultOnsetDateActivity> {
            putExtra(LinkTestResultOnsetDateActivity.EXTRA_TEST_RESULT, testResult)
        }

        linkTestResultOnsetDateRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { assertFalse(activity!!.isDestroyed) }
    }

    private val testResult = ReceivedTestResult(
        diagnosisKeySubmissionToken = "token1",
        testEndDate = Instant.now().minus(1, DAYS),
        testResult = POSITIVE,
        testKitType = LAB_RESULT,
        diagnosisKeySubmissionSupported = true,
        requiresConfirmatoryTest = false
    )
}
