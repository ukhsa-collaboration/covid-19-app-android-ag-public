package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class AreaRiskChangedProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<Boolean>(VALUE_KEY)

    var value: Boolean? by prefs

    companion object {
        const val VALUE_KEY = "AREA_RISK_KEY"
    }
}
