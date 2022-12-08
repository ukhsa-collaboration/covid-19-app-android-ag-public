package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelfReportedNegativeSelfLFDTestResultEnteredManually
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelfReportedVoidSelfLFDTestResultEnteredManually
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportNegativeVoidTestResultViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportNegativeVoidTestResultViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportNegativeVoidTestResultViewModel.NavigationTarget.Status

class SelfReportNegativeVoidTestResultViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)
    private val navigationStateObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val localAuthorityPostCodeProvider = mockk<LocalAuthorityPostCodeProvider>()
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxUnitFun = true)

    @Test
    fun `when LA is updated to England, verify that view state is also updated`() {
        val testSubject = SelfReportNegativeVoidTestResultViewModel(isNegative = true, localAuthorityPostCodeProvider = localAuthorityPostCodeProvider, analyticsEventProcessor = analyticsEventProcessor)
        testSubject.viewState().observeForever(viewStateObserver)

        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
        testSubject.fetchCountry()
        verify { viewStateObserver.onChanged(ViewState(ENGLAND)) }
    }

    @Test
    fun `when LA is updated to Wales, verify that view state is also updated`() {
        val testSubject = SelfReportNegativeVoidTestResultViewModel(isNegative = true, localAuthorityPostCodeProvider = localAuthorityPostCodeProvider, analyticsEventProcessor = analyticsEventProcessor)
        testSubject.viewState().observeForever(viewStateObserver)

        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
        testSubject.fetchCountry()
        verify { viewStateObserver.onChanged(ViewState(WALES)) }
    }

    @Test
    fun `when Back to Home button is pressed, verify that navigation state is correct`() {
        val testSubject = SelfReportNegativeVoidTestResultViewModel(isNegative = true, localAuthorityPostCodeProvider = localAuthorityPostCodeProvider, analyticsEventProcessor = analyticsEventProcessor)
        testSubject.navigate().observeForever(navigationStateObserver)

        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns WALES
        testSubject.fetchCountry()

        testSubject.onClickBackToHome()
        verify { navigationStateObserver.onChanged(Status) }
    }

    @Test
    fun `when Back to Home button is pressed and test result is Negative, verify that negative result analytics is tracked`() {
        val testSubject = SelfReportNegativeVoidTestResultViewModel(isNegative = true, localAuthorityPostCodeProvider = localAuthorityPostCodeProvider, analyticsEventProcessor = analyticsEventProcessor)
        testSubject.navigate().observeForever(navigationStateObserver)

        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
        testSubject.fetchCountry()

        testSubject.onClickBackToHome()
        verify(exactly = 1) { analyticsEventProcessor.track(SelfReportedNegativeSelfLFDTestResultEnteredManually) }
    }

    @Test
    fun `when Back to Home button is pressed and test result is Void, verify that void result analytics is tracked`() {
        val testSubject = SelfReportNegativeVoidTestResultViewModel(isNegative = false, localAuthorityPostCodeProvider = localAuthorityPostCodeProvider, analyticsEventProcessor = analyticsEventProcessor)
        testSubject.navigate().observeForever(navigationStateObserver)

        coEvery { localAuthorityPostCodeProvider.requirePostCodeDistrict() } returns ENGLAND
        testSubject.fetchCountry()

        testSubject.onClickBackToHome()
        verify(exactly = 1) { analyticsEventProcessor.track(SelfReportedVoidSelfLFDTestResultEnteredManually) }
    }
}
