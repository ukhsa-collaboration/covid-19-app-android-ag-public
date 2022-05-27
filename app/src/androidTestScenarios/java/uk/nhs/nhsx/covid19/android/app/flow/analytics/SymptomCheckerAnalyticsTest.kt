package uk.nhs.nhsx.covid19.android.app.flow.analytics

import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CardinalSymptom
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.HowDoYouFeelSymptom
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.NonCardinalSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceActivity
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.TRY_TO_STAY_AT_HOME
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomsCheckerQuestions
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.CheckYourAnswersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomCheckerAdviceRobot

class SymptomCheckerAnalyticsTest : AnalyticsTest() {
    private val checkAnswersRobot = CheckYourAnswersRobot()
    private val adviceRobot = SymptomCheckerAdviceRobot()

    @Test
    fun tryToStayAtHomeResultPageTriggered() = runBlocking {
        givenLocalAuthorityIsInEngland()

        // Current date: 1st Jan
        // Starting state - app running normally
        runBackgroundTasks()

        // Current date: 2nd Jan - Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()

        // Trigger try to stay at home on 2nd Jan
        startTestActivity<SymptomCheckerAdviceActivity> {
            putExtra(SymptomCheckerAdviceActivity.VALUE_KEY_RESULT, TRY_TO_STAY_AT_HOME)
            putExtra(SymptomCheckerAdviceActivity.VALUE_KEY_QUESTIONS, questions)
        }

        waitFor { adviceRobot.checkActivityIsDisplayed() }
        waitFor { adviceRobot.checkTryToStayAtHomeIsDisplayed() }

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            assertEquals(1, Metrics::completedV2SymptomsQuestionnaireAndStayAtHome)
            assertPresent(Metrics::hasCompletedV2SymptomsQuestionnaireAndStayAtHomeBackgroundTick)
        }

        // Keep analytics package for 14 days
        assertOnFieldsForDateRange(4..16) {
            assertPresent(Metrics::hasCompletedV2SymptomsQuestionnaireAndStayAtHomeBackgroundTick)
        }

        // Current date: 17th, background tick not present after two weeks (analytics date 16th)
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun checkYourAnswersCompletedContinueNormalActivities() = runBlocking {
        givenLocalAuthorityIsInEngland()

        // Current date: 1st Jan
        // Starting state - app running normally
        runBackgroundTasks()

        // Current date: 2nd Jan - Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()

        startTestActivity<CheckYourAnswersActivity> {
            putExtra(
                CheckYourAnswersActivity.SYMPTOMS_DATA_KEY, SymptomsCheckerQuestions(
                    nonCardinalSymptoms = NonCardinalSymptoms(
                        title = TranslatableString(mapOf("en-GB" to "Do you have any of these symptoms?")),
                        isChecked = false,
                        nonCardinalSymptomsText = TranslatableString(
                            mapOf(
                                "en-GB" to "Shivering or chills"
                            )
                        )
                    ),
                    cardinalSymptom = CardinalSymptom(
                        title = TranslatableString(mapOf("en-GB" to "Do you have a high temperature?")),
                        isChecked = false,
                    ),
                    howDoYouFeelSymptom = HowDoYouFeelSymptom(isChecked = true)
                )
            )
        }

        // Complete questionnaire on 2nd Jan
        checkAnswersRobot.clickSubmitAnswers()

        waitFor { adviceRobot.checkActivityIsDisplayed() }
        adviceRobot.checkContinueNormalActivitiesIsDisplayed()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            assertEquals(1, Metrics::completedV2SymptomsQuestionnaire)
            assertPresent(Metrics::hasCompletedV2SymptomsQuestionnaireBackgroundTick)
        }

        // Keep analytics package for 14 days - Dates 3nd-15th Jan.
        assertOnFieldsForDateRange(dateRange = 4..16) {
            assertPresent(Metrics::hasCompletedV2SymptomsQuestionnaireBackgroundTick)
        }

        // Current date: 17th, background tick not present after two weeks (analytics date 16th)
        assertAnalyticsPacketIsNormal()
    }

    @Test
    fun checkYourAnswersCompletedTryToStayAtHome() = runBlocking {
        givenLocalAuthorityIsInEngland()

        // Current date: 1st Jan
        // Starting state - app running normally
        runBackgroundTasks()

        // Current date: 2nd Jan - Analytics packet for: 1st Jan
        assertAnalyticsPacketIsNormal()

        startTestActivity<CheckYourAnswersActivity> {
            putExtra(
                CheckYourAnswersActivity.SYMPTOMS_DATA_KEY, SymptomsCheckerQuestions(
                    nonCardinalSymptoms = NonCardinalSymptoms(
                        title = TranslatableString(mapOf("en-GB" to "Do you have any of these symptoms?")),
                        isChecked = true,
                        nonCardinalSymptomsText = TranslatableString(
                            mapOf(
                                "en-GB" to "Shivering or chills"
                            )
                        )
                    ),
                    cardinalSymptom = CardinalSymptom(
                        title = TranslatableString(mapOf("en-GB" to "Do you have a high temperature?")),
                        isChecked = true,
                    ),
                    howDoYouFeelSymptom = HowDoYouFeelSymptom(isChecked = true)
                )
            )
        }

        // Complete questionnaire on 2nd Jan
        checkAnswersRobot.clickSubmitAnswers()

        waitFor { adviceRobot.checkActivityIsDisplayed() }
        adviceRobot.checkTryToStayAtHomeIsDisplayed()

        // Current date: 3rd Jan -> Analytics packet for: 2nd Jan
        assertOnFields {
            assertEquals(1, Metrics::completedV2SymptomsQuestionnaire)
            assertEquals(1, Metrics::completedV2SymptomsQuestionnaireAndStayAtHome)
            assertPresent(Metrics::hasCompletedV2SymptomsQuestionnaireBackgroundTick)
            assertPresent(Metrics::hasCompletedV2SymptomsQuestionnaireAndStayAtHomeBackgroundTick)
        }

        // Keep analytics package for 14 days - Dates 3nd-15th Jan.
        assertOnFieldsForDateRange(dateRange = 4..16) {
            assertPresent(Metrics::hasCompletedV2SymptomsQuestionnaireBackgroundTick)
            assertPresent(Metrics::hasCompletedV2SymptomsQuestionnaireAndStayAtHomeBackgroundTick)
        }

        // Current date: 17th, background tick not present after two weeks (analytics date 16th)
        assertAnalyticsPacketIsNormal()
    }

    private val questions = SymptomsCheckerQuestions(
        null,
        null,
        null
    )
}
