package uk.nhs.nhsx.covid19.android.app.questionnaire

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.called
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.Translatable
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.LoadQuestionnaire
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireState
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireViewModel
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.remote.data.QuestionnaireResponse

class QuestionnaireViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val loadQuestionnaire = mockk<LoadQuestionnaire>()

    private val loadQuestionnaireResultObserver =
        mockk<Observer<Lce<QuestionnaireState>>>(relaxed = true)

    private val navigateToReviewScreenObserver =
        mockk<Observer<List<Question>>>(relaxed = true)

    private val testSubject =
        QuestionnaireViewModel(
            loadQuestionnaire
        )

    @Test
    fun `load questionnaire returns success`() = runBlocking {
        testSubject.viewState().observeForever(loadQuestionnaireResultObserver)

        coEvery { loadQuestionnaire.invoke() } returns Success(
            QuestionnaireResponse(
                listOf(),
                riskThreshold = 100.0f,
                symptomsOnsetWindowDays = 14
            )
        )

        testSubject.loadQuestionnaire()

        verifyOrder {
            loadQuestionnaireResultObserver.onChanged(Lce.Loading)
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    QuestionnaireState(
                        listOf(),
                        riskThreshold = 100.0f,
                        symptomsOnsetWindowDays = 14,
                        showError = false
                    )
                )
            )
        }
    }

    @Test
    fun `load questionnaire returns failure`() = runBlocking {
        testSubject.viewState().observeForever(loadQuestionnaireResultObserver)

        val testException = Exception("Test error")

        coEvery { loadQuestionnaire.invoke() } returns Failure(testException)

        testSubject.loadQuestionnaire()

        verifyOrder {
            loadQuestionnaireResultObserver.onChanged(Lce.Loading)
            loadQuestionnaireResultObserver.onChanged(Lce.Error(testException))
        }
    }

    @Test
    fun `toggleQuestion toggles question and hides error`() {
        val question = question("S1", false)
        val initialQuestions = listOf(
            question,
            question("S2", false)
        )
        testSubject.viewState.postValue(
            Lce.Success(
                QuestionnaireState(
                    initialQuestions,
                    1.0f,
                    14,
                    true
                )
            )
        )
        testSubject.viewState().observeForever(loadQuestionnaireResultObserver)

        testSubject.toggleQuestion(question)

        val updatedQuestions = listOf(
            question("S1", true),
            question("S2", false)
        )
        verify {
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    QuestionnaireState(
                        updatedQuestions,
                        1.0f,
                        14,
                        false
                    )
                )
            )
        }
    }

    @Test
    fun `onButtonReviewSymptomsClicked check if there are any symptoms checked`() {
        val questions = listOf(
            question("S1", false),
            question("S2", false)
        )
        testSubject.viewState.postValue(
            Lce.Success(
                QuestionnaireState(
                    questions,
                    1.0f,
                    14,
                    false
                )
            )
        )
        testSubject.viewState().observeForever(loadQuestionnaireResultObserver)
        testSubject.navigateToReviewScreen().observeForever(navigateToReviewScreenObserver)

        testSubject.onButtonReviewSymptomsClicked()

        verify {
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    QuestionnaireState(
                        questions,
                        1.0f,
                        14,
                        true
                    )
                )
            )
        }

        verify { navigateToReviewScreenObserver wasNot called }
    }

    @Test
    fun `onButtonReviewSymptomsClicked navigates to review screen`() {
        val questions = listOf(
            question("S1", true),
            question("S2", true)
        )
        val state = Lce.Success(
            QuestionnaireState(
                questions,
                1.0f,
                14,
                false
            )
        )
        testSubject.viewState.postValue(state)
        testSubject.navigateToReviewScreen().observeForever(navigateToReviewScreenObserver)

        testSubject.onButtonReviewSymptomsClicked()

        verify { navigateToReviewScreenObserver.onChanged(questions) }
    }

    private fun question(name: String, checked: Boolean): Question {
        val symptom = Symptom(
            title = Translatable(mapOf("en-GB" to name)),
            description = Translatable(mapOf("en-GB" to "")),
            riskWeight = 1.0
        )
        return ReviewSymptomItem.Question(symptom, checked)
    }
}
