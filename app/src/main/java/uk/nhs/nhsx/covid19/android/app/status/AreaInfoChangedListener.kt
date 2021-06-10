package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import uk.nhs.nhsx.covid19.android.app.status.localmessage.LocalMessagesStorage

class AreaInfoChangedListener(private val onChange: () -> Unit) :
    OnSharedPreferenceChangeListener {

    private val areaInfoKeys = setOf(
        RiskyPostCodeIndicatorStorage.VALUE_KEY,
        LocalMessagesStorage.VALUE_KEY
    )

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key in areaInfoKeys) {
            onChange()
        }
    }
}
