package uk.nhs.nhsx.covid19.android.app.battery

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryOptimizationAcknowledgementProvider @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<Long>(VALUE_KEY)

    var value: Long? by prefs

    companion object {
        private const val VALUE_KEY = "BATTERY_OPTIMIZATION_ACKNOWLEDGEMENT_TIME"
    }
}
