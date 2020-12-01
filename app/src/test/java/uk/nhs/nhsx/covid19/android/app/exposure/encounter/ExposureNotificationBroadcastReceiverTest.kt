package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient.EXTRA_TOKEN
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
        exposureNotificationsTokensProvider = mockk(relaxed = true)
        exposureNotificationWorkerScheduler = mockk(relaxed = true)
    }

    private val testToken = "test token"

    private val intent = mockk<Intent>(relaxed = true)

    @Before
    override fun setUp() {
        super.setUp()
        every { testSubject.exposureNotificationWorkerScheduler.schedule(context, any()) } returns Unit
    }

    @Test
    fun scheduleExposureCircuitBreakerInitialCalled() = runBlocking {
        every { intent.action } returns ACTION_EXPOSURE_STATE_UPDATED
        every { intent.getStringExtra(EXTRA_TOKEN) } returns testToken

        testSubject.onReceive(context, intent)

        verify { testSubject.exposureNotificationsTokensProvider.add(testToken) }
        verify { testSubject.exposureNotificationWorkerScheduler.schedule(context, testToken) }
    }

    @Test
    fun wrongActionScheduleExposureCircuitBreakerInitialNotCalled() {
        every { intent.action } returns any()
        every { intent.getStringExtra(EXTRA_TOKEN) } returns testToken

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.exposureNotificationsTokensProvider.add(testToken) }
        verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.schedule(context, testToken) }
    }
}
