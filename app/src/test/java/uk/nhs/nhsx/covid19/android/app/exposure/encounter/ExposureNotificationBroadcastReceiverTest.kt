package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient.ACTION_EXPOSURE_NOT_FOUND
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest

class ExposureNotificationBroadcastReceiverTest : FieldInjectionUnitTest() {

    private val testSubject = ExposureNotificationBroadcastReceiver().apply {
        exposureNotificationWorkerScheduler = mockk(relaxed = true)
        isolationStateMachine = mockk(relaxed = true)
    }

    private val intent = mockk<Intent>(relaxed = true)

    @Before
    override fun setUp() {
        super.setUp()
        every { testSubject.exposureNotificationWorkerScheduler.scheduleProcessNewExposure(context) } returns Unit
        every { testSubject.exposureNotificationWorkerScheduler.scheduleNoMatchesFound(context) } returns Unit
    }

    @Test
    fun `schedules matches found handler if a match is found and interested in exposure notifications`() = runBlocking {
        every { testSubject.isolationStateMachine.isInterestedInExposureNotifications() } returns true
        every { intent.action } returns ACTION_EXPOSURE_STATE_UPDATED

        testSubject.onReceive(context, intent)

        verify { testSubject.exposureNotificationWorkerScheduler.scheduleProcessNewExposure(context) }
        verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleNoMatchesFound(context) }
    }

    @Test
    fun `schedules no matches found handler if a match is found but not interested in exposure notifications`() = runBlocking {
        every { testSubject.isolationStateMachine.isInterestedInExposureNotifications() } returns false
        every { intent.action } returns ACTION_EXPOSURE_STATE_UPDATED

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleProcessNewExposure(context) }
        verify { testSubject.exposureNotificationWorkerScheduler.scheduleNoMatchesFound(context) }
    }

    @Test
    fun `schedules no matches found handler if no match is found`() {
        every { intent.action } returns ACTION_EXPOSURE_NOT_FOUND

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleProcessNewExposure(context) }
        verify { testSubject.exposureNotificationWorkerScheduler.scheduleNoMatchesFound(context) }
    }

    @Test
    fun `skips on wrong action`() {
        every { intent.action } returns any()

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleProcessNewExposure(context) }
        verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleNoMatchesFound(context) }
    }
}
