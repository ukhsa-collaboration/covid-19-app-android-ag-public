package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Intent
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest

class ExposureNotificationRetryReceiverTest : FieldInjectionUnitTest() {

    private val testSubject = ExposureNotificationRetryReceiver().apply {
        exposureNotificationRetryAlarmController = mockk(relaxUnitFun = true)
    }

    private val intent = mockk<Intent>()

    @Test
    fun `onReceive call notify exposureNotificationRetryAlarmController`() {
        testSubject.onReceive(context, intent)

        verify(exactly = 1) { testSubject.exposureNotificationRetryAlarmController.onAlarmTriggered() }
    }
}
