package uk.nhs.nhsx.covid19.android.app.questionnaire

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.LoadQuestionnaire
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.NoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.ReviewSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.SymptomsAdviceForIndexCaseThenNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireState
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireViewModel
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.remote.data.QuestionnaireResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import java.time.Clock

class QuestionnaireViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val loadQuestionnaire = mockk<LoadQuestionnaire>()
    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val clock = mockk<Clock>()
    private val loadQuestionnaireResultObserver = mockk<Observer<Lce<QuestionnaireState>>>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private val testSubject = QuestionnaireViewModel(loadQuestionnaire, isolationStateMachine, clock)

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
                        showError = false,
                        showDialog = false
                    )
                )
            )
        }
    }

    @Test
    fun `load questionnaire multiple times does not fire multiple requests`() = runBlocking {
        testSubject.viewState().observeForever(loadQuestionnaireResultObserver)

        coEvery { loadQuestionnaire.invoke() } returns Success(
            QuestionnaireResponse(
                listOf(),
                riskThreshold = 100.0f,
                symptomsOnsetWindowDays = 14
            )
        )

        testSubject.loadQuestionnaire()
        testSubject.loadQuestionnaire()

        coVerify(exactly = 1) { loadQuestionnaire.invoke() }
        verifyOrder {
            loadQuestionnaireResultObserver.onChanged(Lce.Loading)
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    QuestionnaireState(
                        listOf(),
                        riskThreshold = 100.0f,
                        symptomsOnsetWindowDays = 14,
                        showError = false,
                        showDialog = false
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
                    showError = true,
                    showDialog = false
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
                        showError = false,
                        showDialog = false
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
                    showError = false,
                    showDialog = false
                )
            )
        )
        testSubject.viewState().observeForever(loadQuestionnaireResultObserver)
        testSubject.navigationTarget().observeForever(navigationTargetObserver)

        testSubject.onButtonReviewSymptomsClicked()

        verify {
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    QuestionnaireState(
                        questions,
                        1.0f,
                        14,
                        showError = true,
                        showDialog = false
                    )
                )
            )
        }

        verify { navigationTargetObserver wasNot called }
    }

    @Test
    fun `onButtonReviewSymptomsClicked navigates to review screen`() {
        val questions = listOf(
            question("S1", true),
            question("S2", true)
        )
        val viewState = QuestionnaireState(
            questions,
            1.0f,
            14,
            showError = false,
            showDialog = false
        )
        val response = Lce.Success(viewState)

        testSubject.viewState.postValue(response)
        testSubject.navigationTarget().observeForever(navigationTargetObserver)

        testSubject.onButtonReviewSymptomsClicked()

        verify {
            with(viewState) {
                navigationTargetObserver.onChanged(
                    ReviewSymptoms(questions, riskThreshold, symptomsOnsetWindowDays)
                )
            }
        }
    }

    @Test
    fun `onNoSymptomsDialogDismissed hides dialog`() {
        val viewState = QuestionnaireState(
            questions = emptyList(),
            1.0f,
            14,
            showError = false,
            showDialog = true
        )
        val response = Lce.Success(viewState)
        testSubject.viewState.postValue(response)

        testSubject.onNoSymptomsDialogDismissed()

        testSubject.viewState().observeForever(loadQuestionnaireResultObserver)
        verify {
            loadQuestionnaireResultObserver.onChanged(Lce.Success(viewState.copy(showDialog = false)))
        }
    }

    @Test
    fun `when isolating due to positive test result and user confirms no symptoms`() {
        val viewState = QuestionnaireState(
            questions = emptyList(),
            1.0f,
            14,
            showError = false,
            showDialog = true
        )
        val response = Lce.Success(viewState)
        testSubject.viewState.postValue(response)

        val mockedLogicalState = mockk<PossiblyIsolating>()
        every { mockedLogicalState.hasActivePositiveTestResult(clock) } returns true
        every { isolationStateMachine.readLogicalState() } returns mockedLogicalState

        testSubject.onNoSymptomsConfirmed()

        testSubject.navigationTarget().observeForever(navigationTargetObserver)
        verify { navigationTargetObserver.onChanged(SymptomsAdviceForIndexCaseThenNoSymptoms) }
    }

    @Test
    fun `when isolating for other reason than positive test result and user confirms no symptoms`() {
        val viewState = QuestionnaireState(
            questions = emptyList(),
            1.0f,
            14,
            showError = false,
            showDialog = true
        )

        val response = Lce.Success(viewState)
        testSubject.viewState.postValue(response)

        val mockedLogicalState = mockk<PossiblyIsolating>()
        every { mockedLogicalState.hasActivePositiveTestResult(clock) } returns false
        every { isolationStateMachine.readLogicalState() } returns mockedLogicalState

        testSubject.onNoSymptomsConfirmed()

        testSubject.navigationTarget().observeForever(navigationTargetObserver)

        verify { navigationTargetObserver.onChanged(NoSymptoms) }
    }

    @Test
    fun `when not isolating and user confirms no symptoms`() {
        val viewState = QuestionnaireState(
            questions = emptyList(),
            1.0f,
            14,
            showError = false,
            showDialog = true
        )

        val response = Lce.Success(viewState)
        testSubject.viewState.postValue(response)

        val mockedLogicalState = mockk<NeverIsolating>()
        every { isolationStateMachine.readLogicalState() } returns mockedLogicalState

        testSubject.onNoSymptomsConfirmed()

        testSubject.navigationTarget().observeForever(navigationTargetObserver)

        verify { navigationTargetObserver.onChanged(NoSymptoms) }
    }

    private fun question(name: String, checked: Boolean): Question {
        val symptom = Symptom(
            title = TranslatableString(mapOf("en-GB" to name)),
            description = TranslatableString(mapOf("en-GB" to "")),
            riskWeight = 1.0
        )
        return Question(symptom, checked)
    }
}
