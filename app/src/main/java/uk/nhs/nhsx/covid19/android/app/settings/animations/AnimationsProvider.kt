package uk.nhs.nhsx.covid19.android.app.settings.animations

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class AnimationsProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<Boolean>(VALUE_KEY)

    private var _inAppAnimationEnabled: Boolean? by prefs

    var inAppAnimationEnabled: Boolean
        get() = _inAppAnimationEnabled ?: true
        set(value) {
            _inAppAnimationEnabled = value
        }

    companion object {
        private const val VALUE_KEY = "ANIMATIONS_ENABLED"
    }
}
