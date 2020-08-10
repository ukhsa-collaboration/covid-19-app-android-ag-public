package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class UpdateStatusStorage @Inject constructor(sharedPreferences: SharedPreferences) {

    private val updateStatusPrefs = sharedPreferences.with<Boolean>(UPDATE_STATUS_KEY)

    var value by updateStatusPrefs

    companion object {
        const val UPDATE_STATUS_KEY = "UPDATE_STATUS_KEY"
    }
}
