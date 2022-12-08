package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.addTestResult
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasNotReportedIsolate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasNotReportedNoNeedToIsolate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasReportedIsolate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ResultAdvice.HasReportedNoNeedToIsolate
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAdviceViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class SelfReportAdviceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-27T01:00:00.00Z"), ZoneOffset.UTC)
    private val isolationStateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()
    private val isolationHelper = IsolationLogicalHelper(fixedClock)
    private val isolationConfiguration = IsolationConfiguration()
    val isolationState = isolationHelper.selfAssessment(onsetDate = LocalDate.now(fixedClock))
        .asIsolation(isolationConfiguration = isolationConfiguration).addTestResult(
        testResult = AcknowledgedTestResult(
            testEndDate = LocalDate.now(fixedClock),
            testResult = POSITIVE,
            testKitType = LAB_RESULT,
            acknowledgedDate = LocalDate.now(fixedClock)
        )
    )

    @Test
    fun `when not reported and not in isolation England`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND

        val testSubject = createTestSubject(false)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(HasNotReportedNoNeedToIsolate, ENGLAND)
        coVerify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when not reported and not in isolation Wales`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

        val testSubject = createTestSubject(false)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(HasNotReportedNoNeedToIsolate, WALES)
        coVerify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when reported and not in isolation England`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND

        val testSubject = createTestSubject(true)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(HasReportedNoNeedToIsolate, ENGLAND)
        coVerify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when reported and not in isolation Wales`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES

        val testSubject = createTestSubject(true)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(HasReportedNoNeedToIsolate, WALES)
        coVerify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when not reported and in isolation England`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
        every { isolationStateMachine.readLogicalState() } returns isolationState

        val testSubject = createTestSubject(false)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(HasNotReportedIsolate(currentDate = LocalDate.now(fixedClock),
            isolationEndDate = LocalDate.now(fixedClock).plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())), ENGLAND)
        coVerify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when not reported and in isolation Wales`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
        every { isolationStateMachine.readLogicalState() } returns isolationState

        val testSubject = createTestSubject(false)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(HasNotReportedIsolate(currentDate = LocalDate.now(fixedClock),
            isolationEndDate = LocalDate.now(fixedClock).plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())), WALES)
        coVerify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when reported and in isolation England`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
        every { isolationStateMachine.readLogicalState() } returns isolationState

        val testSubject = createTestSubject(true)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(
            HasReportedIsolate(currentDate = LocalDate.now(fixedClock),
            isolationEndDate = LocalDate.now(fixedClock).plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())), ENGLAND)
        coVerify { viewStateObserver.onChanged(expectedState) }
    }

    @Test
    fun `when reported and in isolation Wales`() = runBlocking {
        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
        every { isolationStateMachine.readLogicalState() } returns isolationState

        val testSubject = createTestSubject(true)
        testSubject.viewState().observeForever(viewStateObserver)

        val expectedState = ViewState(
            HasReportedIsolate(currentDate = LocalDate.now(fixedClock),
                isolationEndDate = LocalDate.now(fixedClock).plusDays(isolationConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong())), WALES)
        coVerify { viewStateObserver.onChanged(expectedState) }
    }

    private fun createTestSubject(hasReported: Boolean): SelfReportAdviceViewModel {
        return SelfReportAdviceViewModel(isolationStateMachine, fixedClock, localAuthorityPostCodeProvider, hasReported)
    }
}
