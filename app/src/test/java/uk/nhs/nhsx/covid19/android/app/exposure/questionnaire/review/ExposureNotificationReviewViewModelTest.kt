package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.called
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.AcknowledgeRiskyContact
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.GetAgeLimitBeforeEncounter
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.GetLastDoseDateLimit
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.NavigationTarget.AgeLimit
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.NavigationTarget.IsolationAdvice
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.NavigationTarget.VaccinationStatus
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ExposureNotificationReviewViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.Minor
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome.NotExempt
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.QUESTIONNAIRE
import java.time.LocalDate

class ExposureNotificationReviewViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val getAgeLimitBeforeEncounter = mockk<GetAgeLimitBeforeEncounter>()
    private val getLastDoseDateLimit = mockk<GetLastDoseDateLimit>()
    private val optOutOfContactIsolation = mockk<OptOutOfContactIsolation>(relaxUnitFun = true)
    private val acknowledgeRiskyContact = mockk<AcknowledgeRiskyContact>(relaxUnitFun = true)

    private val navigationObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)

    private val defaultReviewData = mockk<ReviewData>()

    private val expectedQuestionnaireOutcome = MedicallyExempt
    private val expectedAgeLimitBeforeEncounterDate = LocalDate.parse("2021-10-10")
    private val expectedLastDoseDateLimit = LocalDate.parse("2021-10-18")

    @Before
    fun setUp() {
        every { defaultReviewData.questionnaireOutcome } returns expectedQuestionnaireOutcome
        every { defaultReviewData.ageResponse } returns mockk()
        every { defaultReviewData.vaccinationStatusResponses } returns mockk()
        coEvery { getAgeLimitBeforeEncounter.invoke() } returns expectedAgeLimitBeforeEncounterDate
        every { getLastDoseDateLimit.invoke() } returns expectedLastDoseDateLimit
    }

    @Test
    fun `on ExposureNotificationReviewViewModel initialization, emit view state`() {
        createTestSubject(defaultReviewData)

        val expectedViewState = ViewState(
            ageLimitResponse = defaultReviewData.ageResponse,
            vaccinationStatusResponse = defaultReviewData.vaccinationStatusResponses,
            ageLimitDate = expectedAgeLimitBeforeEncounterDate,
            lastDoseDateLimit = expectedLastDoseDateLimit
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when submit clicked as MedicallyExempt, then opt-out of contact isolation, acknowledge risky contact and emit IsolationAdvice navigation event`() {
        every { defaultReviewData.questionnaireOutcome } returns MedicallyExempt
        verifySubmitAsExempt(expectedOutcome = MedicallyExempt)
    }

    @Test
    fun `when submit clicked as Minor, then opt-out of contact isolation, acknowledge risky contact and emit IsolationAdvice navigation event`() {
        every { defaultReviewData.questionnaireOutcome } returns Minor
        verifySubmitAsExempt(expectedOutcome = Minor)
    }

    @Test
    fun `when submit clicked as FullyVaccinated, then opt-out of contact isolation, acknowledge risky contact and emit IsolationAdvice navigation event`() {
        every { defaultReviewData.questionnaireOutcome } returns FullyVaccinated
        verifySubmitAsExempt(expectedOutcome = FullyVaccinated)
    }

    @Test
    fun `when submit clicked as not exempt from isolation, then acknowledge risky contact and emit IsolationAdvice navigation event`() {
        val expectedOutcome = NotExempt
        every { defaultReviewData.questionnaireOutcome } returns expectedOutcome

        val testSubject = createTestSubject(defaultReviewData)

        testSubject.onSubmitClicked()

        verify { optOutOfContactIsolation wasNot called }
        verifyOrder {
            acknowledgeRiskyContact()
            navigationObserver.onChanged(IsolationAdvice(questionnaireOutcome = expectedOutcome))
        }
    }

    @Test
    fun `when change age limit response clicked, then emit AgeLimit navigation event`() {
        val testSubject = createTestSubject(defaultReviewData)

        testSubject.onChangeAgeLimitResponseClicked()

        verify { navigationObserver.onChanged(AgeLimit) }
    }

    @Test
    fun `when change vaccination status response clicked, then emit VaccinationStatus navigation event`() {
        val testSubject = createTestSubject(defaultReviewData)

        testSubject.onChangeVaccinationStatusResponseClicked()

        verify { navigationObserver.onChanged(VaccinationStatus) }
    }

    private fun verifySubmitAsExempt(expectedOutcome: QuestionnaireOutcome) {
        val testSubject = createTestSubject(defaultReviewData)

        testSubject.onSubmitClicked()

        verifyOrder {
            optOutOfContactIsolation(reason = QUESTIONNAIRE)
            acknowledgeRiskyContact()
            navigationObserver.onChanged(IsolationAdvice(questionnaireOutcome = expectedOutcome))
        }
    }

    private fun createTestSubject(reviewData: ReviewData): ExposureNotificationReviewViewModel {
        val testSubject = ExposureNotificationReviewViewModel(
            getAgeLimitBeforeEncounter,
            getLastDoseDateLimit,
            optOutOfContactIsolation,
            acknowledgeRiskyContact,
            reviewData
        )

        testSubject.navigationTarget.observeForever(navigationObserver)
        testSubject.viewState.observeForever(viewStateObserver)

        return testSubject
    }
}
