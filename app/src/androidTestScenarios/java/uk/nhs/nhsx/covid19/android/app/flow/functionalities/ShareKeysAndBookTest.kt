package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import android.content.Context
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BookFollowUpTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class ShareKeysAndBookTest(context: Context) {

    private val testResultRobot = TestResultRobot(context)
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()
    private val bookFollowUpTestRobot = BookFollowUpTestRobot()
    private val testOrderingRobot = TestOrderingRobot()

    operator fun invoke() {
        testResultRobot.clickIsolationActionButton()

        waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }
        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityWithContinueButtonIsDisplayed() }
        shareKeysResultRobot.clickActionButton()

        waitFor { bookFollowUpTestRobot.checkActivityIsDisplayed() }
        bookFollowUpTestRobot.clickBookFollowUpTestButton()

        waitFor { testOrderingRobot.checkActivityIsDisplayed() }
    }
}
