package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.HowDoYouFeelRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper

class HowDoYouFeelActivityTest : EspressoTest(), LocalAuthoritySetupHelper {
    private val howDoYouFeelRobot = HowDoYouFeelRobot()

    @Test
    fun whenLocalAuthorityIsInEnglandAndUserClicksContinue_errorIsDisplayed() {
        givenLocalAuthorityIsInEngland()
        startActivityWithExtras()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        howDoYouFeelRobot.checkErrorVisible(false)
        howDoYouFeelRobot.checkNothingSelected()
        howDoYouFeelRobot.clickContinueButton()

        waitFor { howDoYouFeelRobot.checkErrorVisible(true) }
    }

    @Test
    fun whenLocalAuthorityIsInEnglandAndUserHasMadeSelection_canChange_survivesRotation() {
        givenLocalAuthorityIsInEngland()

        startActivityWithExtras()
        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        howDoYouFeelRobot.checkNothingSelected()
        howDoYouFeelRobot.clickYesButton()
        waitFor { howDoYouFeelRobot.checkYesSelected() }
        setScreenOrientation(LANDSCAPE)
        waitFor { howDoYouFeelRobot.checkYesSelected() }
        setScreenOrientation(PORTRAIT)
        waitFor { howDoYouFeelRobot.checkYesSelected() }
    }

    @Test
    fun whenLocalAuthorityIsInEnglandAndUserHadMadeSelection_canChangeSelection() {
        givenLocalAuthorityIsInEngland()

        startActivityWithExtras()
        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        howDoYouFeelRobot.checkNothingSelected()
        howDoYouFeelRobot.clickNoButton()
        waitFor { howDoYouFeelRobot.checkNoSelected() }
        howDoYouFeelRobot.clickYesButton()
        waitFor { howDoYouFeelRobot.checkYesSelected() }
    }

    private fun startActivityWithExtras() {
        startTestActivity<HowDoYouFeelActivity> {
            putExtra(
                HowDoYouFeelActivity.SYMPTOMS_DATA_KEY, SymptomsCheckerQuestions(
                    null,
                    null,
                    null
                )
            )
        }
    }
}
