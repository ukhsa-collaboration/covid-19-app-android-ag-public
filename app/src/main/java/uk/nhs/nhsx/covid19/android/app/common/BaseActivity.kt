package uk.nhs.nhsx.covid19.android.app.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.BLUETOOTH_STATE_NAME
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.LOCATION_STATE_NAME
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationManager
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import javax.inject.Inject
import javax.inject.Named

abstract class BaseActivity(contentView: Int) : AppCompatActivity(contentView) {

    @Inject
    lateinit var exposureNotificationManager: ExposureNotificationManager

    @Inject
    @Named(BLUETOOTH_STATE_NAME)
    lateinit var bluetoothStateProvider: AvailabilityStateProvider

    @Inject
    @Named(LOCATION_STATE_NAME)
    lateinit var locationStateProvider: AvailabilityStateProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        bluetoothStateProvider.availabilityState.observe(
            this,
            Observer { state ->
                if (state == DISABLED) {
                    EnableBluetoothActivity.start(this)
                }
            }
        )

        locationStateProvider.availabilityState.observe(
            this,
            Observer { state ->
                if (state == DISABLED) {
                    EnableLocationActivity.start(this)
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        bluetoothStateProvider.start(this)
        locationStateProvider.start(this)
    }

    override fun onPause() {
        super.onPause()
        bluetoothStateProvider.stop(this)
        locationStateProvider.stop(this)
    }
}
