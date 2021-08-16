package uk.nhs.nhsx.covid19.android.app.status.isolationhub

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class IsolationHubReminderTimeProvider @Inject constructor(
    sharedPreferences: SharedPreferences
) {
    private val prefs = sharedPreferences.with<Long>(VALUE_KEY)

    var value: Long? by prefs

    companion object {
        private const val VALUE_KEY = "ISOLATION_HUB_REMINDER_TIME"
    }
}
