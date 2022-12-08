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
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestOriginViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestOriginViewModel.NavigationTarget.TestDate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestOriginViewModel.NavigationTarget.TestKitType
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.TestOriginViewModel.ViewState
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_1 as YES
import uk.nhs.nhsx.covid19.android.app.widgets.BinaryVerticalRadioGroup.BinaryVerticalRadioGroupOption.OPTION_2 as NO

class TestOriginViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-27T01:00:00.00Z"), ZoneOffset.UTC)
    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, null, null, null, null, null)

    private val selfReportTestQuestionsWithAllAnswers = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, true, ChosenDate(false, LocalDate.now(fixedClock)), true,
        ChosenDate(false, LocalDate.now(fixedClock)), true)

    private val testSubject = TestOriginViewModel(selfReportTestQuestions)

    @Before
    fun setup() {
        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)
    }

    @Test
    fun `when nothing selected, error state set to true`() {
        testSubject.onClickContinue()

        val expectedState = ViewState(testOriginSelection = null, hasError = true)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when Yes selected, testOriginSelection set to YES`() {
        testSubject.onTestOriginOptionChecked(YES)

        val expectedState = ViewState(testOriginSelection = YES, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when no selected, testOriginSelection set to No`() {
        testSubject.onTestOriginOptionChecked(NO)

        val expectedState = ViewState(testOriginSelection = NO, hasError = false)
        verify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is pressed, should send user to test kit type screen`() {
        testSubject.onBackPressed()

        val expectedState = TestKitType(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when yes selected, navigate to test date screen`() {
        testSubject.onTestOriginOptionChecked(YES)
        testSubject.onClickContinue()

        val expectedState = TestDate(selfReportTestQuestions.copy(isNHSTest = true))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when no selected, navigate to test date screen`() {
        testSubject.onTestOriginOptionChecked(NO)
        testSubject.onClickContinue()

        val expectedState = TestDate(selfReportTestQuestions.copy(isNHSTest = false))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when LFD, isNHS and reported previously saved, answer changed to not NHS test should navigate to test date screen, and null related fields`() {
        val testSubjectWithLFDFromNHS = TestOriginViewModel(selfReportTestQuestionsWithAllAnswers)

        testSubjectWithLFDFromNHS.navigate().observeForever(navigationStateObserver)

        testSubjectWithLFDFromNHS.onTestOriginOptionChecked(NO)
        testSubjectWithLFDFromNHS.onClickContinue()

        val expectedState = TestDate(
            selfReportTestQuestionsWithAllAnswers.copy(isNHSTest = false, hasReportedResult = null
            )
        )

        verify { navigationStateObserver.onChanged(expectedState) }
    }
}
