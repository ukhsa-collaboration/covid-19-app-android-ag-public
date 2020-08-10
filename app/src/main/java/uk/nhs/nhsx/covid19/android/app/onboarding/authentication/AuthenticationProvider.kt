package uk.nhs.nhsx.covid19.android.app.onboarding.authentication

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    fun isAuthenticated() = value ?: false

    private val prefs = sharedPreferences.with<Boolean>(VALUE_KEY)

    var value: Boolean? by prefs

    companion object {
        private const val VALUE_KEY = "AUTHENTICATED"
    }
}
