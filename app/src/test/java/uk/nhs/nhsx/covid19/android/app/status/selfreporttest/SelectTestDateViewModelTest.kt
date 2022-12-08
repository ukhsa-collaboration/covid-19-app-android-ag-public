package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.CannotRememberDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.ExplicitDate
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SelectedDate.NotStated
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.CheckAnswers
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.ReportedTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.Symptoms
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.TestKitType
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.NavigationTarget.TestOrigin
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelectTestDateViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.DAYS
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelectTestDateViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-27T01:00:00.00Z"), ZoneOffset.UTC)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val datePickerContainerClickedObserver = mockk<Observer<Long>>(relaxed = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private val indexCaseSinceTestResultEndDateEngland = 6
    private val indexCaseSinceTestResultEndDateWales = 5
    private val testDateWindowDays = LocalDate.parse("2020-07-22")..LocalDate.parse("2020-07-27")
    private val testDateWindowDaysWales = LocalDate.parse("2020-07-23")..LocalDate.parse("2020-07-27")
    private val previouslySavedCannotRememberDate = LocalDate.now(fixedClock)
    private val previouslySavedTestDate = LocalDate.now(fixedClock).minus(1, DAYS)
    private val previouslySavedSymptomsDate = ChosenDate(true, LocalDate.now(fixedClock).minus(2, DAYS))
    private val defaultInitialState = ViewState(
        selectedTestDate = NotStated,
        hasError = false,
        testDateWindowDays = testDateWindowDays
    )

    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE, temporaryExposureKeys = listOf(NHSTemporaryExposureKey(key = "key", rollingStartNumber = 2)),
        RAPID_SELF_REPORTED, false, null, null, null, null)

    private val selfReportTestQuestionsWithExplicitTestDateAndSymptoms = selfReportTestQuestions.copy(testEndDate =
        ChosenDate(true, previouslySavedTestDate),
        hadSymptoms = true,
        symptomsOnsetDate = previouslySavedSymptomsDate
    )

    private val selfReportTestQuestionsWithCannotRememberTestDateAndSymptoms = selfReportTestQuestions.copy(testEndDate =
        ChosenDate(false, previouslySavedCannotRememberDate),
        hadSymptoms = true,
        symptomsOnsetDate = previouslySavedSymptomsDate
    )

    private val selfReportTestQuestionsWithOldCannotRememberTestDateAndSymptoms = selfReportTestQuestions.copy(testEndDate =
    ChosenDate(false, previouslySavedCannotRememberDate.minusDays(1)),
        hadSymptoms = true,
        symptomsOnsetDate = previouslySavedSymptomsDate
    )

    private val isolationHelper = IsolationLogicalHelper(fixedClock)

    private val acknowledgedTestResult = AcknowledgedTestResult(
        testEndDate = LocalDate.now(fixedClock),
        testResult = RelevantVirologyTestResult.POSITIVE,
        testKitType = mockk(),
        acknowledgedDate = mockk()
    )

    @Before
    fun setUp() {
        every { isolationStateMachine.readLogicalState().isolationConfiguration.indexCaseSinceTestResultEndDate } returns indexCaseSinceTestResultEndDateEngland
    }

    @Test
    fun `create and calculate testDateWindowDays england`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        val expectedViewState = ViewState(
            selectedTestDate = NotStated,
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `create and calculate testDateWindowDays wales`() = runBlocking {
        every { isolationStateMachine.readLogicalState().isolationConfiguration.indexCaseSinceTestResultEndDate } returns indexCaseSinceTestResultEndDateWales

        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        val expectedViewState = ViewState(
            selectedTestDate = NotStated,
            hasError = false,
            testDateWindowDays = testDateWindowDaysWales
        )

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `select date`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        val selectedDate = Instant.now(fixedClock).minus(1, DAYS)

        testSubject.onDateSelected(selectedDate.toEpochMilli())

        val expectedState = ViewState(
            selectedTestDate = ExplicitDate(selectedDate.toLocalDate(ZoneOffset.UTC)),
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
        }
    }

    @Test
    fun `select cannot remember date`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.cannotRememberDateChecked()

        val expectedState = ViewState(
            selectedTestDate = CannotRememberDate,
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
        }
    }

    @Test
    fun `unselect cannot remember date`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.cannotRememberDateUnchecked()

        val expectedState = ViewState(
            selectedTestDate = NotStated,
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        verify(exactly = 2) { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `user clicks continue without stating a date, error is shown`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onButtonContinueClicked()

        val expectedState = ViewState(
            selectedTestDate = NotStated,
            hasError = true,
            testDateWindowDays = testDateWindowDays
        )

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
        }
    }

    @Test
    fun `user clicks continue with setting an explicit date and is not in isolation, should navigate to symptoms`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        val selectedDate = Instant.now(fixedClock).minus(1, DAYS)

        testSubject.onDateSelected(selectedDate.toEpochMilli())

        val expectedState = ViewState(
            selectedTestDate = ExplicitDate(selectedDate.toLocalDate(ZoneOffset.UTC)),
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(Symptoms(selfReportTestQuestions.copy(testEndDate =
                ChosenDate(true, selectedDate.toLocalDate(ZoneOffset.UTC)))))
        }
    }

    @Test
    fun `user clicks continue with cannot remember date and is not in isolation, should navigate to symptoms`() = runBlocking {
        val testSubject = createDefaultTestSubject()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        testSubject.cannotRememberDateChecked()

        val expectedState = ViewState(
            selectedTestDate = CannotRememberDate,
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(Symptoms(selfReportTestQuestions.copy(testEndDate =
                ChosenDate(false, LocalDate.now(fixedClock)))))
        }
    }

    @Test
    fun `user clicks continue with setting an explicit date, is in isolation and has LFD from NHS, should navigate to report test`() = runBlocking {
        val selfReportTestQuestionsWithNHSLFD = selfReportTestQuestions.copy(isNHSTest = true, hadSymptoms = true)
        val testSubject = createTestSubject(selfReportTestQuestionsWithNHSLFD)

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        every { isolationStateMachine.readLogicalState() } returns
                isolationHelper.positiveTest(acknowledgedTestResult).asIsolation()

        val selectedDate = Instant.now(fixedClock).minus(1, DAYS)

        testSubject.onDateSelected(selectedDate.toEpochMilli())

        val expectedState = ViewState(
            selectedTestDate = ExplicitDate(selectedDate.toLocalDate(ZoneOffset.UTC)),
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(ReportedTest(selfReportTestQuestionsWithNHSLFD.copy(testEndDate =
            ChosenDate(true, selectedDate.toLocalDate(ZoneOffset.UTC)),
                hadSymptoms = null, symptomsOnsetDate = null)))
        }
    }

    @Test
    fun `user clicks continue with cannot remember date, is in isolation and has LFD from NHS, should navigate to report test`() = runBlocking {
        val selfReportTestQuestionsWithNHSLFD = selfReportTestQuestions.copy(isNHSTest = true, hadSymptoms = true)
        val testSubject = createTestSubject(selfReportTestQuestionsWithNHSLFD)

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        every { isolationStateMachine.readLogicalState() } returns
                isolationHelper.positiveTest(acknowledgedTestResult).asIsolation()

        testSubject.cannotRememberDateChecked()

        val expectedState = ViewState(
            selectedTestDate = CannotRememberDate,
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(ReportedTest(selfReportTestQuestionsWithNHSLFD.copy(testEndDate =
            ChosenDate(false, LocalDate.now(fixedClock)),
                hadSymptoms = null, symptomsOnsetDate = null)))
        }
    }

    @Test
    fun `user clicks continue with setting an explicit date, is in isolation and has PCR, should navigate to check answers`() = runBlocking {
        val selfReportTestQuestionsWithPCR = selfReportTestQuestions.copy(testKitType = LAB_RESULT, hadSymptoms = true)
        val testSubject = createTestSubject(selfReportTestQuestionsWithPCR)

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        every { isolationStateMachine.readLogicalState() } returns
                isolationHelper.positiveTest(acknowledgedTestResult).asIsolation()

        val selectedDate = Instant.now(fixedClock).minus(1, DAYS)

        testSubject.onDateSelected(selectedDate.toEpochMilli())

        val expectedState = ViewState(
            selectedTestDate = ExplicitDate(selectedDate.toLocalDate(ZoneOffset.UTC)),
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(CheckAnswers(selfReportTestQuestionsWithPCR.copy(testEndDate =
            ChosenDate(true, selectedDate.toLocalDate(ZoneOffset.UTC)),
                hadSymptoms = null, symptomsOnsetDate = null)))
        }
    }

    @Test
    fun `user clicks continue with cannot remember date, is in isolation and has PCR, should navigate to check answers`() = runBlocking {
        val selfReportTestQuestionsWithPCR = selfReportTestQuestions.copy(testKitType = LAB_RESULT, hadSymptoms = true)
        val testSubject = createTestSubject(selfReportTestQuestionsWithPCR)

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        every { isolationStateMachine.readLogicalState() } returns
                isolationHelper.positiveTest(acknowledgedTestResult).asIsolation()

        testSubject.cannotRememberDateChecked()

        val expectedState = ViewState(
            selectedTestDate = CannotRememberDate,
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(defaultInitialState)
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(CheckAnswers(selfReportTestQuestionsWithPCR.copy(testEndDate =
            ChosenDate(false, LocalDate.now(fixedClock)),
                hadSymptoms = null, symptomsOnsetDate = null)))
        }
    }

    @Test
    fun `on click of date picker container emit current day to parameterize data picker`() = runBlocking {
        val testSubject = createDefaultTestSubject()
        testSubject.datePickerContainerClicked().observeForever(datePickerContainerClickedObserver)

        testSubject.onDatePickerContainerClicked()

        verify { datePickerContainerClickedObserver.onChanged(LocalDate.now(fixedClock).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()) }
    }

    @Test
    fun `test date in future is invalid`() {
        val testSubject = createDefaultTestSubject()
        assertFalse(
            testSubject.isTestDateValid(
                Instant.now(fixedClock).plus(1, DAYS).toEpochMilli(),
                testDateWindowDays
            )
        )
    }

    @Test
    fun `test date today is valid`() {
        val testSubject = createDefaultTestSubject()
        assertTrue(
            testSubject.isTestDateValid(
                Instant.now(fixedClock).toEpochMilli(),
                testDateWindowDays
            )
        )
    }

    @Test
    fun `test date 5 days in past is valid`() {
        val testSubject = createDefaultTestSubject()
        assertTrue(
            testSubject.isTestDateValid(
                Instant.now(fixedClock).minus(5, DAYS).toEpochMilli(),
                testDateWindowDays
            )
        )
    }

    @Test
    fun `test date 6 days in past is invalid`() {
        val testSubject = createDefaultTestSubject()
        assertFalse(
            testSubject.isTestDateValid(
                Instant.now(fixedClock).minus(6, DAYS).toEpochMilli(),
                testDateWindowDays
            )
        )
    }

    @Test
    fun `on test date changed from previously saved explicit date to new explicit date and is not in isolation, symptoms date set to null`() {
        val testSubject = createTestSubjectWithPreviouslySavedExplicitTestDateAndSymptoms()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        val selectedDate = Instant.now(fixedClock)

        testSubject.onDateSelected(selectedDate.toEpochMilli())
        val expectedState = ViewState(
            selectedTestDate = ExplicitDate(selectedDate.toLocalDate(ZoneOffset.UTC)),
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(expectedState.copy(selectedTestDate = ExplicitDate(previouslySavedTestDate)))
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(Symptoms(selfReportTestQuestionsWithExplicitTestDateAndSymptoms.copy(testEndDate =
                ChosenDate(true, LocalDate.now(fixedClock)),
                hadSymptoms = true,
                symptomsOnsetDate = null
            )))
        }
    }

    @Test
    fun `on test date changed from previously saved cannotRemember date to new explicit date and is not in isolation, symptoms date set to null`() {
        val testSubject = createTestSubjectWithPreviouslySavedCannotRememberTestDateAndSymptoms()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        val selectedDate = Instant.now(fixedClock).minus(1, DAYS)

        testSubject.onDateSelected(selectedDate.toEpochMilli())
        val expectedState = ViewState(
            selectedTestDate = ExplicitDate(selectedDate.toLocalDate(ZoneOffset.UTC)),
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(expectedState.copy(selectedTestDate = CannotRememberDate))
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(Symptoms(selfReportTestQuestionsWithExplicitTestDateAndSymptoms.copy(
                testEndDate = ChosenDate(true, selectedDate.toLocalDate(ZoneOffset.UTC)),
                hadSymptoms = true,
                symptomsOnsetDate = null
            )))
        }
    }

    @Test
    fun `on test date changed from previously saved cannotRemember date to new cannotRemember date and is not in isolation, symptoms date set to null`() {
        val testSubject = createTestSubjectWithPreviouslySavedOldCannotRememberTestDateAndSymptoms()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        val expectedState = ViewState(
            selectedTestDate = CannotRememberDate,
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(Symptoms(selfReportTestQuestionsWithOldCannotRememberTestDateAndSymptoms.copy(
                testEndDate = ChosenDate(false, LocalDate.now(fixedClock)),
                hadSymptoms = true,
                symptomsOnsetDate = null
            )))
        }
    }

    @Test
    fun `previously saved explicit date unchanged and is not in isolation, symptoms date kept`() {
        val testSubject = createTestSubjectWithPreviouslySavedExplicitTestDateAndSymptoms()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        val expectedState = ViewState(
            selectedTestDate = ExplicitDate(previouslySavedTestDate),
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(Symptoms(selfReportTestQuestionsWithExplicitTestDateAndSymptoms))
        }
    }

    @Test
    fun `previously saved cannot remember date unchanged and is not in isolation, symptoms date kept`() {
        val testSubject = createTestSubjectWithPreviouslySavedCannotRememberTestDateAndSymptoms()

        testSubject.viewState().observeForever(viewStateObserver)
        testSubject.navigate().observeForever(navigationStateObserver)

        val expectedState = ViewState(
            selectedTestDate = CannotRememberDate,
            hasError = false,
            testDateWindowDays = testDateWindowDays
        )

        testSubject.onButtonContinueClicked()

        verifyOrder {
            viewStateObserver.onChanged(expectedState)
            navigationStateObserver.onChanged(Symptoms(selfReportTestQuestionsWithCannotRememberTestDateAndSymptoms))
        }
    }

    @Test
    fun `when back is pressed and test kit is LFD, should send user to test origin screen`() {
        val testSubject = createDefaultTestSubject()

        testSubject.navigate().observeForever(navigationStateObserver)

        testSubject.onBackPressed()

        val expectedState = TestOrigin(selfReportTestQuestions)
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when back is pressed and test kit is PCR, should send user to test kit Type screen`() {
        val testSubject = createDefaultTestSubjectWithPCR()

        testSubject.navigate().observeForever(navigationStateObserver)

        testSubject.onBackPressed()

        val expectedState = TestKitType(selfReportTestQuestions.copy(testKitType = LAB_RESULT))
        verify { navigationStateObserver.onChanged(expectedState) }
    }

    private fun createTestSubject(selfReportTestQuestions: SelfReportTestQuestions): SelectTestDateViewModel {
        return SelectTestDateViewModel(
            fixedClock,
            isolationStateMachine,
            selfReportTestQuestions
        )
    }

    private fun createDefaultTestSubject(): SelectTestDateViewModel {
        return SelectTestDateViewModel(
            fixedClock,
            isolationStateMachine,
            selfReportTestQuestions
        )
    }

    private fun createDefaultTestSubjectWithPCR(): SelectTestDateViewModel {
        return SelectTestDateViewModel(
            fixedClock,
            isolationStateMachine,
            selfReportTestQuestions.copy(testKitType = LAB_RESULT)
        )
    }

    private fun createTestSubjectWithPreviouslySavedExplicitTestDateAndSymptoms(): SelectTestDateViewModel {
        return SelectTestDateViewModel(
            fixedClock,
            isolationStateMachine,
            selfReportTestQuestionsWithExplicitTestDateAndSymptoms
        )
    }

    private fun createTestSubjectWithPreviouslySavedCannotRememberTestDateAndSymptoms(): SelectTestDateViewModel {
        return SelectTestDateViewModel(
            fixedClock,
            isolationStateMachine,
            selfReportTestQuestionsWithCannotRememberTestDateAndSymptoms
        )
    }

    private fun createTestSubjectWithPreviouslySavedOldCannotRememberTestDateAndSymptoms(): SelectTestDateViewModel {
        return SelectTestDateViewModel(
            fixedClock,
            isolationStateMachine,
            selfReportTestQuestionsWithOldCannotRememberTestDateAndSymptoms
        )
    }
}
