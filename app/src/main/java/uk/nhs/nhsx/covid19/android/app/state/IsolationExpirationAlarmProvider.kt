package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class IsolationExpirationAlarmProvider @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<Long>(ISOLATION_EXPIRATION_ALARM_TIME)

    var value: Long? by prefs

    companion object {
        private const val ISOLATION_EXPIRATION_ALARM_TIME = "ISOLATION_EXPIRATION_ALARM_TIME"
    }
}
