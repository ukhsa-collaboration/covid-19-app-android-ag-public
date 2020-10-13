package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

class PostCodeRiskIndicatorChangedListener(private val onChange: () -> Unit) :
    OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != RiskyPostCodeIndicatorStorage.VALUE_KEY) {
            return
        }
        onChange()
    }
}
