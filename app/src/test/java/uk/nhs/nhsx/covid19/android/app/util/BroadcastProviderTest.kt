package uk.nhs.nhsx.covid19.android.app.util

import android.app.PendingIntent
import android.content.Context
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.receiver.ExposureNotificationRetryReceiver

class BroadcastProviderTest {

    private val testSubject = BroadcastProvider()

    private val context = mockk<Context>()
    private val requestCode = 1
    private val clazz = ExposureNotificationRetryReceiver::class.java
    private val flags = PendingIntent.FLAG_UPDATE_CURRENT

    @Test
    fun `passes all arguments to PendingIntent's getBroadcast`() {
        mockkStatic(PendingIntent::class)

        testSubject.getBroadcast(context, requestCode, clazz, flags)

        verify { PendingIntent.getBroadcast(context, requestCode, any(), flags) }
    }
}
