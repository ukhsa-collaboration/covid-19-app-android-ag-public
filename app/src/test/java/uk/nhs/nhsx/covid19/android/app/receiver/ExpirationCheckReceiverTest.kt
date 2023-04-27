package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Intent
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature

class ExpirationCheckReceiverTest : FieldInjectionUnitTest() {

    private val intent = mockk<Intent>()

    private val testSubject = ExpirationCheckReceiver().apply {
        displayStateExpirationNotification = mockk(relaxed = true)
    }

    @Test
    fun `onReceive display state expiration notification`() = runBlocking {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            testSubject.onReceive(context, intent)

            verify { testSubject.displayStateExpirationNotification.doWork() }
        }
    }

    @Test
    fun `onReceive in decommissioning state do not display state expiration notification`() = runBlocking {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {
            testSubject.onReceive(context, intent)

            verify(exactly = 0) { testSubject.displayStateExpirationNotification.doWork() }
        }
    }
}
