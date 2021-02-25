package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import org.junit.Assert.assertFalse
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertTrue

class LinkTestResultSymptomsActivityTest : EspressoTest() {

    private val linkTestResultSymptomsRobot = LinkTestResultSymptomsRobot()

    @Test
    fun clickYes_closeActivity() = notReported {
        val activity = startTestActivity<LinkTestResultSymptomsActivity> {
            putExtra(LinkTestResultSymptomsActivity.EXTRA_TEST_RESULT, testResult)
        }

        linkTestResultSymptomsRobot.checkActivityIsDisplayed()

        linkTestResultSymptomsRobot.clickYes()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun clickNo_closeActivity() = notReported {
        val activity = startTestActivity<LinkTestResultSymptomsActivity> {
            putExtra(LinkTestResultSymptomsActivity.EXTRA_TEST_RESULT, testResult)
        }

        linkTestResultSymptomsRobot.checkActivityIsDisplayed()

        linkTestResultSymptomsRobot.clickNo()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun onBackPressed_keepActivity() = notReported {
        val activity = startTestActivity<LinkTestResultSymptomsActivity> {
            putExtra(LinkTestResultSymptomsActivity.EXTRA_TEST_RESULT, testResult)
        }

        linkTestResultSymptomsRobot.checkActivityIsDisplayed()

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
