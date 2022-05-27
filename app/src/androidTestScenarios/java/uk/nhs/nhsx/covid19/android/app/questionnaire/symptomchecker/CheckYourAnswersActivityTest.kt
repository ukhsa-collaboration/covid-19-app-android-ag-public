package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.LANDSCAPE
import uk.nhs.nhsx.covid19.android.app.report.config.Orientation.PORTRAIT
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.CheckYourAnswersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.HowDoYouFeelRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomCheckerAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.YourSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setScreenOrientation

class CheckYourAnswersActivityTest() : EspressoTest() {

    private val yourSymptomsRobot = YourSymptomsRobot()
    private val howDoYouFeelRobot = HowDoYouFeelRobot()
    private val checkYourAnswersRobot = CheckYourAnswersRobot()
    private val symptomCheckerAdviceRobot = SymptomCheckerAdviceRobot()

    @Test
    fun startCheckYourAnswers_withAllYesAnswers_verifyAnswersDisplayedAreCorrect() {
        startActivityWithExtras()

        checkYourAnswersRobot.checkActivityIsDisplayed()

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasYesAnswer() }
    }

    @Test
    fun startCheckYourAnswers_withAllNoAnswers_verifyAnswersDisplayedAreCorrect() {
        startActivityWithExtras(nonCardinalSymptomsChecked = false, cardinalSymptomChecked = false, howDoYouFeelChecked = false)

        checkYourAnswersRobot.checkActivityIsDisplayed()

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasNoAnswer() }
    }

    @Test
    fun startCheckYourAnswers_withYesAndNoAnswers_verifyAnswersDisplayedAreCorrect() {
        startActivityWithExtras(nonCardinalSymptomsChecked = true, cardinalSymptomChecked = false, howDoYouFeelChecked = true)

        checkYourAnswersRobot.checkActivityIsDisplayed()

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasYesAnswer() }
    }

    @Test
    fun whenAllYesAnswers_pressChangeYourSymptomsAnswers_changeYourSymptomsAnswersAndReturn() {
        allYesAnswersPressChangeYourSymptomsAnswersChangeNonCardinalAnswerToNoAndPressContinue()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }
        howDoYouFeelRobot.clickContinueButton()

        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasYesAnswer() }
    }

    @Test
    fun whenAllYesAnswers_pressChangeYourSymptomsAnswers_changeYourSymptomsAnswersAndHowYouFeelAnswerAndReturn() {
        allYesAnswersPressChangeYourSymptomsAnswersChangeNonCardinalAnswerToNoAndPressContinue()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        howDoYouFeelRobot.clickNoButton()
        howDoYouFeelRobot.clickContinueButton()

        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasNoAnswer() }
    }

    @Test
    fun whenAllYesAnswers_pressChangeYourSymptomsAnswers_changeYourSymptomsAnswers_goBackOnHowYouFeel_answerIsNotChanged() {
        allYesAnswersPressChangeYourSymptomsAnswersChangeNonCardinalAnswerToNoAndPressContinue()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        waitFor { yourSymptomsRobot.checkYourSymptomsIsDisplayed() }
        waitFor { yourSymptomsRobot.checkNonCardinalNoButtonIsSelected() }

        yourSymptomsRobot.clickContinueButton()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }
        howDoYouFeelRobot.clickContinueButton()

        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasYesAnswer() }
    }

    @Test
    fun whenAllYesAnswers_pressChangeHowYouFeelAnswers_changeYourAnswerAndReturn() {
        startActivityWithExtras()

        checkYourAnswersRobot.checkActivityIsDisplayed()

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasYesAnswer() }

        checkYourAnswersRobot.clickChangeHowYouFeel()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        howDoYouFeelRobot.clickNoButton()
        howDoYouFeelRobot.clickContinueButton()

        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasNoAnswer() }
    }

    @Test
    fun whenAllNoAnswers_pressChangeHowYouFeelAnswers_changeYourAnswerReturn_PressBackChangeAnswerAgainAndReturn() {
        allNoAnswersChangeHowDoYouFeelAndReturn()

        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasYesAnswer() }

        testAppContext.device.pressBack()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        howDoYouFeelRobot.clickNoButton()
        howDoYouFeelRobot.clickContinueButton()

        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasNoAnswer() }
    }

    @Test
    fun whenAllNoAnswers_changeHowDoYouFeelAndReturn_survivesRotation() {
        allNoAnswersChangeHowDoYouFeelAndReturn()

        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasYesAnswer() }

        waitFor { setScreenOrientation(LANDSCAPE) }

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasYesAnswer() }

        waitFor { setScreenOrientation(PORTRAIT) }

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasYesAnswer() }
    }

    @Test
    fun clickContinue_whenShouldContinueWithNormalActivities_navigateAndCheckStateIsDisplayed() {
        startActivityWithExtras(
            nonCardinalSymptomsChecked = false,
            cardinalSymptomChecked = false,
            howDoYouFeelChecked = true
        )
        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }

        checkYourAnswersRobot.clickSubmitAnswers()
        waitFor { symptomCheckerAdviceRobot.checkActivityIsDisplayed() }
        waitFor { symptomCheckerAdviceRobot.checkContinueNormalActivitiesIsDisplayed() }
    }

    @Test
    fun clickContinue_whenShouldTryToStayAtHome_navigateAndCheckStateIsDisplayed() {
        startActivityWithExtras(
            nonCardinalSymptomsChecked = true,
            cardinalSymptomChecked = true,
            howDoYouFeelChecked = false
        )
        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }

        checkYourAnswersRobot.clickSubmitAnswers()
        waitFor { symptomCheckerAdviceRobot.checkActivityIsDisplayed() }
        waitFor { symptomCheckerAdviceRobot.checkTryToStayAtHomeIsDisplayed() }
    }

    private fun allNoAnswersChangeHowDoYouFeelAndReturn() {
        startActivityWithExtras(nonCardinalSymptomsChecked = false, cardinalSymptomChecked = false, howDoYouFeelChecked = false)

        checkYourAnswersRobot.checkActivityIsDisplayed()

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasNoAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasNoAnswer() }

        checkYourAnswersRobot.clickChangeHowYouFeel()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        howDoYouFeelRobot.clickYesButton()
        howDoYouFeelRobot.clickContinueButton()
    }

    private fun allYesAnswersPressChangeYourSymptomsAnswersChangeNonCardinalAnswerToNoAndPressContinue() {
        startActivityWithExtras()

        checkYourAnswersRobot.checkActivityIsDisplayed()

        waitFor { checkYourAnswersRobot.checkNonCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkCardinalSymptomsHasYesAnswer() }
        waitFor { checkYourAnswersRobot.checkHowYouFeelHasYesAnswer() }

        checkYourAnswersRobot.clickChangeYourSymptoms()

        waitFor { yourSymptomsRobot.checkYourSymptomsIsDisplayed() }

        yourSymptomsRobot.clickNonCardinalNoButton()
        yourSymptomsRobot.clickContinueButton()
    }

    private fun startActivityWithExtras(
        nonCardinalSymptomsChecked: Boolean = true,
        cardinalSymptomChecked: Boolean = true,
        howDoYouFeelChecked: Boolean = true
    ) {
        startTestActivity<CheckYourAnswersActivity> {
            putExtra(
                CheckYourAnswersActivity.SYMPTOMS_DATA_KEY, SymptomsCheckerQuestions(
                    nonCardinalSymptoms = NonCardinalSymptoms(
                        title = TranslatableString(mapOf("en-GB" to "Do you have any of these symptoms?")),
                        isChecked = nonCardinalSymptomsChecked,
                        nonCardinalSymptomsText = TranslatableString(
                            mapOf(
                                "en-GB" to "Shivering or chills\n\nA new, continuous cough\n\nA loss or change to your sense of smell or taste\n\nShortness of breath\n\nFeeling tired or exhausted\n\nAn aching body\n\nA headache\n\nA sore throat\n\nA blocked or runny nose\n\nLoss of appetite\n\nDiarrhoea\n\nFeeling sick or being sick"
                            )
                        )
                    ),
                    cardinalSymptom = CardinalSymptom(
                        title = TranslatableString(mapOf("en-GB" to "Do you have a high temperature?")),
                        isChecked = cardinalSymptomChecked,
                    ),
                    howDoYouFeelSymptom = HowDoYouFeelSymptom(isChecked = howDoYouFeelChecked)
                )
            )
        }
    }
}
