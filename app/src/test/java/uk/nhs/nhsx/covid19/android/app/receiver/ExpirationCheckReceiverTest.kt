package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Intent
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest

class ExpirationCheckReceiverTest : FieldInjectionUnitTest() {

    private val intent = mockk<Intent>()

    private val testSubject = ExpirationCheckReceiver().apply {
        displayStateExpirationNotification = mockk(relaxed = true)
    }

    @Test
    fun `onReceive display state expiration notification`() = runBlocking {
        testSubject.onReceive(context, intent)

        verify { testSubject.displayStateExpirationNotification.doWork() }
    }
}
