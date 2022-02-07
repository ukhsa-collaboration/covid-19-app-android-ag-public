package uk.nhs.nhsx.covid19.android.app.status

import android.os.Bundle
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.EnableLocationActivity
import uk.nhs.nhsx.covid19.android.app.di.module.AppModule.Companion.LOCATION_STATE_NAME
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import javax.inject.Inject
import javax.inject.Named

abstract class StatusBaseActivity : BaseActivity() {

    @Inject
    @Named(LOCATION_STATE_NAME)
    lateinit var locationStateProvider: AvailabilityStateProvider

    @Inject
    lateinit var exposureNotificationApi: ExposureNotificationApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)

        if (!exposureNotificationApi.deviceSupportsLocationlessScanning()) {
            locationStateProvider.availabilityState.observe(
                this, { state ->
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
        locationStateProvider.start(this)
    }

    override fun onPause() {
        super.onPause()
        locationStateProvider.stop(this)
    }
}
