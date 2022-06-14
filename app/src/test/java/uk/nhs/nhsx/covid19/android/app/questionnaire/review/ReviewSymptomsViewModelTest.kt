package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.ReviewSymptomsViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsFixture.symptom1
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsFixture.symptom2
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsFixture.symptom3
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsFixture.symptom4
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.NegativeHeader
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.PositiveHeader
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewSymptomsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val symptomsAdviceIsolationHandler = mockk<QuestionnaireIsolationHandler>()
    private val navigateToSymptomsAdviceScreenObserver = mockk<Observer<SymptomAdvice>>(relaxed = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-22T20:00:00Z"), ZoneOffset.UTC)
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()

    private fun createTestSubject() = ReviewSymptomsViewModel(
        symptomsAdviceIsolationHandler,
        fixedClock,
        questions,
        riskThreshold,
        symptomsOnsetWindowDays,
        isSymptomaticSelfIsolationForWalesEnabled,
        localAuthorityPostCodeProvider
    )

    private val defaultViewState = ViewState(
        reviewSymptomItems = properReviewSymptomItems,
        onsetDate = NotStated,
        showOnsetDateError = false,
        symptomsOnsetWindowDays = symptomsOnsetWindowDays,
        showOnsetDatePicker = false,
        datePickerSelection = fixedClock.millis(),
        isSymptomaticSelfIsolationForWalesEnabled = false
    )

    @Test
    fun `when initialized contain the correct view state`() {
        val testSubject = createTestSubject()

        assertEquals(expected = defaultViewState, testSubject.viewState.value)
    }

    @Test
    fun `onDateSelected sets onsetDate`() {
        val testSubject = createTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        val selectedDate = Instant.parse("2020-05-21T10:00:00Z")

        testSubject.onDateSelected(dateInMillis = selectedDate.toEpochMilli())

        verify {
            viewStateObserver.onChanged(
                defaultViewState.copy(onsetDate = ExplicitDate(LocalDate.parse("2020-05-21")))
            )
        }
    }

    @Test
    fun `cannotRememberDateChecked sets onsetDate`() {
        val testSubject = createTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.cannotRememberDateChecked()

        verify {
            viewStateObserver.onChanged(
                defaultViewState.copy(onsetDate = CannotRememberDate)
            )
        }
    }

    @Test
    fun `cannotRememberDateUnchecked if date is explicitly stated`() {
        val testSubject = createTestSubject()

        testSubject.viewState.value = testSubject.viewState.value?.copy(
            onsetDate = ExplicitDate(
                LocalDate.parse("2020-05-21")
            ),
            showOnsetDateError = true
        )
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.cannotRememberDateUnchecked()

        verify {
            viewStateObserver.onChanged(
                defaultViewState.copy(onsetDate = ExplicitDate(LocalDate.parse("2020-05-21")))
            )
        }
    }

    @Test
    fun `cannotRememberDateUnchecked if no date is explicitly stated`() {
        val testSubject = createTestSubject()

        testSubject.viewState.value = testSubject.viewState.value?.copy(
            onsetDate = CannotRememberDate,
            showOnsetDateError = true
        )
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.cannotRememberDateUnchecked()

        verify {
            viewStateObserver.onChanged(defaultViewState)
        }
    }

    @Test
    fun `onButtonConfirmedClicked but date is not stated`() {
        val testSubject = createTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onButtonConfirmedClicked()

        verify { viewStateObserver.onChanged(defaultViewState.copy(showOnsetDateError = true)) }
    }

    @Test
    fun `onButtonConfirmedClicked and onset date is selected does not update view state and emits correct navigation event England`() {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
        val testSubject = createTestSubject()

        testSubject.navigateToSymptomAdviceScreen().observeForever(navigateToSymptomsAdviceScreenObserver)

        val onsetDate = ExplicitDate(LocalDate.parse("2020-05-21"))
        testSubject.viewState.value =
            testSubject.viewState.value?.copy(onsetDate = onsetDate, showOnsetDateError = true)

        val expectedIsolationSymptomsAdvice = mockk<IsolationSymptomAdvice>()

        every {
            symptomsAdviceIsolationHandler.computeAdvice(
                riskThreshold, properReviewSymptomItems.toSelectedSymptoms(), onsetDate, true
            )
        } returns expectedIsolationSymptomsAdvice

        testSubject.onButtonConfirmedClicked()

        verify { navigateToSymptomsAdviceScreenObserver.onChanged(expectedIsolationSymptomsAdvice) }
        verify(exactly = 0) { viewStateObserver.onChanged(any()) }
    }

    @Test
    fun `onButtonConfirmedClicked and onset date is selected does not update view state and emits correct navigation event Wales`() {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
        val testSubject = createTestSubject()

        testSubject.navigateToSymptomAdviceScreen().observeForever(navigateToSymptomsAdviceScreenObserver)

        val onsetDate = ExplicitDate(LocalDate.parse("2020-05-21"))
        testSubject.viewState.value =
            testSubject.viewState.value?.copy(onsetDate = onsetDate, showOnsetDateError = true)

        val expectedIsolationSymptomsAdvice = mockk<IsolationSymptomAdvice>()

        every {
            symptomsAdviceIsolationHandler.computeAdvice(
                riskThreshold, properReviewSymptomItems.toSelectedSymptoms(), onsetDate, false
            )
        } returns expectedIsolationSymptomsAdvice

        testSubject.onButtonConfirmedClicked()

        verify { navigateToSymptomsAdviceScreenObserver.onChanged(expectedIsolationSymptomsAdvice) }
        verify(exactly = 0) { viewStateObserver.onChanged(any()) }
    }

    @Test
    fun `toSelectedSymptoms returns empty list if list of ReviewSymptomItem does not contain a question`() {
        val reviewSymptomItemsWithoutQuestions = listOf(PositiveHeader, NegativeHeader)

        assertEquals(expected = emptyList(), reviewSymptomItemsWithoutQuestions.toSelectedSymptoms())
    }

    @Test
    fun `toSelectedSymptoms returns empty list if list of ReviewSymptomItem does not contain a checked question`() {
        val reviewSymptomItemsWithoutCheckedQuestions = listOf(
            PositiveHeader,
            NegativeHeader,
            Question(symptom2, false),
            Question(symptom4, false)
        )
        assertEquals(expected = emptyList(), reviewSymptomItemsWithoutCheckedQuestions.toSelectedSymptoms())
    }

    @Test
    fun `toSelectedSymptoms returns list of checked symptoms`() {
        assertEquals(
            expected = listOf(question1, question3).map { it.symptom },
            properReviewSymptomItems.toSelectedSymptoms()
        )
    }

    @Test
    fun `onset date in future is invalid`() {
        val testSubject = createTestSubject()
        assertFalse(testSubject.isOnsetDateValid(fixedClock.instant().plusSeconds(10).toEpochMilli(), 14))
    }

    @Test
    fun `onset date outside symptoms onset window is invalid`() {
        val testSubject = createTestSubject()
        assertFalse(testSubject.isOnsetDateValid(fixedClock.instant().minus(14, ChronoUnit.DAYS).toEpochMilli(), 14))
    }

    @Test
    fun `onset date in the past and within symptoms onset window is valid`() {
        val testSubject = createTestSubject()
        assertTrue(testSubject.isOnsetDateValid(fixedClock.instant().minus(13, ChronoUnit.DAYS).toEpochMilli(), 14))
    }

    @Test
    fun `onDatePickerDismissed updates view state to hide date picker`() {
        val testSubject = createTestSubject()
        testSubject.viewState.value = defaultViewState.copy(showOnsetDatePicker = true)
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onDatePickerDismissed()

        verify { viewStateObserver.onChanged(defaultViewState) }
    }

    @Test
    fun `onSelectDateClicked updates view state to show date picker`() {
        val testSubject = createTestSubject()
        testSubject.viewState.value = defaultViewState
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onSelectDateClicked()

        verify { viewStateObserver.onChanged(defaultViewState.copy(showOnsetDatePicker = true)) }
    }

    companion object {
        private val question1 = Question(symptom = symptom1, isChecked = true)
        private val question2 = Question(symptom = symptom2, isChecked = false)
        private val question3 = Question(symptom = symptom3, isChecked = true)
        private val question4 = Question(symptom = symptom4, isChecked = false)
        val questions = listOf(
            question1,
            question2,
            question3,
            question4
        )
        const val symptomsOnsetWindowDays = 14
        const val riskThreshold = 100F
        val properReviewSymptomItems = listOf(
            PositiveHeader,
            question1,
            question3,
            NegativeHeader,
            question2,
            question4
        )
        const val isSymptomaticSelfIsolationForWalesEnabled = false
    }
}
