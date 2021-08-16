package uk.nhs.nhsx.covid19.android.app.common

import android.app.Activity
import android.app.Activity.RESULT_OK
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsViewModel.ActivationResult
import uk.nhs.nhsx.covid19.android.app.common.EnableExposureNotificationsViewModel.ActivationResult.Success
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper

class EnableExposureNotificationsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val exposureNotificationPermissionHelperFactory = mockk<ExposureNotificationPermissionHelper.Factory>()
    private val exposureNotificationPermissionHelper = mockk<ExposureNotificationPermissionHelper>(relaxUnitFun = true)
    private val permissionRequestObserver = mockk<Observer<(Activity) -> Unit>>(relaxUnitFun = true)
    private val activationResultObserver = mockk<Observer<ActivationResult>>(relaxUnitFun = true)

    private lateinit var testSubject: EnableExposureNotificationsViewModel

    @Before
    fun setUp() {
        every { exposureNotificationPermissionHelperFactory.create(any(), any()) } returns
            exposureNotificationPermissionHelper
        testSubject = EnableExposureNotificationsViewModel(exposureNotificationPermissionHelperFactory)
        testSubject.permissionRequest().observeForever(permissionRequestObserver)
        testSubject.activationResult().observeForever(activationResultObserver)
    }

    @Test
    fun `when exposure notifications activation was successful then emit activation result success`() {
        testSubject.onExposureNotificationsEnabled()

        verify { activationResultObserver.onChanged(Success) }
    }

    @Test
    fun `onPermissionRequired passes permission request to activity`() {
        val expectedPermissionRequest = mockk<(Activity) -> Unit>()
        testSubject.onPermissionRequired(expectedPermissionRequest)

        verify { permissionRequestObserver.onChanged(expectedPermissionRequest) }
    }

    @Test
    fun `onPermissionDenied does nothing`() {
        testSubject.onPermissionDenied()

        confirmVerified(exposureNotificationPermissionHelper, permissionRequestObserver, activationResultObserver)
    }

    @Test
    fun `when exposure notifications activation results in an error then emit activation result error`() {
        val throwable = mockk<Throwable>()
        val expectedMessage = "Test"
        every { throwable.message } returns expectedMessage

        testSubject.onError(throwable)

        verify { activationResultObserver.onChanged(ActivationResult.Error(expectedMessage)) }
    }

    @Test
    fun `when start exposure notifications button is clicked then trigger activation via ExposureNotificationPermissionHelper`() {
        testSubject.onEnableExposureNotificationsClicked()

        verify { exposureNotificationPermissionHelper.startExposureNotifications() }
    }

    @Test
    fun `onActivityResult delegates call to ExposureNotificationPermissionHelper`() {
        val expectedRequestCode = 123
        val expectedResultCode = RESULT_OK

        testSubject.onActivityResult(expectedRequestCode, expectedResultCode)

        verify { exposureNotificationPermissionHelper.onActivityResult(expectedRequestCode, expectedResultCode) }
    }
}
