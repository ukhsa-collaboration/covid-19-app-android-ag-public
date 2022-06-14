package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Lce
import uk.nhs.nhsx.covid19.android.app.common.Lce.Loading
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Cardinal
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.LoadQuestionnaire
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.NonCardinal
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.YourSymptomsViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.YourSymptomsViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.remote.data.QuestionnaireResponse
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_1
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryRadioGroup.BinaryRadioGroupOption.OPTION_2

class YourSymptomsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val loadQuestionnaire = mockk<LoadQuestionnaire>()
    private val loadQuestionnaireResultObserver = mockk<Observer<Lce<ViewState>>>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private val testSubject = YourSymptomsViewModel(loadQuestionnaire)

    private val questionnaireResponse = QuestionnaireResponse(
        listOf(),
        Cardinal(title = TranslatableString(mapOf())),
        NonCardinal(title = TranslatableString(mapOf()), description = TranslatableString(mapOf())),
        riskThreshold = 100.0f,
        symptomsOnsetWindowDays = 14,
        isSymptomaticSelfIsolationForWalesEnabled = false
    )

    @Before
    fun setUp() {
        testSubject.navigate().observeForever(navigationTargetObserver)
        testSubject.viewState().observeForever(loadQuestionnaireResultObserver)
    }

    @Test
    fun `load questionnaire returns success`() = runBlocking {
        coEvery { loadQuestionnaire.invoke() } returns Success(questionnaireResponse)

        testSubject.loadQuestionnaire()

        verifyOrder {
            loadQuestionnaireResultObserver.onChanged(Loading)
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    ViewState(
                        cardinal = Cardinal(title = TranslatableString(mapOf())),
                        nonCardinal = NonCardinal(title = TranslatableString(mapOf()), description = TranslatableString(mapOf())),
                        nonCardinalSymptomsSelection = null,
                        cardinalSymptomSelection = null,
                        hasDidNotCheckAnyQuestionError = false,
                        hasDidNotCheckNonCardinalSymptomsError = false,
                        hasDidNotCheckCardinalSymptomsError = false
                    )
                )
            )
        }
    }

    @Test
    fun `load questionnaire multiple times does not fire multiple requests`() = runBlocking {
        coEvery { loadQuestionnaire.invoke() } returns Success(questionnaireResponse)

        testSubject.loadQuestionnaire()
        testSubject.loadQuestionnaire()

        coVerify(exactly = 1) { loadQuestionnaire.invoke() }

        verifyOrder {
            loadQuestionnaireResultObserver.onChanged(Loading)
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    ViewState(
                        cardinal = Cardinal(title = TranslatableString(mapOf())),
                        nonCardinal = NonCardinal(title = TranslatableString(mapOf()), description = TranslatableString(mapOf())),
                        nonCardinalSymptomsSelection = null,
                        cardinalSymptomSelection = null,
                        hasDidNotCheckAnyQuestionError = false,
                        hasDidNotCheckNonCardinalSymptomsError = false,
                        hasDidNotCheckCardinalSymptomsError = false
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
    fun `load questionnaire with questions previously answered yes updates viewState on load success to reflect answers`() = runBlocking {
        symptomsQuestionsAnsweredViewStateShouldMatchTheseOnSuccess(previouslyAnsweredYes = true)
    }

    @Test
    fun `load questionnaire with questions previously answered no updates viewState on load success to reflect answers`() = runBlocking {
        symptomsQuestionsAnsweredViewStateShouldMatchTheseOnSuccess(previouslyAnsweredYes = false)
    }

    @Test
    fun `load questionnaire with one of two questions previously answered should not update viewState to reflect answers`() = runBlocking {
        coEvery { loadQuestionnaire.invoke() } returns Success(questionnaireResponse)

        var symptomsCheckerQuestions: SymptomsCheckerQuestions = SymptomsCheckerQuestions(
            nonCardinalSymptoms = NonCardinalSymptoms(
                nonCardinalSymptomsText = TranslatableString(mapOf()),
                title = TranslatableString(mapOf()), isChecked = true
            ),
            cardinalSymptom = CardinalSymptom(title = TranslatableString(mapOf()), isChecked = null),
            null
        )

        testSubject.loadQuestionnaire(symptomsCheckerQuestions)

        verifyOrder {
            loadQuestionnaireResultObserver.onChanged(Loading)
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    ViewState(
                        cardinal = Cardinal(title = TranslatableString(mapOf())),
                        nonCardinal = NonCardinal(
                            title = TranslatableString(mapOf()),
                            description = TranslatableString(mapOf())
                        ),
                        nonCardinalSymptomsSelection = null,
                        cardinalSymptomSelection = null,
                        hasDidNotCheckAnyQuestionError = false,
                        hasDidNotCheckNonCardinalSymptomsError = false,
                        hasDidNotCheckCardinalSymptomsError = false
                    )
                )
            )
        }
    }

    @Test
    fun `click continue with both questions answered yes should send symptomsCheckerQuestions`() = runBlocking {
        clickContinueAndVerifyCorrectViewStateNoErrors(OPTION_1, OPTION_1)
    }

    @Test
    fun `click continue with both questions answered no should send symptomsCheckerQuestions`() = runBlocking {
        clickContinueAndVerifyCorrectViewStateNoErrors(OPTION_2, OPTION_2)
    }

    @Test
    fun `click continue with questions answered yes and no should send symptomsCheckerQuestions`() = runBlocking {
        clickContinueAndVerifyCorrectViewStateNoErrors(OPTION_1, OPTION_2)
    }

    @Test
    fun `click continue with questions answered no and yes should send symptomsCheckerQuestions`() = runBlocking {
        clickContinueAndVerifyCorrectViewStateNoErrors(OPTION_2, OPTION_1)
    }

    @Test
    fun `click continue with first question not answered should display error`() = runBlocking {
        coEvery { loadQuestionnaire.invoke() } returns Success(questionnaireResponse)

        testSubject.loadQuestionnaire()

        testSubject.onCardinalOptionChecked(OPTION_1)

        testSubject.onClickContinue()
        val viewState = ViewState(
            questionnaireResponse.cardinal,
            questionnaireResponse.noncardinal,
            nonCardinalSymptomsSelection = null,
            cardinalSymptomSelection = OPTION_1,
            hasDidNotCheckAnyQuestionError = false,
            hasDidNotCheckNonCardinalSymptomsError = true,
            hasDidNotCheckCardinalSymptomsError = false
        )

        verifyOrder {
            loadQuestionnaireResultObserver.onChanged(Lce.Loading)
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(viewState.copy(hasDidNotCheckNonCardinalSymptomsError = false, cardinalSymptomSelection = null))
            )
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(viewState.copy(hasDidNotCheckNonCardinalSymptomsError = false))
            )
            loadQuestionnaireResultObserver.onChanged(Lce.Success(viewState))
        }
    }

    @Test
    fun `click continue with second question not answered should display error`() = runBlocking {
        coEvery { loadQuestionnaire.invoke() } returns Success(questionnaireResponse)

        testSubject.loadQuestionnaire()

        testSubject.onNonCardinalOptionChecked(OPTION_1)

        testSubject.onClickContinue()
        val viewState = ViewState(
            questionnaireResponse.cardinal,
            questionnaireResponse.noncardinal,
            nonCardinalSymptomsSelection = OPTION_1,
            cardinalSymptomSelection = null,
            hasDidNotCheckAnyQuestionError = false,
            hasDidNotCheckNonCardinalSymptomsError = false,
            hasDidNotCheckCardinalSymptomsError = true
        )

        verifyOrder {
            loadQuestionnaireResultObserver.onChanged(Lce.Loading)
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(viewState.copy(hasDidNotCheckCardinalSymptomsError = false, nonCardinalSymptomsSelection = null))
            )
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(viewState.copy(hasDidNotCheckCardinalSymptomsError = false))
            )
            loadQuestionnaireResultObserver.onChanged(Lce.Success(viewState))
        }
    }

    @Test
    fun `click continue with no questions answered should display error`() = runBlocking {
        coEvery { loadQuestionnaire.invoke() } returns Success(questionnaireResponse)

        testSubject.loadQuestionnaire()

        testSubject.onClickContinue()
        val viewState = ViewState(
            questionnaireResponse.cardinal,
            questionnaireResponse.noncardinal,
            nonCardinalSymptomsSelection = null,
            cardinalSymptomSelection = null,
            hasDidNotCheckAnyQuestionError = true,
            hasDidNotCheckNonCardinalSymptomsError = true,
            hasDidNotCheckCardinalSymptomsError = true
        )

        verifyOrder {
            loadQuestionnaireResultObserver.onChanged(Lce.Loading)
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    viewState.copy(
                        hasDidNotCheckAnyQuestionError = false,
                        hasDidNotCheckNonCardinalSymptomsError = false,
                        hasDidNotCheckCardinalSymptomsError = false
                    )
                )
            )
            loadQuestionnaireResultObserver.onChanged(Lce.Success(viewState))
        }
    }

    private fun symptomsQuestionsAnsweredViewStateShouldMatchTheseOnSuccess(previouslyAnsweredYes: Boolean) {
        coEvery { loadQuestionnaire.invoke() } returns Success(questionnaireResponse)

        var symptomsCheckerQuestions: SymptomsCheckerQuestions = SymptomsCheckerQuestions(
            nonCardinalSymptoms = NonCardinalSymptoms(
                nonCardinalSymptomsText = TranslatableString(mapOf()),
                title = TranslatableString(mapOf()), isChecked = previouslyAnsweredYes
            ),
            cardinalSymptom = CardinalSymptom(title = TranslatableString(mapOf()), isChecked = previouslyAnsweredYes),
            null
        )

        testSubject.loadQuestionnaire(symptomsCheckerQuestions)

        verifyOrder {
            loadQuestionnaireResultObserver.onChanged(Loading)
            loadQuestionnaireResultObserver.onChanged(
                Lce.Success(
                    ViewState(
                        cardinal = Cardinal(title = TranslatableString(mapOf())),
                        nonCardinal = NonCardinal(
                            title = TranslatableString(mapOf()),
                            description = TranslatableString(mapOf())
                        ),
                        nonCardinalSymptomsSelection = if (previouslyAnsweredYes) OPTION_1 else OPTION_2,
                        cardinalSymptomSelection = if (previouslyAnsweredYes) OPTION_1 else OPTION_2,
                        hasDidNotCheckAnyQuestionError = false,
                        hasDidNotCheckNonCardinalSymptomsError = false,
                        hasDidNotCheckCardinalSymptomsError = false
                    )
                )
            )
        }
    }

    private fun clickContinueAndVerifyCorrectViewStateNoErrors(buttonOne: BinaryRadioGroupOption, buttonTwo: BinaryRadioGroupOption) {
        coEvery { loadQuestionnaire.invoke() } returns Success(questionnaireResponse)

        testSubject.loadQuestionnaire()

        testSubject.onNonCardinalOptionChecked(buttonOne)
        testSubject.onCardinalOptionChecked(buttonTwo)

        testSubject.onClickContinue()
        val updatedViewState = ViewState(
            questionnaireResponse.cardinal,
            questionnaireResponse.noncardinal,
            nonCardinalSymptomsSelection = buttonOne,
            cardinalSymptomSelection = buttonTwo,
            hasDidNotCheckAnyQuestionError = false,
            hasDidNotCheckNonCardinalSymptomsError = false,
            hasDidNotCheckCardinalSymptomsError = false
        )
        verify { loadQuestionnaireResultObserver.onChanged(Lce.Success(updatedViewState)) }
    }
}
