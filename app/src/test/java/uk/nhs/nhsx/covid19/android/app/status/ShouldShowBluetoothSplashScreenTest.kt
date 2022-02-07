package uk.nhs.nhsx.covid19.android.app.status

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldShowBluetoothSplashScreenTest {

    private val isDebug = true
    private val bluetoothAvailabilityStateProvider = mockk<AvailabilityStateProvider>()
    private val testSubject = ShouldShowBluetoothSplashScreen(bluetoothAvailabilityStateProvider)

    @Test
    fun `returns false if bluetooth is enabled`() {
        every { bluetoothAvailabilityStateProvider.getState(isDebug) } returns AvailabilityState.ENABLED

        val result = testSubject(isDebug)

        assertFalse(result)
        verify { bluetoothAvailabilityStateProvider.getState(isDebug) }
    }

    @Test
    fun `returns true if bluetooth is disabled and screen has not been shown`() {
        every { bluetoothAvailabilityStateProvider.getState(isDebug) } returns AvailabilityState.DISABLED

        val result = testSubject(isDebug)

        assertTrue(result)
        verify { bluetoothAvailabilityStateProvider.getState(isDebug) }
    }

    @Test
    fun `returns false if already has been shown`() {
        every { bluetoothAvailabilityStateProvider.getState(isDebug) } returns AvailabilityState.DISABLED

        testSubject.setHasBeenShown(true)
        val result = testSubject(isDebug)

        assertFalse(result)
        verify { bluetoothAvailabilityStateProvider.getState(isDebug) }
    }
}
