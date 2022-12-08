package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAppWillNotNotifyOtherUsersViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAppWillNotNotifyOtherUsersViewModel.NavigationTarget.ShareKeysInfo
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportAppWillNotNotifyOtherUsersViewModel.NavigationTarget.TestKit

class SelfReportAppWillNotNotifyOtherUsersViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val navigationObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val selfReportTestQuestions = SelfReportTestQuestions(POSITIVE, null, null,
        null, null, null, null, null)

    private val testSubject = SelfReportAppWillNotNotifyOtherUsersViewModel(selfReportTestQuestions)

    @Before
    fun setUp() {
        testSubject.navigate().observeForever(navigationObserver)
    }

    @Test
    fun `on click continue success navigates to TestKit flow`() {
        testSubject.onClickContinue()

        verify { navigationObserver.onChanged(TestKit(selfReportTestQuestions)) }
    }

    @Test
    fun `on back pressed success navigates to ShareKeysInfo flow`() {
        testSubject.onBackPressed()

        verify { navigationObserver.onChanged(ShareKeysInfo(selfReportTestQuestions)) }
    }
}
