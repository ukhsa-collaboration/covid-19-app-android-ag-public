package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.SymptomsOnset
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.ReportedTestViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_2 as NO
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ReportedTestViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-27T01:00:00.00Z"), ZoneOffset.UTC)
    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, true, ChosenDate(false, LocalDate.now(fixedClock)), true,
        ChosenDate(false, LocalDate.now(fixedClock)), null)

    private val testSubject = ReportedTestViewModel(selfReportTestQuestions)

    @Before
    fun setup() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)
    }

    @Test
    fun `when nothing selected, error state set to true`() {
        testSubject.onClickContinue()

        val expectedState = ViewState(reportedTestSelection = null, hasError = true)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when Yes selected, reportedTestSelection set to YES`() {
        testSubject.onReportedTestOptionChecked(YES)

        val expectedState = ViewState(reportedTestSelection = YES, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when no selected, reportedTestSelection set to No`() {
        testSubject.onReportedTestOptionChecked(NO)

        val expectedState = ViewState(reportedTestSelection = NO, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is pressed with symptoms, should send user to symptoms onset screen`() {
        testSubject.onBackPressed()

        val expectedState = SymptomsOnset(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is pressed with symptoms answered no, should send user to symptoms screen`() {
        val selfReportTestQuestionsWithoutSymptoms = selfReportTestQuestions.copy(hadSymptoms = false, symptomsOnsetDate = null)
        val testSubjectWithoutSymptoms = ReportedTestViewModel(selfReportTestQuestionsWithoutSymptoms)

        testSubjectWithoutSymptoms.navigate().observeForever(navigationStateObserver)

        testSubjectWithoutSymptoms.onBackPressed()

        val expectedState = Symptoms(selfReportTestQuestionsWithoutSymptoms)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is pressed with symptoms unanswered, should send user to select test date screen`() {
        val selfReportTestQuestionsWithoutSymptomsAnswer = selfReportTestQuestions.copy(hadSymptoms = null, symptomsOnsetDate = null)
        val testSubjectWithoutSymptomsAnswer = ReportedTestViewModel(selfReportTestQuestionsWithoutSymptomsAnswer)

        testSubjectWithoutSymptomsAnswer.navigate().observeForever(navigationStateObserver)

        testSubjectWithoutSymptomsAnswer.onBackPressed()

        val expectedState = TestDate(selfReportTestQuestionsWithoutSymptomsAnswer)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when yes selected, navigate to check answers screen`() {
        testSubject.onReportedTestOptionChecked(YES)
        testSubject.onClickContinue()

        val expectedState = CheckAnswers(selfReportTestQuestions.copy(hasReportedResult = true))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when no selected, navigate to check answers screen`() {
        testSubject.onReportedTestOptionChecked(NO)
        testSubject.onClickContinue()

        val expectedState = CheckAnswers(selfReportTestQuestions.copy(hasReportedResult = false))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `previous yes selection set on init`() {
        val selfReportTestQuestionsWithPreviousYes = selfReportTestQuestions.copy(hasReportedResult = true)
        val testSubjectWithPreviousYes = ReportedTestViewModel(selfReportTestQuestionsWithPreviousYes)

        testSubjectWithPreviousYes.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(reportedTestSelection = YES, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `previous no selection set on init`() {
        val selfReportTestQuestionsWithPreviousNo = selfReportTestQuestions.copy(hasReportedResult = false)
        val testSubjectWithPreviousNo = ReportedTestViewModel(selfReportTestQuestionsWithPreviousNo)

        testSubjectWithPreviousNo.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(reportedTestSelection = NO, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }
}
