package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Finish
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.NavigationTarget.Review
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.Question
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.ExposureNotificationVaccinationStatusViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.OptOutResponseEntry
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.AgeLimitQuestionType.IsAdult
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.ClinicalTrial
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.DoseDate
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.FullyVaccinated
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionType.VaccinationStatusQuestionType.MedicallyExempt
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.QuestionnaireOutcome
import uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review.ReviewData
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2 as NO

class ExposureNotificationVaccinationStatusViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mockGetLastDoseDateLimit: GetLastDoseDateLimit = mockk(relaxed = true)
    private val mockLocalAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider = mockk(relaxUnitFun = true)

    private val testSubject: ExposureNotificationVaccinationStatusViewModel by lazy { setUpTestSubject() }

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private val fixedClock = Clock.fixed(Instant.parse("2021-10-10T00:00:00.00Z"), ZoneOffset.UTC)
    private val expectedLastDoseDateLimit = LocalDate.now(fixedClock)

    private val isolationStateMachine: IsolationStateMachine = mockk(relaxUnitFun = true)
    private val isolationLogicalState: IsolationLogicalState = mockk(relaxUnitFun = true)

    @Before
    fun setUp() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns null
        every { mockGetLastDoseDateLimit() } returns expectedLastDoseDateLimit
        every { isolationStateMachine.readLogicalState() } returns isolationLogicalState
        every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns false
    }

    @Test
    fun `when all doses option selected, show error set to false`() {
        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onClickContinue()

        val expectedViewState = ViewState(
            questions = listOf(Question(FullyVaccinated, YES), Question(DoseDate, null)),
            showError = false,
            date = expectedLastDoseDateLimit,
            showSubtitle = true
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when no option selected, on click continue sets show error to true`() {
        testSubject.onClickContinue()

        val expectedViewState = ViewState(
            questions = listOf(Question(FullyVaccinated, null)),
            showError = true,
            date = expectedLastDoseDateLimit,
            showSubtitle = true
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when all doses option selected yes, but no date, on click continue sets show error to true`() {
        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onClickContinue()

        val expectedViewState = ViewState(
            questions = listOf(
                Question(FullyVaccinated, YES),
                Question(DoseDate, null)
            ),
            showError = true,
            date = expectedLastDoseDateLimit,
            showSubtitle = true
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when all doses option selected yes, date option selected no, clinical trial option no response, on click continue sets show error to true`() {
        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(NO)
        testSubject.onClickContinue()

        val expectedViewState = ViewState(
            questions = listOf(
                Question(FullyVaccinated, YES),
                Question(DoseDate, NO),
                Question(ClinicalTrial, null)
            ),
            showError = true,
            date = expectedLastDoseDateLimit,
            showSubtitle = true
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when in England, all doses selected yes, date selected no, clinical trial selected no, medically exempt no response, on click continue sets show error to true`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(NO)
        testSubject.onClickContinue()

        val expectedViewState = ViewState(
            questions = listOf(
                Question(FullyVaccinated, YES),
                Question(DoseDate, NO),
                Question(ClinicalTrial, NO),
                Question(MedicallyExempt, null)
            ),
            showError = true,
            date = expectedLastDoseDateLimit,
            showSubtitle = true
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when in Wales, all doses option selected no, clinical trial option no response, on click continue sets show error to true`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        testSubject.onFullyVaccinatedOptionChanged(NO)
        testSubject.onClickContinue()

        val expectedViewState = ViewState(
            questions = listOf(
                Question(FullyVaccinated, NO),
                Question(ClinicalTrial, null)
            ),
            showError = true,
            date = expectedLastDoseDateLimit,
            showSubtitle = true
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when in England, when all doses option selected no, medically exempt selected no, clinical trial option no response, on click continue sets show error to true`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(NO)
        testSubject.onMedicallyExemptOptionChanged(NO)
        testSubject.onClickContinue()

        val expectedViewState = ViewState(
            questions = listOf(
                Question(FullyVaccinated, NO),
                Question(MedicallyExempt, NO),
                Question(ClinicalTrial, null)
            ),
            showError = true,
            date = expectedLastDoseDateLimit,
            showSubtitle = true
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when in England, all doses option selected no, medically exempt option no response, on click continue sets show error to true`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(NO)
        testSubject.onClickContinue()

        val expectedViewState = ViewState(
            questions = listOf(Question(FullyVaccinated, NO), Question(MedicallyExempt, null)),
            showError = true,
            date = expectedLastDoseDateLimit,
            showSubtitle = true
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when all doses option selected yes, date option selected yes, on click continue navigates to review`() {
        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(YES)
        testSubject.onClickContinue()

        val expected = listOf(
            OptOutResponseEntry(FullyVaccinated, true),
            OptOutResponseEntry(DoseDate, true)
        )

        verify {
            navigateObserver.onChanged(
                createNavigationState(
                    questionnaireOutcome = QuestionnaireOutcome.FullyVaccinated,
                    vaccinationStatusResponse = expected
                )
            )
        }
    }

    @Test
    fun `when all doses option selected yes, date option selected no, clinical trial option selected yes, on click continue navigates to review`() {
        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(YES)
        testSubject.onClickContinue()

        val expected = listOf(
            OptOutResponseEntry(FullyVaccinated, true),
            OptOutResponseEntry(DoseDate, false),
            OptOutResponseEntry(ClinicalTrial, true)
        )

        verify {
            navigateObserver.onChanged(
                createNavigationState(
                    questionnaireOutcome = QuestionnaireOutcome.FullyVaccinated,
                    vaccinationStatusResponse = expected
                )
            )
        }
    }

    @Test
    fun `when in England, when date option selected no and clinical option selected no, fully vaccinated option changed to no should clear date and clinical trial option`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(NO)
        testSubject.onFullyVaccinatedOptionChanged(NO)

        val expected = listOf(Question(FullyVaccinated, NO), Question(MedicallyExempt, null))

        verify { viewStateObserver.onChanged(createViewState(questions = expected)) }
    }

    @Test
    fun `when in England, when date option selected no and clinical option selected no, date option changed to yes should clear clinical trial and medically exempt option`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(NO)
        testSubject.onLastDoseDateOptionChanged(YES)

        val questionsAfterDoseDateYes = listOf(Question(FullyVaccinated, YES), Question(DoseDate, YES))

        verify { viewStateObserver.onChanged(createViewState(questions = questionsAfterDoseDateYes)) }
    }

    @Test
    fun `when in England, when date selected no, clinical selected no, medically exempt selected yes, clinical trial option changed to yes should clear medically exempt option`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(NO)
        testSubject.onMedicallyExemptOptionChanged(YES)
        testSubject.onClinicalTrialOptionChanged(YES)

        val expected = listOf(Question(FullyVaccinated, YES), Question(DoseDate, NO), Question(ClinicalTrial, YES))

        verify { viewStateObserver.onChanged(createViewState(questions = expected)) }
    }

    @Test
    fun `when in Wales, dose date selected no, clinical selected no, dose date option changed to yes should clear clinical trial option`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(NO)
        testSubject.onLastDoseDateOptionChanged(YES)

        val expected = listOf(Question(FullyVaccinated, YES), Question(DoseDate, YES))

        verify { viewStateObserver.onChanged(createViewState(questions = expected)) }
    }

    @Test
    fun `when in England, when fully vaccinated selected no, medically exempt selected no, fully vaccinated option changed to yes should clear medically exempt option`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(NO)
        testSubject.onMedicallyExemptOptionChanged(NO)
        testSubject.onFullyVaccinatedOptionChanged(YES)

        val expected = listOf(Question(FullyVaccinated, YES), Question(DoseDate, null))

        verify { viewStateObserver.onChanged(createViewState(questions = expected)) }
    }

    @Test
    fun `when in England, when fully vaccinated selected no, medically exempt selected no, clinical trial yes, medically exempt changed to yes should clear clinical trial option`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(NO)
        testSubject.onMedicallyExemptOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(YES)
        testSubject.onMedicallyExemptOptionChanged(YES)

        val expected = listOf(Question(FullyVaccinated, NO), Question(MedicallyExempt, YES))

        verify { viewStateObserver.onChanged(createViewState(questions = expected)) }
    }

    @Test
    fun `when in Wales, all doses option selected yes, date option selected no, clinical trial option selected no, on click continue navigates to review`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(NO)
        testSubject.onClickContinue()

        val expected = listOf(
            OptOutResponseEntry(FullyVaccinated, true),
            OptOutResponseEntry(DoseDate, false),
            OptOutResponseEntry(ClinicalTrial, false)
        )

        verify {
            navigateObserver.onChanged(createNavigationState(QuestionnaireOutcome.NotExempt, expected))
        }
    }

    @Test
    fun `when in Wales, all doses option selected no, clinical trial option selected no, on click continue navigates to review`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        testSubject.onFullyVaccinatedOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(NO)
        testSubject.onClickContinue()

        val expected = listOf(
            OptOutResponseEntry(FullyVaccinated, false),
            OptOutResponseEntry(ClinicalTrial, false)
        )

        verify {
            navigateObserver.onChanged(createNavigationState(QuestionnaireOutcome.NotExempt, expected))
        }
    }

    @Test
    fun `when in Wales, all doses option selected no, clinical trial option selected yes, on click continue navigates to review`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        testSubject.onFullyVaccinatedOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(YES)
        testSubject.onClickContinue()

        val expected = listOf(
            OptOutResponseEntry(FullyVaccinated, false),
            OptOutResponseEntry(ClinicalTrial, true)
        )

        verify {
            navigateObserver.onChanged(createNavigationState(QuestionnaireOutcome.FullyVaccinated, expected))
        }
    }

    @Test
    fun `when in England, all doses selected yes, date selected no, clinical trial selected no, medically exempt yes, on click continue navigates to review`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(NO)
        testSubject.onMedicallyExemptOptionChanged(YES)
        testSubject.onClickContinue()

        val expected = listOf(
            OptOutResponseEntry(FullyVaccinated, true),
            OptOutResponseEntry(DoseDate, false),
            OptOutResponseEntry(ClinicalTrial, false),
            OptOutResponseEntry(MedicallyExempt, true)
        )

        verify {
            navigateObserver.onChanged(createNavigationState(QuestionnaireOutcome.MedicallyExempt, expected))
        }
    }

    @Test
    fun `when in England, all doses selected yes, date selected no, clinical trial selected no, medically exempt no, on click continue navigates to review`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onLastDoseDateOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(NO)
        testSubject.onMedicallyExemptOptionChanged(NO)
        testSubject.onClickContinue()

        val expected = listOf(
            OptOutResponseEntry(FullyVaccinated, true),
            OptOutResponseEntry(DoseDate, false),
            OptOutResponseEntry(ClinicalTrial, false),
            OptOutResponseEntry(MedicallyExempt, false)
        )

        verify {
            navigateObserver.onChanged(createNavigationState(QuestionnaireOutcome.NotExempt, expected))
        }
    }

    @Test
    fun `when in England, all doses option selected no, medically exempt option selected yes, on click continue navigates to review`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(NO)
        testSubject.onMedicallyExemptOptionChanged(YES)
        testSubject.onClickContinue()

        val expected = listOf(
            OptOutResponseEntry(FullyVaccinated, false),
            OptOutResponseEntry(MedicallyExempt, true)
        )

        verify {
            navigateObserver.onChanged(createNavigationState(QuestionnaireOutcome.MedicallyExempt, expected))
        }
    }

    @Test
    fun `when in England, all doses option selected no, medically exempt option selected no, clinical trial selected yes, on click continue navigates to review`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(NO)
        testSubject.onMedicallyExemptOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(YES)
        testSubject.onClickContinue()

        val expected = listOf(
            OptOutResponseEntry(FullyVaccinated, false),
            OptOutResponseEntry(MedicallyExempt, false),
            OptOutResponseEntry(ClinicalTrial, true)
        )

        verify {
            navigateObserver.onChanged(createNavigationState(QuestionnaireOutcome.FullyVaccinated, expected))
        }
    }

    @Test
    fun `when in England, all doses option selected no, medically exempt option selected no, clinical trial selected no, on click continue navigates to review`() {
        coEvery { mockLocalAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        testSubject.onFullyVaccinatedOptionChanged(NO)
        testSubject.onMedicallyExemptOptionChanged(NO)
        testSubject.onClinicalTrialOptionChanged(NO)
        testSubject.onClickContinue()

        val expected = listOf(
            OptOutResponseEntry(FullyVaccinated, false),
            OptOutResponseEntry(MedicallyExempt, false),
            OptOutResponseEntry(ClinicalTrial, false)
        )

        verify {
            navigateObserver.onChanged(createNavigationState(QuestionnaireOutcome.NotExempt, expected))
        }
    }

    @Test
    fun `lastDoseDateLimit returns result of getLastDoseDateLimit`() {
        val expectedDate = LocalDate.of(2021, 9, 5)
        every { mockGetLastDoseDateLimit() } returns expectedDate

        setUpTestSubject()

        val expectedViewState = ViewState(
            questions = listOf(Question(FullyVaccinated, state = null)),
            date = expectedDate,
            showSubtitle = true
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when cannot get last dose date limit, send finish navigation target`() {
        every { mockGetLastDoseDateLimit() } returns null

        setUpTestSubject()

        verify(exactly = 0) { viewStateObserver.onChanged(any()) }
        verify { navigateObserver.onChanged(Finish) }
    }

    @Test
    fun `when already in isolation, hide subtitle`() {
        every { isolationLogicalState.isActiveIndexCase(fixedClock) } returns true
        testSubject.onFullyVaccinatedOptionChanged(YES)
        testSubject.onClickContinue()

        val expectedViewState = ViewState(
            questions = listOf(Question(FullyVaccinated, YES), Question(DoseDate, null)),
            showError = false,
            date = expectedLastDoseDateLimit,
            showSubtitle = false
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    private fun setUpTestSubject(): ExposureNotificationVaccinationStatusViewModel {
        return ExposureNotificationVaccinationStatusViewModel(
            mockGetLastDoseDateLimit,
            mockLocalAuthorityPostCodeProvider,
            isolationStateMachine,
            fixedClock
        ).also {
            it.viewState().observeForever(viewStateObserver)
            it.navigate().observeForever(navigateObserver)
        }
    }

    private fun createViewState(questions: List<Question>, showError: Boolean = false): ViewState =
        ViewState(
            questions = questions,
            showError = showError,
            date = expectedLastDoseDateLimit,
            showSubtitle = true
        )

    private fun createNavigationState(
        questionnaireOutcome: QuestionnaireOutcome,
        vaccinationStatusResponse: List<OptOutResponseEntry>
    ): Review {
        return Review(
            ReviewData(
                questionnaireOutcome,
                ageResponse = OptOutResponseEntry(IsAdult, true),
                vaccinationStatusResponses = vaccinationStatusResponse
            )
        )
    }
}
