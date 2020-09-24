package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class LastAppRatingStartedDateProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<Long>(LAST_APP_RATING_STARTED_DATE_KEY)

    var value by prefs

    companion object {
        const val LAST_APP_RATING_STARTED_DATE_KEY = "LAST_APP_RATING_STARTED_DATE_KEY"
    }
}
