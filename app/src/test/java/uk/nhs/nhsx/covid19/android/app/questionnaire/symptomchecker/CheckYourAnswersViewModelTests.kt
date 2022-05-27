package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.NavigationTarget.HowYouFeel
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.NavigationTarget.Next
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.NavigationTarget.YourSymptoms
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.CheckYourAnswersViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.CONTINUE_NORMAL_ACTIVITIES
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.TRY_TO_STAY_AT_HOME
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CheckYourAnswersViewModelTests {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val mockSymptomCheckerAdviceHandler = mockk<SymptomCheckerAdviceHandler>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val lastCompletedV2SymptomsQuestionnaireDateProvider = mockk<LastCompletedV2SymptomsQuestionnaireDateProvider>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2022-05-01T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = CheckYourAnswersViewModel(
        questions = symptomsCheckerQuestions,
        symptomCheckerAdviceHandler = mockSymptomCheckerAdviceHandler,
        analyticsEventProcessor = analyticsEventProcessor,
        lastCompletedV2SymptomsQuestionnaireDateProvider = lastCompletedV2SymptomsQuestionnaireDateProvider,
        clock = fixedClock
    )

    @Before
    fun setup() {
        testSubject.navigate().observeForever(navigationStateObserver)
    }

    @Test
    fun `when change your symptoms is clicked, navigate to your symptoms screen`() {
        testSubject.onClickYourSymptomsChange()

        val expectedState = YourSymptoms(symptomsCheckerQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when change how do you feel is clicked, navigate to how you feel screen`() {
        testSubject.onClickHowYouFeelChange()

        val expectedState = HowYouFeel(symptomsCheckerQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when submit is clicked, navigate to symptom checker advice screen with try to stay at home`() {
        every { mockSymptomCheckerAdviceHandler.invoke(any()) } returns TRY_TO_STAY_AT_HOME
        testSubject.onClickSubmitAnswers()

        val expectedState = Next(symptomsCheckerQuestions, TRY_TO_STAY_AT_HOME)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when submit is clicked, navigate to symptom checker advice screen with continue normal activities`() {
        every { mockSymptomCheckerAdviceHandler.invoke(any()) } returns CONTINUE_NORMAL_ACTIVITIES
        testSubject.onClickSubmitAnswers()

        val expectedState = Next(symptomsCheckerQuestions, CONTINUE_NORMAL_ACTIVITIES)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `verify ViewState is updated by init block`() {
        CheckYourAnswersViewModel(
            questions = symptomsCheckerQuestions,
            symptomCheckerAdviceHandler = mockSymptomCheckerAdviceHandler,
            analyticsEventProcessor = analyticsEventProcessor,
            lastCompletedV2SymptomsQuestionnaireDateProvider = lastCompletedV2SymptomsQuestionnaireDateProvider,
            clock = fixedClock
        ).viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(symptomsCheckerQuestions)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    companion object {
        val symptomsCheckerQuestions = SymptomsCheckerQuestions(
            nonCardinalSymptoms = NonCardinalSymptoms(
                title = TranslatableString(mapOf()),
                isChecked = true,
                nonCardinalSymptomsText = TranslatableString(mapOf())
            ),
            cardinalSymptom = CardinalSymptom(
                title = TranslatableString(mapOf()),
                isChecked = true
            ),
            howDoYouFeelSymptom = HowDoYouFeelSymptom(true)
        )
    }
}
