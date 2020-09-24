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

class AndroidLocationStateProvider : AvailabilityStateProvider,
    BroadcastReceiver() {
    private val locationStateMutable = MutableLiveData<AvailabilityState>()

    override val availabilityState: LiveData<AvailabilityState> = distinctUntilChanged(locationStateMutable)

    override fun start(context: Context) {
        locationStateMutable.postValue(determineLocationAvailabilityState(context))
        context.registerReceiver(this, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    override fun stop(context: Context) {
        context.unregisterReceiver(this)
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun determineLocationAvailabilityState(context: Context) =
        if (isLocationEnabled(context)) ENABLED else DISABLED

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LocationManager.PROVIDERS_CHANGED_ACTION) {
            return
        }
        locationStateMutable.postValue(determineLocationAvailabilityState(context))
    }
}
