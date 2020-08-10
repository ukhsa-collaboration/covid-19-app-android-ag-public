package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

class AreaRiskPreferenceChangedListener(private val onChange: () -> Unit) : OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != AreaRiskChangedProvider.VALUE_KEY) {
            return
        }
        val hasAreaRiskChanged = sharedPreferences?.getBoolean(key, false) ?: false

        if (hasAreaRiskChanged) {
            onChange()
        }
    }
}
