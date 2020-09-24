package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCodeProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        private const val VALUE_KEY = "MAIN_POST_CODE"
    }
}
