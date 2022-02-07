package uk.nhs.nhsx.covid19.android.app.common.bluetooth

import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.status.ShouldShowBluetoothSplashScreen
import javax.inject.Inject

class EnableBluetoothViewModel @Inject constructor(
    private val shouldShowBluetoothSplashScreen: ShouldShowBluetoothSplashScreen
) : ViewModel() {

    fun onScreenShown() {
        shouldShowBluetoothSplashScreen.setHasBeenShown(true)
    }
}
