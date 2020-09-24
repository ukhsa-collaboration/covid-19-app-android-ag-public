package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest

class UpdateReceiverTest : FieldInjectionUnitTest() {

    private val testSubject = UpdateReceiver().apply {
        updateStatusStorage = mockk(relaxed = true)
    }

    private val intent = mockk<Intent>(relaxed = true)

    @Test
    fun `on receive with action ACTION_MY_PACKAGE_REPLACED sets update status storage true`() {
        every { intent.action } returns Intent.ACTION_PACKAGE_REPLACED

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.updateStatusStorage setProperty "value" value eq(true) }
    }

    @Test
    fun `on receive with action other than ACTION_MY_PACKAGE_REPLACED has no side-effects`() {
        every { intent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED

        testSubject.onReceive(context, intent)

        verify { testSubject.updateStatusStorage setProperty "value" value eq(true) }
    }
}
