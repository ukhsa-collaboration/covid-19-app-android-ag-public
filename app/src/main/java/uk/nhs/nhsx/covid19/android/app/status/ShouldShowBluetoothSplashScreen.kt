package uk.nhs.nhsx.covid19.android.app.status

import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ShouldShowBluetoothSplashScreen @Inject constructor(
    @Named(AppModule.BLUETOOTH_STATE_NAME) private val bluetoothAvailabilityStateProvider: AvailabilityStateProvider
) {
    private var hasBeenShown = false

    operator fun invoke(isDebug: Boolean = BuildConfig.DEBUG): Boolean {
        return bluetoothAvailabilityStateProvider.getState(isDebug) == DISABLED && !hasBeenShown
    }

    fun setHasBeenShown(hasBeenShown: Boolean) {
        this.hasBeenShown = hasBeenShown
    }
}
