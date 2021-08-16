package uk.nhs.nhsx.covid19.android.app.questionnaire

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlag.NEW_NO_SYMPTOMS_SCREEN
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.NoIsolationSymptomAdvice.NoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.QuestionnaireIsolationHandler
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.LoadQuestionnaire
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.AdviceScreen
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.NewNoSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NavigationTarget.ReviewSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireState
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.QuestionnaireViewModel
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.remote.data.QuestionnaireResponse
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class QuestionnaireViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val loadQuestionnaire = mockk<LoadQuestionnaire>()
    private val questionnaireIsolationHandler = mockk<QuestionnaireIsolationHandler>()
    private val loadQuestionnaireResultObserver = mockk<Observer<Lce<QuestionnaireState>>>(relaxUnitFun = true)
    private val isolationStateMachine = mockk<IsolationStateMachine>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-01-01T10:00:00Z"), ZoneOffset.UTC)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private val testSubject =
        QuestionnaireViewModel(loadQuestionnaire, questionnaireIsolationHandler, isolationStateMachine, fixedClock)

    @Before
    fun setUp() {
        FeatureFlagTestHelper.disableFeatureFlag(NEW_NO_SYMPTOMS_SCREEN)
        testSubject.navigationTarget().observeForever(navigationTargetObserver)
        testSubject.viewState().observeForever(loadQuestionnaireResultObserver)
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun `load questionnaire returns success`() = runBlocking {
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
                        showDialog = false,
                    )
                )
            )
        }
    }

    @Test
    fun `load questionnaire multiple times does not fire multiple requests`() = runBlocking {
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
                        showDialog = false,
                    )
                )
            )
        }
    }

    @Test
    fun `load questionnaire returns failure`() = runBlocking {
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
                    showDialog = false,
                )
            )
        )

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
                        showDialog = false,
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
                    showDialog = false,
                )
            )
        )

        testSubject.onButtonReviewSymptomsClicked()

        verify {
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    QuestionnaireState(
                        questions,
                        1.0f,
                        14,
                        showError = true,
                        showDialog = false,
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
            showDialog = false,
        )
        val response = Lce.Success(viewState)

        testSubject.viewState.postValue(response)

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
            showDialog = true,
        )
        val response = Lce.Success(viewState)
        testSubject.viewState.postValue(response)

        testSubject.onNoSymptomsDialogDismissed()

        verify {
            loadQuestionnaireResultObserver.onChanged(Lce.Success(viewState.copy(showDialog = false)))
        }
    }

    @Test
    fun `when no symptoms confirmed, feature flag disabled navigate to advice screen`() {
        val viewState = QuestionnaireState(
            questions = emptyList(),
            1.0f,
            14,
            showError = false,
            showDialog = true,
        )
        val response = Lce.Success(viewState)
        testSubject.viewState.postValue(response)

        every {
            questionnaireIsolationHandler.computeAdvice(
                riskThreshold = Float.MAX_VALUE,
                selectedSymptoms = emptyList(),
                onsetDate = SelectedDate.NotStated
            )
        } returns NoSymptoms

        runWithFeature(NEW_NO_SYMPTOMS_SCREEN, enabled = false) {
            testSubject.onNoSymptomsConfirmed()
        }

        verify { navigationTargetObserver.onChanged(AdviceScreen(NoSymptoms)) }
    }

    @Test
    fun `when no symptoms confirmed, feature flag enabled and user is in isolation navigate to advice screen`() {
        every { isolationStateMachine.readLogicalState().isActiveIsolation(fixedClock) } returns true
        val viewState = QuestionnaireState(
            questions = emptyList(),
            1.0f,
            14,
            showError = false,
            showDialog = true,
        )
        val response = Lce.Success(viewState)
        testSubject.viewState.postValue(response)

        every {
            questionnaireIsolationHandler.computeAdvice(
                riskThreshold = Float.MAX_VALUE,
                selectedSymptoms = emptyList(),
                onsetDate = SelectedDate.NotStated
            )
        } returns NoSymptoms

        runWithFeatureEnabled(NEW_NO_SYMPTOMS_SCREEN) {
            testSubject.onNoSymptomsConfirmed()
        }

        verify { navigationTargetObserver.onChanged(AdviceScreen(NoSymptoms)) }
    }

    @Test
    fun `when no symptoms clicked then show confirmation dialog`() {
        val viewState = QuestionnaireState(questions = emptyList(), 1.0f, 14, showError = false, showDialog = false)
        val response = Lce.Success(viewState)
        testSubject.viewState.postValue(response)

        testSubject.onNoSymptomsClicked()

        val updatedViewState = viewState.copy(showDialog = true)
        verify { loadQuestionnaireResultObserver.onChanged(Lce.Success(updatedViewState)) }
        verify(exactly = 0) { navigationTargetObserver.onChanged(any()) }
    }

    @Test
    fun `when no symptoms confirmed, feature flag enabled and user is not in active isolation then emit NewNoSymptoms as navigation target`() {
        every { isolationStateMachine.readLogicalState().isActiveIsolation(fixedClock) } returns false

        runWithFeatureEnabled(NEW_NO_SYMPTOMS_SCREEN, clearFeatureFlags = true) {
            testSubject.onNoSymptomsConfirmed()
        }

        verify { navigationTargetObserver.onChanged(NewNoSymptoms) }
        verify(exactly = 0) { loadQuestionnaireResultObserver.onChanged(any()) }
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
