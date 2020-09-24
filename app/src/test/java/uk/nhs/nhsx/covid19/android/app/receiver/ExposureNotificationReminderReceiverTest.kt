package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Intent
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest

class ExposureNotificationReminderReceiverTest : FieldInjectionUnitTest() {

    private val testSubject = ExposureNotificationReminderReceiver().apply {
        notificationProvider = mockk(relaxed = true)
        resumeContactTracingNotificationTimeProvider = mockk(relaxed = true)
    }

    private val intent = mockk<Intent>()

    @Test
    fun `on receive triggers exposure notification reminder`() = runBlocking {
        testSubject.onReceive(context, intent)

        verify { testSubject.notificationProvider.showExposureNotificationReminder() }
    }
}
