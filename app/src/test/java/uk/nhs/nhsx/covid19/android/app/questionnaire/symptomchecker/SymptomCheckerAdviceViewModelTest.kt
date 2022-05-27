package uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.CONTINUE_NORMAL_ACTIVITIES
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceResult.TRY_TO_STAY_AT_HOME
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.ViewState.ContinueNormalActivities
import uk.nhs.nhsx.covid19.android.app.questionnaire.symptomchecker.SymptomCheckerAdviceViewModel.ViewState.TryToStayAtHome
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SymptomCheckerAdviceViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)

    private val lastCompletedV2SymptomsQuestionnaireDateProvider =
        mockk<LastCompletedV2SymptomsQuestionnaireDateProvider>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2022-05-01T10:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `verify continue normal activities set`() {
        setupTestSubject(CONTINUE_NORMAL_ACTIVITIES)

        verify { viewStateObserver.onChanged(ContinueNormalActivities) }
    }

    @Test
    fun `verify try to stay at home set`() {
        setupTestSubject(TRY_TO_STAY_AT_HOME)

        verify { viewStateObserver.onChanged(TryToStayAtHome) }
    }

    private fun setupTestSubject(result: SymptomCheckerAdviceResult): SymptomCheckerAdviceViewModel {
        val testSubject = SymptomCheckerAdviceViewModel(
            questions = SymptomsCheckerQuestions(
                null,
                null,
                null
            ),
            result = result,
            analyticsEventProcessor = analyticsEventProcessor,
            lastCompletedV2SymptomsQuestionnaireDateProvider = lastCompletedV2SymptomsQuestionnaireDateProvider,
            clock = fixedClock
        )
        testSubject.viewState().observeForever(viewStateObserver)
        return testSubject
    }
}
