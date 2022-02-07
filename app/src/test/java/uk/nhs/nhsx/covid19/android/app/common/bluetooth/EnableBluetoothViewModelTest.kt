package uk.nhs.nhsx.covid19.android.app.common.bluetooth

import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.status.ShouldShowBluetoothSplashScreen

class EnableBluetoothViewModelTest {

    private val shouldShowBluetoothSplashScreen = mockk<ShouldShowBluetoothSplashScreen>(relaxUnitFun = true)
    private val testSubject = EnableBluetoothViewModel(shouldShowBluetoothSplashScreen)

    @Test
    fun `onScreenShown store flag in shouldShowBluetoothSplashScreen`() {
        testSubject.onScreenShown()

        verify { shouldShowBluetoothSplashScreen.setHasBeenShown(true) }
        confirmVerified(shouldShowBluetoothSplashScreen)
    }
}
