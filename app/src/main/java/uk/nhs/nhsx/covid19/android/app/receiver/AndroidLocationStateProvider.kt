package uk.nhs.nhsx.covid19.android.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.distinctUntilChanged
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED

class AndroidLocationStateProvider(val applicationContext: Context) : AvailabilityStateProvider,
    BroadcastReceiver() {
    private val locationStateMutable = MutableLiveData<AvailabilityState>()

    override val availabilityState: LiveData<AvailabilityState> = distinctUntilChanged(locationStateMutable)

    override fun start(context: Context) {
        locationStateMutable.postValue(determineLocationAvailabilityState())
        context.registerReceiver(this, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    override fun stop(context: Context) {
        context.unregisterReceiver(this)
    }

    override fun getState(isDebug: Boolean): AvailabilityState {
        return determineLocationAvailabilityState()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun determineLocationAvailabilityState() =
        if (isLocationEnabled()) ENABLED else DISABLED

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LocationManager.PROVIDERS_CHANGED_ACTION) {
            return
        }
        locationStateMutable.postValue(determineLocationAvailabilityState())
    }
}
