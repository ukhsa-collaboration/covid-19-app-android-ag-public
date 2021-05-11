package uk.nhs.nhsx.covid19.android.app.exposure

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Error
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.ResolutionRequired
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationActivationResult.Success
import uk.nhs.nhsx.covid19.android.app.notifications.ExposureNotificationReminderAlarmController
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExposureNotificationManagerTest {

    private val exposureNotificationApi = mockk<ExposureNotificationApi>(relaxed = true)
    private val exposureNotificationReminderAlarmController =
        mockk<ExposureNotificationReminderAlarmController>(relaxUnitFun = true)

    private val testSubject =
        ExposureNotificationManager(exposureNotificationApi, exposureNotificationReminderAlarmController)

    @Before
    fun setUp() {
        coEvery { exposureNotificationApi.isEnabled() } returns false
    }

    @Test
    fun `startExposureNotifications calls start and cancels pending reminders`() = runBlocking {
        coEvery { exposureNotificationApi.start() } returns Unit

        testSubject.startExposureNotifications()

        coVerify { exposureNotificationApi.start() }
        verify { exposureNotificationReminderAlarmController.cancel() }
    }

    @Test
    fun `startExposureNotifications cancels pending reminders and returns Success`() = runBlocking {
        coEvery { exposureNotificationApi.start() } returns Unit

        val result = testSubject.startExposureNotifications()

        assertEquals(Success, result)

        verify { exposureNotificationReminderAlarmController.cancel() }
    }

    @Test
    fun `startExposureNotifications returns ResolutionRequired if error is recoverable`() =
        runBlocking {
            val apiException = mockk<ApiException>()
            every { apiException.statusCode } returns ExposureNotificationStatusCodes.RESOLUTION_REQUIRED
            val status = mockk<Status>()
            every { status.hasResolution() } returns true
            every { apiException.status } returns status
            coEvery { exposureNotificationApi.start() } throws apiException

            val result = testSubject.startExposureNotifications()

            assertThat(result, instanceOf(ResolutionRequired::class.java))
            assertEquals(status, (result as ResolutionRequired).status)

            verify(exactly = 0) { exposureNotificationReminderAlarmController.cancel() }
        }

    @Test
    fun `startExposureNotifications returns Error if error is not recoverable`() =
        runBlocking {
            val apiException = mockk<ApiException>()
            every { apiException.statusCode } returns ExposureNotificationStatusCodes.NETWORK_ERROR
            val status = Status.RESULT_CANCELED
            every { apiException.status } returns status
            coEvery { exposureNotificationApi.start() } throws apiException

            val result = testSubject.startExposureNotifications()

            assertThat(result, instanceOf(Error::class.java))
            assertEquals(apiException, (result as Error).exception)

            verify(exactly = 0) { exposureNotificationReminderAlarmController.cancel() }
        }

    @Test
    fun `onStopExposureNotifications calls stop`() = runBlocking {
        testSubject.stopExposureNotifications()

        coVerify { exposureNotificationApi.stop() }
    }

    @Test
    fun `isEnabled returns true if ExposureNotificationApi returns true`() = runBlocking {
        coEvery { exposureNotificationApi.isEnabled() } returns true

        val result = testSubject.isEnabled()

        assertTrue { result }
    }

    @Test
    fun `isEnabled returns false if ExposureNotificationApi returns false`() = runBlocking {
        coEvery { exposureNotificationApi.isEnabled() } returns false

        val result = testSubject.isEnabled()

        assertFalse { result }
    }
}
