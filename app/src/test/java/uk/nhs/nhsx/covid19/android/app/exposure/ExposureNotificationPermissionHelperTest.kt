package uk.nhs.nhsx.covid19.android.app.exposure

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import com.google.android.gms.common.api.Status
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper.Callback

class ExposureNotificationPermissionHelperTest {
    private val callback = mockk<Callback>(relaxUnitFun = true)
    private val exposureNotificationManager = mockk<ExposureNotificationManager>()
    private val coroutineScope = TestCoroutineScope()
    private val testSubject =
        spyk(ExposureNotificationPermissionHelper(callback, exposureNotificationManager, coroutineScope))

    @Test
    fun `onActivityResult calls startExposureNotifications when user consented`() {
        testSubject.onActivityResult(
            ExposureNotificationPermissionHelper.REQUEST_CODE_START_EXPOSURE_NOTIFICATION,
            RESULT_OK
        )
        verify { testSubject.startExposureNotifications() }
    }

    @Test
    fun `onActivityResult does not call startExposureNotifications if request code does not indicate starting exposure notifications`() {
        val unexpectedRequestCode = 1234

        testSubject.onActivityResult(unexpectedRequestCode, RESULT_OK)

        verify(exactly = 0) { testSubject.startExposureNotifications() }
    }

    @Test
    fun `onActivityResult does not call startExposureNotifications if result code is not ok`() {
        testSubject.onActivityResult(
            ExposureNotificationPermissionHelper.REQUEST_CODE_START_EXPOSURE_NOTIFICATION,
            RESULT_CANCELED
        )
        verify(exactly = 0) { testSubject.startExposureNotifications() }
    }

    @Test
    fun `invokes callback's onExposureNotificationsEnabled if activation was successful`() {
        coEvery { exposureNotificationManager.isEnabled() } returns true

        testSubject.startExposureNotifications()

        verify { callback.onExposureNotificationsEnabled() }

        confirmVerified(callback)
    }

    @Test
    fun `invokes callback's onPermissionRequired if activation responds with ResolutionRequired`() {
        val status = mockk<Status>(relaxUnitFun = true)

        coEvery { exposureNotificationManager.isEnabled() } returns false
        coEvery { exposureNotificationManager.startExposureNotifications() } returns
            ExposureNotificationActivationResult.ResolutionRequired(status)

        testSubject.startExposureNotifications()

        val slot = slot<(Activity) -> Unit>()
        verify { callback.onPermissionRequired(capture(slot)) }

        confirmVerified(callback)

        val activity = mockk<Activity>()
        slot.captured.invoke(activity)

        verify {
            status.startResolutionForResult(
                activity,
                ExposureNotificationPermissionHelper.REQUEST_CODE_START_EXPOSURE_NOTIFICATION
            )
        }
    }

    @Test
    fun `invokes callback's onError if activation responds with Error`() {
        val exception = mockk<Exception>()

        coEvery { exposureNotificationManager.isEnabled() } returns false
        coEvery { exposureNotificationManager.startExposureNotifications() } returns
            ExposureNotificationActivationResult.Error(exception)

        testSubject.startExposureNotifications()

        verify { callback.onError(exception) }

        confirmVerified(callback)
    }
}
