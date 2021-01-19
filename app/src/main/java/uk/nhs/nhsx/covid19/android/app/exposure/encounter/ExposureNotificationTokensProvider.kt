package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.SharedPreferences
import javax.inject.Inject
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with

@Deprecated("This class should not be used anymore.")
class ExposureNotificationTokensProvider @Inject constructor(
    private val exposureNotificationTokensStorage: ExposureNotificationTokensStorage
) {

    fun clear() {
        exposureNotificationTokensStorage.value = null
    }
}

class ExposureNotificationTokensStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "EXPOSURE_NOTIFICATION_TOKENS_KEY"
    }
}
