package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Intent
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest

class ExposureNotificationReminderReceiverTest : FieldInjectionUnitTest() {

    private val testSubject = ExposureNotificationReminderReceiver().apply {
        notificationProvider = mockk(relaxUnitFun = true)
        scheduleContactTracingActivationAdditionalReminderIfNeeded = mockk(relaxUnitFun = true)
        exposureNotificationApi = mockk(relaxUnitFun = true)
    }

    private val intent = mockk<Intent>()

    @Test
    fun `on receive triggers reminder notification and schedules additional reminder if exposure notifications are disabled`() =
        runBlocking {
            coEvery { testSubject.exposureNotificationApi.isEnabled() } returns false
            testSubject.onReceive(context, intent)
            verify {
                testSubject.notificationProvider.showExposureNotificationReminder()
                testSubject.scheduleContactTracingActivationAdditionalReminderIfNeeded()
            }
        }

    @Test
    fun `on receive does not trigger reminder notification and schedules additional reminder if exposure notifications are enabled`() =
        runBlocking {
            coEvery { testSubject.exposureNotificationApi.isEnabled() } returns true
            testSubject.onReceive(context, intent)
            verify(inverse = true) {
                testSubject.notificationProvider.showExposureNotificationReminder()
                testSubject.scheduleContactTracingActivationAdditionalReminderIfNeeded()
            }
        }
}
