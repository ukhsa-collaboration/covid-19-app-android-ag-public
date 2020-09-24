package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

class AreaRiskLevelPreferenceChangedListener(private val onChange: () -> Unit) :
    OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != AreaRiskLevelProvider.VALUE_KEY) {
            return
        }
        onChange()
    }
}
