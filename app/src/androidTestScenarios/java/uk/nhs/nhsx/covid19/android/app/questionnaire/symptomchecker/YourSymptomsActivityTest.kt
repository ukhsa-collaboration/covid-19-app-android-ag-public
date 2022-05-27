package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.YourSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation

class YourSymptomsActivityTest() : EspressoTest() {

    private val yourSymptomsRobot = YourSymptomsRobot()

    @Test
    fun startYourSymptoms_fetchingDelayed_showSpinner() {
        MockApiModule.behaviour.delayMillis = 5000
        startTestActivity<YourSymptomsActivity>()

        waitFor { yourSymptomsRobot.checkActivityIsDisplayed() }
        waitFor { yourSymptomsRobot.checkLoadingSpinnerIsDisplayed() }
    }

    @Test
    fun startYourSymptoms_loadingQuestionnaireFails_showsErrorState() {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL

        startTestActivity<YourSymptomsActivity>()

        yourSymptomsRobot.checkErrorStateIsDisplayed()
    }

    @Test
    fun startYourSymptoms_loadingQuestionnaireFails_showsErrorState_clickRetry() {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL

        startTestActivity<YourSymptomsActivity>()

        yourSymptomsRobot.checkErrorStateIsDisplayed()

        MockApiModule.behaviour.responseType = ALWAYS_SUCCEED

        yourSymptomsRobot.clickTryAgainButton()

        waitFor { yourSymptomsRobot.checkYourSymptomsIsDisplayed() }
    }

    @Test
    fun whenUserClicksContinue_noQuestionsAnswered_showError() {
        startTestActivity<YourSymptomsActivity>()

        waitFor { yourSymptomsRobot.checkYourSymptomsIsDisplayed() }

        yourSymptomsRobot.checkErrorVisible(false)
        waitFor { yourSymptomsRobot.checkNothingSelected() }
        yourSymptomsRobot.clickContinueButton()

        waitFor { yourSymptomsRobot.checkErrorVisible(true) }
    }

    @Test
    fun whenUserClicksContinue_onlyNonCardinalAnswered_showError() {
        startTestActivity<YourSymptomsActivity>()

        waitFor { yourSymptomsRobot.checkYourSymptomsIsDisplayed() }

        yourSymptomsRobot.checkErrorVisible(false)
        waitFor { yourSymptomsRobot.checkNothingSelected() }
        yourSymptomsRobot.clickNonCardinalYesButton()
        yourSymptomsRobot.clickContinueButton()

        waitFor { yourSymptomsRobot.checkErrorVisible(true) }
    }

    @Test
    fun whenUserClicksContinue_onlyCardinalAnswered_showError() {
        startTestActivity<YourSymptomsActivity>()

        waitFor { yourSymptomsRobot.checkYourSymptomsIsDisplayed() }

        yourSymptomsRobot.checkErrorVisible(false)
        waitFor { yourSymptomsRobot.checkNothingSelected() }
        yourSymptomsRobot.clickCardinalYesButton()
        yourSymptomsRobot.clickContinueButton()

        waitFor { yourSymptomsRobot.checkErrorVisible(true) }
    }

    @Test
    fun whenUserClickSContinue_allAnswersGiven_showNoError() {
        startTestActivity<YourSymptomsActivity>()

        waitFor { yourSymptomsRobot.checkYourSymptomsIsDisplayed() }

        yourSymptomsRobot.clickCardinalYesButton()
        yourSymptomsRobot.clickNonCardinalYesButton()
        yourSymptomsRobot.clickContinueButton()

        waitFor { yourSymptomsRobot.checkErrorVisible(false) }
    }

    @Test
    fun whenUserHasMadeSelection_canChangeSelection() {
        startTestActivity<YourSymptomsActivity>()

        waitFor { yourSymptomsRobot.checkYourSymptomsIsDisplayed() }

        waitFor { yourSymptomsRobot.checkNothingSelected() }

        yourSymptomsRobot.clickNonCardinalYesButton()
        waitFor { yourSymptomsRobot.checkNonCardinalYesButtonIsSelected() }
        yourSymptomsRobot.clickCardinalNoButton()
        waitFor { yourSymptomsRobot.checkCardinalNoButtonIsSelected() }

        yourSymptomsRobot.clickNonCardinalNoButton()
        waitFor { yourSymptomsRobot.checkNonCardinalNoButtonIsSelected() }
        yourSymptomsRobot.clickCardinalYesButton()
        waitFor { yourSymptomsRobot.checkCardinalYesButtonIsSelected() }
    }

    @Test
    fun whenUserIsReturningToPage_showPreviousSelections() {
        startActivityWithExtras()

        waitFor { yourSymptomsRobot.checkYourSymptomsIsDisplayed() }

        waitFor { yourSymptomsRobot.checkNonCardinalYesButtonIsSelected() }
        waitFor { yourSymptomsRobot.checkCardinalYesButtonIsSelected() }
    }

    @Test
    fun whenUserIsReturningToPage_showPreviousSelections_canChangeAnswer_survivesRotation() {
        startActivityWithExtras()

        waitFor { yourSymptomsRobot.checkYourSymptomsIsDisplayed() }

        waitFor { yourSymptomsRobot.checkNonCardinalYesButtonIsSelected() }
        waitFor { yourSymptomsRobot.checkCardinalYesButtonIsSelected() }

        yourSymptomsRobot.clickCardinalNoButton()
        yourSymptomsRobot.clickNonCardinalNoButton()
        waitFor { yourSymptomsRobot.checkNonCardinalNoButtonIsSelected() }
        waitFor { yourSymptomsRobot.checkCardinalNoButtonIsSelected() }

        setScreenOrientation(LANDSCAPE)
        waitFor { yourSymptomsRobot.checkNonCardinalNoButtonIsSelected() }
        waitFor { yourSymptomsRobot.checkCardinalNoButtonIsSelected() }

        setScreenOrientation(PORTRAIT)
        waitFor { yourSymptomsRobot.checkNonCardinalNoButtonIsSelected() }
        waitFor { yourSymptomsRobot.checkCardinalNoButtonIsSelected() }
    }

    private fun startActivityWithExtras() {
        startTestActivity<YourSymptomsActivity> {
            putExtra(
                YourSymptomsActivity.SYMPTOMS_DATA_KEY, SymptomsCheckerQuestions(
                    nonCardinalSymptoms = NonCardinalSymptoms(
                        title = TranslatableString(
                            mapOf(
                                "en-GB" to "Test placeholder text"
                            )
                        ),
                        isChecked = true,
                        nonCardinalSymptomsText = TranslatableString(mapOf("en-GB" to "Test placeholder text"))
                    ),
                    cardinalSymptom = CardinalSymptom(
                        title = TranslatableString(mapOf("en-GB" to "Test placeholder text")),
                        isChecked = true
                    ),
                    howDoYouFeelSymptom = HowDoYouFeelSymptom(true)
                )
            )
        }
    }
}
