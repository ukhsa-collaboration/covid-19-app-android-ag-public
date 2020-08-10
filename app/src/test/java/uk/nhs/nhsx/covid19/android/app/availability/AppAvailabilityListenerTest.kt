package uk.nhs.nhsx.covid19.android.app.availability

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class AppAvailabilityListenerTest {

    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()

    private val testSubject =
        AppAvailabilityListener(
            appAvailabilityProvider
        )

    @Test
    fun `trigger show availability screen when the current version is not supported`() {
        every { appAvailabilityProvider.isAppAvailable() } returns false
        val triggerResult = testSubject.shouldShowAvailabilityScreen()

        assertEquals(true, triggerResult)
    }

    @Test
    fun `do not trigger show availability screen when the current version is supported`() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        val triggerResult = testSubject.shouldShowAvailabilityScreen()

        assertEquals(false, triggerResult)
    }
}
