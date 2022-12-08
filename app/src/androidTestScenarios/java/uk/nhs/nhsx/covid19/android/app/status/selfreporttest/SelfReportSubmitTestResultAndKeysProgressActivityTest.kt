package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportThankYouRobot
import java.time.LocalDate

class SelfReportSubmitTestResultAndKeysProgressActivityTest : EspressoTest() {

    private val selfReportSubmitTestResultAndKeysProgressRobot = ProgressRobot()
    private val selfReportThankYouRobot = SelfReportThankYouRobot()

    @Test
    fun startActivityWithKeysLoadingIsShown_FailsDueToAPIError() {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL
        MockApiModule.behaviour.delayMillis = 1000
        startActivityWithExtras()

        selfReportSubmitTestResultAndKeysProgressRobot.checkLoadingIsDisplayed()
        waitFor { selfReportSubmitTestResultAndKeysProgressRobot.checkErrorIsDisplayed() }
    }

    @Test
    fun failsDueToAPIError_PressBack_StartThankYouActivity() {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL
        MockApiModule.behaviour.delayMillis = 1000
        startActivityWithExtras()

        selfReportSubmitTestResultAndKeysProgressRobot.checkLoadingIsDisplayed()
        waitFor { selfReportSubmitTestResultAndKeysProgressRobot.checkErrorIsDisplayed() }

        testAppContext.device.pressBack()
        waitFor { selfReportThankYouRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun apiIsSuccessful_StartThankYouActivity() {
        MockApiModule.behaviour.responseType = ALWAYS_SUCCEED
        MockApiModule.behaviour.delayMillis = 2000
        startActivityWithExtras()

        waitFor { selfReportSubmitTestResultAndKeysProgressRobot.checkLoadingIsDisplayed() }

        waitFor { selfReportThankYouRobot.checkActivityIsDisplayed() }
    }

    private fun startActivityWithExtras() {
        startTestActivity<SelfReportSubmitTestResultAndKeysProgressActivity> {
            putExtra(
                SelfReportSubmitTestResultAndKeysProgressActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE,
        temporaryExposureKeys = listOf(
            NHSTemporaryExposureKey(
                key = "key",
                rollingStartNumber = 2
            )
        ),
        RAPID_SELF_REPORTED,
        true,
        ChosenDate(true, LocalDate.now(testAppContext.clock)),
        true,
        ChosenDate(false, LocalDate.now(testAppContext.clock)),
        true
    )
}
