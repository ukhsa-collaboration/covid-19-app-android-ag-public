package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.SuccessfullySharedKeysAndNoNeedToReportTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.SuccessfullySharedKeysAndNHSTestNotReported
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.UnsuccessfullySharedKeysAndNoNeedToReportTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.UnsuccessfullySharedKeysAndNHSTestNotReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportThankYouRobot

class SelfReportThankYouActivityTest : EspressoTest() {
    private val selfReportThankYouRobot = SelfReportThankYouRobot()
    private val selfReportAdviceRobot = SelfReportAdviceRobot()

    @Test
    fun showCorrectContentForSuccessfullySharedKeysAndNoNeedToReportTestScreen() {
        startActivityWithExtras(sharingSuccessful = true, hasReported = true)

        waitFor { selfReportThankYouRobot.checkActivityIsDisplayed() }

        selfReportThankYouRobot.checkCorrectParagraphIsShowing(SuccessfullySharedKeysAndNoNeedToReportTest)

        selfReportThankYouRobot.checkInfoViewIsVisible(false)
    }

    @Test
    fun showCorrectContentForSuccessfullySharedKeysAndNHSTestNotReportedScreen() {
        startActivityWithExtras(sharingSuccessful = true, hasReported = false)

        waitFor { selfReportThankYouRobot.checkActivityIsDisplayed() }

        selfReportThankYouRobot.checkCorrectParagraphIsShowing(SuccessfullySharedKeysAndNHSTestNotReported)

        selfReportThankYouRobot.checkInfoViewIsVisible(true)
    }

    @Test
    fun showCorrectContentForUnsuccessfullySharedKeysAndNoNeedToReportTestScreen() {
        startActivityWithExtras(sharingSuccessful = false, hasReported = true)

        waitFor { selfReportThankYouRobot.checkActivityIsDisplayed() }

        selfReportThankYouRobot.checkCorrectParagraphIsShowing(UnsuccessfullySharedKeysAndNoNeedToReportTest)

        selfReportThankYouRobot.checkInfoViewIsVisible(false)
    }

    @Test
    fun showCorrectContentForUnsuccessfullySharedKeysAndNHSTestNotReportedScreen() {
        startActivityWithExtras(sharingSuccessful = false, hasReported = false)

        waitFor { selfReportThankYouRobot.checkActivityIsDisplayed() }

        selfReportThankYouRobot.checkCorrectParagraphIsShowing(UnsuccessfullySharedKeysAndNHSTestNotReported)

        selfReportThankYouRobot.checkInfoViewIsVisible(true)
    }

    @Test
    fun pressBack_doNothing() {
        startActivityWithExtras(sharingSuccessful = true, hasReported = false)

        waitFor { selfReportThankYouRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        waitFor { selfReportThankYouRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressContinue_startAdviceActivity() {
        startActivityWithExtras(sharingSuccessful = true, hasReported = true)

        waitFor { selfReportThankYouRobot.checkActivityIsDisplayed() }

        selfReportThankYouRobot.checkCorrectParagraphIsShowing(SuccessfullySharedKeysAndNoNeedToReportTest)

        selfReportThankYouRobot.checkInfoViewIsVisible(false)

        selfReportThankYouRobot.clickContinue()

        waitFor { selfReportAdviceRobot.checkActivityIsDisplayed() }
    }

    private fun startActivityWithExtras(sharingSuccessful: Boolean, hasReported: Boolean) {
        startTestActivity<SelfReportThankYouActivity> {
            putExtra(SelfReportThankYouActivity.SHARING_SUCCESSFUL, sharingSuccessful)
            putExtra(SelfReportThankYouActivity.HAS_REPORTED, hasReported)
        }
    }
}
