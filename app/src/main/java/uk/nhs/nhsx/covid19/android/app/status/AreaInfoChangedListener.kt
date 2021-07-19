package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import uk.nhs.nhsx.covid19.android.app.status.localmessage.LocalMessagesProvider

class AreaInfoChangedListener(private val onChange: () -> Unit) :
    OnSharedPreferenceChangeListener {

    private val areaInfoKeys = setOf(
        RiskyPostCodeIndicatorStorage.VALUE_KEY,
        LocalMessagesProvider.VALUE_KEY
    )

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key in areaInfoKeys) {
            onChange()
        }
    }
}
