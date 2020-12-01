package uk.nhs.nhsx.covid19.android.app.battery

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.util.SingleLiveEvent
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class BatteryOptimizationViewModel @Inject constructor(
    private val batteryOptimizationAcknowledgementProvider: BatteryOptimizationAcknowledgementProvider,
    private val batteryOptimizationRequired: BatteryOptimizationRequired,
    private val clock: Clock
) : ViewModel() {

    private val acknowledgeLiveData = SingleLiveEvent<Void>()
    fun onAcknowledge(): LiveData<Void> = acknowledgeLiveData

    fun onCreate() {
        if (!batteryOptimizationRequired()) {
            onIgnoreBatteryOptimizationAcknowledged()
        }
    }

    fun onIgnoreBatteryOptimizationAcknowledged() {
        batteryOptimizationAcknowledgementProvider.value = Instant.now(clock).toEpochMilli()
        acknowledgeLiveData.postCall()
    }
}
