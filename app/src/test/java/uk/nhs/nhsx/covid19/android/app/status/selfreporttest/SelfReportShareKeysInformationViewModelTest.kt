package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.app.Activity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.FetchKeysFlow
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.FetchKeysFlow.Factory
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportShareKeysInformationViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportShareKeysInformationViewModel.NavigationTarget.DeclinedKeySharing
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportShareKeysInformationViewModel.NavigationTarget.SharedKeys

class SelfReportShareKeysInformationViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val fetchKeysFlowFactory = mockk<Factory>()
    private val fetchKeysFlow = mockk<FetchKeysFlow>(relaxUnitFun = true)
    private val navigationObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)
    private val permissionRequestObserver = mockk<Observer<(Activity) -> Unit>>(relaxUnitFun = true)
    private val selfReportTestQuestions = SelfReportTestQuestions(POSITIVE, null, null,
        null, null, null, null, null)

    private val testSubject = SelfReportShareKeysInformationViewModel(
        fetchKeysFlowFactory,
        questions = selfReportTestQuestions
    )

    @Before
    fun setUp() {
        every { fetchKeysFlowFactory.create(any(), any(), any()) } returns fetchKeysFlow
        testSubject.navigate().observeForever(navigationObserver)
        testSubject.permissionRequest().observeForever(permissionRequestObserver)
    }

    @Test
    fun `when user chooses to submit keys then fetch keys flow is called`() {
        testSubject.onClickContinue()

        verify { fetchKeysFlow() }
    }

    @Test
    fun `onActivityResult the fetchKeysFlow onActivityResult is called`() {
        val unexpectedRequestCode = 1234

        testSubject.onActivityResult(unexpectedRequestCode, Activity.RESULT_OK)

        verify { fetchKeysFlow.onActivityResult(unexpectedRequestCode, Activity.RESULT_OK) }
    }

    @Test
    fun `on fetch keys success navigates to SharedKeys flow`() {
        val keys = emptyList<NHSTemporaryExposureKey>()
        val token = null

        testSubject.onFetchKeysSuccess(keys, token)

        verify { navigationObserver.onChanged(SharedKeys(selfReportTestQuestions.copy(temporaryExposureKeys = keys))) }
    }

    @Test
    fun `on fetch keys permission denied with old acknowledgement`() {
        testSubject.onFetchKeysPermissionDenied()

        verify { navigationObserver.onChanged(DeclinedKeySharing(selfReportTestQuestions)) }
    }

    @Test
    fun `on fetch keys unexpected error`() {
        testSubject.onFetchKeysUnexpectedError()

        verify { navigationObserver.onChanged(DeclinedKeySharing(selfReportTestQuestions)) }
    }

    @Test
    fun `on fetch keys permission required`() {
        val permissionRequest: (Activity) -> Unit = { }

        testSubject.onPermissionRequired(permissionRequest)

        verify { permissionRequestObserver.onChanged(permissionRequest) }
    }
}
