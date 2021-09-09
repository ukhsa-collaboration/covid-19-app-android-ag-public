package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysReminderRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class ShareKeysReminder(private val testAppContext: TestApplicationContext) {

    private val statusRobot = StatusRobot()
    private val shareKeysReminderRobot = ShareKeysReminderRobot()
    private val shareKeysProgressRobot = ProgressRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()

    operator fun invoke(
        shouldConsentToShareKeys: Boolean,
        keySharingFinishesSuccessfully: Boolean
    ) {
        waitFor { statusRobot.checkActivityIsDisplayed() }

        // Navigate to settings and back to trigger the onResume in the StatusActivity
        statusRobot.clickSettings()
        testAppContext.device.pressBack()

        waitFor { shareKeysReminderRobot.checkActivityIsDisplayed() }

        if (shouldConsentToShareKeys) {
            if (keySharingFinishesSuccessfully) {
                waitFor { shareKeysReminderRobot.clickShareResultsButton() }
                waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }
                shareKeysResultRobot.clickActionButton()
            } else {
                testAppContext.executeWhileOffline {
                    shareKeysReminderRobot.clickShareResultsButton()
                    waitFor { shareKeysProgressRobot.checkActivityIsDisplayed() }
                    shareKeysProgressRobot.checkErrorIsDisplayed()
                }
                shareKeysProgressRobot.clickCancelButton()
            }
        } else {
            shareKeysReminderRobot.clickDoNotShareResultsButton()
        }
        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}
