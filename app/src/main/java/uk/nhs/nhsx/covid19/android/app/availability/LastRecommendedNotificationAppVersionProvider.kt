package uk.nhs.nhsx.covid19.android.app.availability

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class LastRecommendedNotificationAppVersionProvider @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<Int?>(LAST_RECOMMENDED_NOTIFICATION_APP_VERSION_KEY)

    var value: Int? by prefs

    companion object {
        private const val LAST_RECOMMENDED_NOTIFICATION_APP_VERSION_KEY =
            "LAST_RECOMMENDED_NOTIFICATION_APP_VERSION_KEY"
    }
}
