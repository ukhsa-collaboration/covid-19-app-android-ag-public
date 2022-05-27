package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.FLOW
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.CheckYourAnswersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.HowDoYouFeelRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomCheckerAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.YourSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper

@RunWith(Parameterized::class)
class SymptomCheckerScenarioTest(override val configuration: TestConfiguration) : EspressoTest(),
    LocalAuthoritySetupHelper {
    private val statusRobot = StatusRobot()
    private val yourSymptomsRobot = YourSymptomsRobot()
    private val howDoYouFeelRobot = HowDoYouFeelRobot()
    private val checkYourAnswersRobot = CheckYourAnswersRobot()
    private val symptomCheckerAdviceRobot = SymptomCheckerAdviceRobot()

    private val symptomCheckerScenario = "Symptom Checker England"

    @Before
    fun setUp() {
        givenLocalAuthorityIsInEngland()
    }

    @Test
    @Reported
    fun userSelectsPositiveSymptoms_navigatesToTryToStayAtHomeScreen() = reporter(
        scenario = symptomCheckerScenario,
        title = "User selects positive symptoms",
        description = "User selects positive symptoms - is presented with stay at home screen",
        kind = FLOW
    ) {
        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        step(
            stepName = "Home screen - Default state",
            stepDescription = "When the user is on the Home screen they can tap 'Report symptoms'"
        )

        statusRobot.clickReportSymptoms()

        waitFor { yourSymptomsRobot.checkActivityIsDisplayed() }
        yourSymptomsRobot.checkNothingSelected()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented a list of cardinal- and non-cardinal symptoms"
        )

        yourSymptomsRobot.clickCardinalYesButton()
        yourSymptomsRobot.clickNonCardinalYesButton()
        yourSymptomsRobot.checkCardinalYesButtonIsSelected()
        yourSymptomsRobot.checkNonCardinalYesButtonIsSelected()

        step(
            stepName = "Symptom selected",
            stepDescription = "The user selects both cardinal- and non-cardinal symptoms and confirms the screen"
        )

        yourSymptomsRobot.clickContinueButton()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        howDoYouFeelRobot.checkNothingSelected()

        step(
            stepName = "How do you feel",
            stepDescription = "The user is asked how they are feeling"
        )

        howDoYouFeelRobot.clickNoButton()
        howDoYouFeelRobot.checkNoSelected()

        step(
            stepName = "How do you feel selected",
            stepDescription = "The user selects that they are not feeling well and confirms the screen"
        )

        howDoYouFeelRobot.clickContinueButton()

        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }
        checkYourAnswersRobot.checkCardinalSymptomsHasYesAnswer()
        checkYourAnswersRobot.checkNonCardinalSymptomsHasYesAnswer()
        checkYourAnswersRobot.checkHowYouFeelHasNoAnswer()

        step(
            stepName = "Check your answers",
            stepDescription = "The user is asked to review their answers and confirms the screen"
        )

        checkYourAnswersRobot.clickSubmitAnswers()

        waitFor { symptomCheckerAdviceRobot.checkActivityIsDisplayed() }
        symptomCheckerAdviceRobot.checkTryToStayAtHomeIsDisplayed()

        step(
            stepName = "Try to stay at home",
            stepDescription = "The user is advised to stay at home and confirms the screen"
        )

        symptomCheckerAdviceRobot.clickBackToHomeButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    @Reported
    fun userSelectsNegativeSymptoms_navigatesToContinueNormalActivitiesScreen() = reporter(
        scenario = symptomCheckerScenario,
        title = "User selects negative symptoms",
        description = "User selects negative symptoms - is presented with continue normal activities screen",
        kind = FLOW
    ) {
        startTestActivity<StatusActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        step(
            stepName = "Home screen - Default state",
            stepDescription = "When the user is on the Home screen they can tap 'Report symptoms'"
        )

        statusRobot.clickReportSymptoms()

        waitFor { yourSymptomsRobot.checkActivityIsDisplayed() }
        yourSymptomsRobot.checkNothingSelected()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented a list of cardinal- and non-cardinal symptoms"
        )

        yourSymptomsRobot.clickCardinalNoButton()
        yourSymptomsRobot.clickNonCardinalNoButton()
        yourSymptomsRobot.checkCardinalNoButtonIsSelected()
        yourSymptomsRobot.checkNonCardinalNoButtonIsSelected()

        step(
            stepName = "Symptom selected",
            stepDescription = "The user selects neither cardinal- or non-cardinal symptoms and confirms the screen"
        )

        yourSymptomsRobot.clickContinueButton()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        howDoYouFeelRobot.checkNothingSelected()

        step(
            stepName = "How do you feel",
            stepDescription = "The user is asked how they are feeling"
        )

        howDoYouFeelRobot.clickYesButton()
        howDoYouFeelRobot.checkYesSelected()

        step(
            stepName = "How do you feel selected",
            stepDescription = "The user selects that they are feeling well and confirms the screen"
        )

        howDoYouFeelRobot.clickContinueButton()

        waitFor { checkYourAnswersRobot.checkActivityIsDisplayed() }
        checkYourAnswersRobot.checkCardinalSymptomsHasNoAnswer()
        checkYourAnswersRobot.checkCardinalSymptomsHasNoAnswer()
        checkYourAnswersRobot.checkHowYouFeelHasYesAnswer()

        step(
            stepName = "Check your answers",
            stepDescription = "The user is asked to review their answers and confirms the screen"
        )

        checkYourAnswersRobot.clickSubmitAnswers()

        waitFor { symptomCheckerAdviceRobot.checkActivityIsDisplayed() }
        symptomCheckerAdviceRobot.checkContinueNormalActivitiesIsDisplayed()

        step(
            stepName = "Continue normal activities",
            stepDescription = "The user is advised to continue with normal activities and confirms the screen"
        )

        symptomCheckerAdviceRobot.clickBackToHomeButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    @Reported
    fun userDoNotSelectNonCardinalSymptoms_showErrorPanel() = reporter(
        scenario = symptomCheckerScenario,
        title = "Continue no non-cardinal symptoms checked",
        description = "User attempts to continue without selecting any non-cardinal symptoms",
        kind = SCREEN
    ) {
        startTestActivity<YourSymptomsActivity>()

        waitFor { yourSymptomsRobot.checkActivityIsDisplayed() }

        yourSymptomsRobot.checkNothingSelected()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented a list of cardinal- and non-cardinal symptoms"
        )

        yourSymptomsRobot.clickCardinalYesButton()
        yourSymptomsRobot.clickCardinalNoButton()

        yourSymptomsRobot.clickContinueButton()

        yourSymptomsRobot.checkErrorVisible(true)

        step(
            stepName = "No non-cardinal symptoms selected",
            stepDescription = "An error message is shown to the user"
        )
    }

    @Test
    @Reported
    fun userDoNotSelectCardinalSymptoms_showErrorPanel() = reporter(
        scenario = symptomCheckerScenario,
        title = "Continue no cardinal symptoms checked",
        description = "User attempts to continue without selecting any cardinal symptoms",
        kind = SCREEN
    ) {
        startTestActivity<YourSymptomsActivity>()

        waitFor { yourSymptomsRobot.checkActivityIsDisplayed() }

        yourSymptomsRobot.checkNothingSelected()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented a list of cardinal- and non-cardinal symptoms"
        )

        yourSymptomsRobot.clickNonCardinalNoButton()
        yourSymptomsRobot.checkNonCardinalNoButtonIsSelected()

        yourSymptomsRobot.clickContinueButton()

        yourSymptomsRobot.checkErrorVisible(true)

        step(
            stepName = "No cardinal symptoms selected",
            stepDescription = "An error message is shown to the user"
        )
    }

    @Test
    @Reported
    fun userDoNotSelectAnySymptoms_showErrorPanel() = reporter(
        scenario = symptomCheckerScenario,
        title = "Continue no symptoms checked",
        description = "User attempts to continue without selecting any cardinal- or non-cardinal symptoms",
        kind = SCREEN
    ) {
        startTestActivity<YourSymptomsActivity>()

        waitFor { yourSymptomsRobot.checkActivityIsDisplayed() }

        yourSymptomsRobot.checkNothingSelected()

        step(
            stepName = "Symptom list",
            stepDescription = "The user is presented a list of cardinal- and non-cardinal symptoms"
        )

        yourSymptomsRobot.clickContinueButton()

        yourSymptomsRobot.checkErrorVisible(true)

        step(
            stepName = "No cardinal symptoms selected",
            stepDescription = "An error message is shown to the user"
        )
    }

    @Test
    @Reported
    fun userDoNotSelectHowDoYouFeel_showErrorPanel() = reporter(
        scenario = symptomCheckerScenario,
        title = "Continue how do you feel not checked",
        description = "User attempts to continue without selecting how they are feeling",
        kind = SCREEN
    ) {
        startTestActivity<YourSymptomsActivity>()
        yourSymptomsRobot.clickNonCardinalNoButton()
        yourSymptomsRobot.clickCardinalNoButton()

        yourSymptomsRobot.clickContinueButton()

        waitFor { howDoYouFeelRobot.checkActivityIsDisplayed() }

        howDoYouFeelRobot.checkNothingSelected()

        step(
            stepName = "How do you feel",
            stepDescription = "The user is asked how they are feeling"
        )

        howDoYouFeelRobot.clickContinueButton()

        howDoYouFeelRobot.checkErrorVisible(true)

        step(
            stepName = "How do you feel not selected",
            stepDescription = "An error message is shown to the user"
        )
    }
}
