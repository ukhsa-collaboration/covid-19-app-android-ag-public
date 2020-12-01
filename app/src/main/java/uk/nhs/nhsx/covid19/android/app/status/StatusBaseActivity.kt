package uk.nhs.nhsx.covid19.android.app.status

import android.os.Bundle
import androidx.lifecycle.Observer
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableBluetoothActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableLocationActivity
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.BLUETOOTH_STATE_NAME
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.LOCATION_STATE_NAME
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import javax.inject.Inject
import javax.inject.Named

abstract class StatusBaseActivity(contentView: Int) : BaseActivity(contentView) {

    @Inject
    @Named(BLUETOOTH_STATE_NAME)
    lateinit var bluetoothStateProvider: AvailabilityStateProvider

    @Inject
    @Named(LOCATION_STATE_NAME)
    lateinit var locationStateProvider: AvailabilityStateProvider

    @Inject
    lateinit var exposureNotificationApi: ExposureNotificationApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        bluetoothStateProvider.availabilityState.observe(
            this,
            Observer { state ->
                if (state == DISABLED) {
                    EnableBluetoothActivity.start(
                        this
                    )
                }
            }
        )

        if (!exposureNotificationApi.deviceSupportsLocationlessScanning()) {
            locationStateProvider.availabilityState.observe(
                this,
                Observer { state ->
                    if (state == DISABLED) {
                        EnableLocationActivity.start(
                            this
                        )
                    }
                }
            )
        }
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
