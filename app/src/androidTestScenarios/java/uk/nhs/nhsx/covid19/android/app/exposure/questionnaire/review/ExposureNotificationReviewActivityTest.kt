package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.AgeLimitQuestionType.IsAdult
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.ClinicalTrial
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.DoseDate
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationReviewRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.IsolationSetupHelper

class ExposureNotificationReviewActivityTest : EspressoTest(), IsolationSetupHelper {

    private val robot = ExposureNotificationReviewRobot(testAppContext)

    override val isolationHelper = IsolationHelper(testAppContext.clock)

    @Before
    fun setUp() {
        testAppContext.setState(isolationHelper.contact().asIsolation())
    }

    private val adultResponse = OptOutResponseEntry(questionType = IsAdult, response = true)

    private val fullyVaccinatedResponse =
        OptOutResponseEntry(questionType = VaccinationStatusQuestionType.FullyVaccinated, response = true)

    private val withinDoseDateResponse = OptOutResponseEntry(questionType = DoseDate, response = true)
    private val notInClinicalTrialResponse = OptOutResponseEntry(questionType = ClinicalTrial, response = false)
    private val notMedicallyExemptResponse = OptOutResponseEntry(questionType = MedicallyExempt, response = false)
    private val medicallyExemptResponse = OptOutResponseEntry(questionType = MedicallyExempt, response = true)

    @Test
    fun verifyResponseGroupViewState() {
        val reviewData = ReviewData(
            questionnaireOutcome = FullyVaccinated,
            ageResponse = adultResponse,
            vaccinationStatusResponses = listOf(
                fullyVaccinatedResponse,
                withinDoseDateResponse,
                notInClinicalTrialResponse,
                notMedicallyExemptResponse,
                medicallyExemptResponse
            )
        )

        startTestActivity<ExposureNotificationReviewActivity> {
            putExtra("EXTRA_REVIEW_DATA", reviewData)
        }

        robot.checkActivityIsDisplayed()

        runBlocking {
            robot.verifyViewState(
                ageResponse = true,
                vaccinationStatusResponses = reviewData.vaccinationStatusResponses,
                ageLimitDate = testAppContext.getAgeLimitBeforeEncounter()!!,
                lastDoseDateLimit = testAppContext.getLastDoseDateLimit()!!
            )
        }
    }

    @Test
    fun verifyVaccinationStatusGroupNotShownWhenListOfResponsesIsEmpty() {
        val reviewData = ReviewData(
            questionnaireOutcome = FullyVaccinated,
            ageResponse = adultResponse,
            vaccinationStatusResponses = listOf()
        )

        startTestActivity<ExposureNotificationReviewActivity> {
            putExtra("EXTRA_REVIEW_DATA", reviewData)
        }

        robot.checkActivityIsDisplayed()

        runBlocking {
            robot.verifyViewState(
                ageResponse = true,
                vaccinationStatusResponses = reviewData.vaccinationStatusResponses,
                ageLimitDate = testAppContext.getAgeLimitBeforeEncounter()!!,
                lastDoseDateLimit = testAppContext.getLastDoseDateLimit()!!
            )
        }
    }
}
