package uk.nhs.nhsx.covid19.android.app

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DecommissioningNotificationSentProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<Boolean>(VALUE_KEY)

    var value: Boolean? by prefs

    companion object {
        private const val VALUE_KEY = "DECOMMISSIONING_NOTIFICATION_SENT"
    }
}
