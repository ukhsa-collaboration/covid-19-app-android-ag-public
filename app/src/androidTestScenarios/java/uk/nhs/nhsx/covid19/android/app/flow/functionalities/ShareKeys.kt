package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class ShareKeys {

    private val statusRobot = StatusRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()

    operator fun invoke() {
        waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}
