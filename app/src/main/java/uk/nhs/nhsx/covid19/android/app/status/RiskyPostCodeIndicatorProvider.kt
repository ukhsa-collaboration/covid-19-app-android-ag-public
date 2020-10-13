package uk.nhs.nhsx.covid19.android.app.status

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicatorWrapper
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class RiskyPostCodeIndicatorProvider @Inject constructor(
    private val riskyPostCodeIndicatorStorage: RiskyPostCodeIndicatorStorage,
    private val moshi: Moshi
) {
    private val lock = Object()

    var riskyPostCodeIndicator: RiskIndicatorWrapper?
        get() = synchronized(lock) {
            riskyPostCodeIndicatorStorage.value?.let {
                runCatching {
                    moshi.adapter(RiskIndicatorWrapper::class.java).fromJson(it)
                }
                    .getOrElse {
                        Timber.e(it)
                        null
                    } // TODO add crash analytics and come up with a more sophisticated solution
            }
        }
        set(value) = synchronized(lock) {
            riskyPostCodeIndicatorStorage.value =
                moshi.adapter(RiskIndicatorWrapper::class.java).toJson(value)
        }

    fun clear() = synchronized(lock) {
        riskyPostCodeIndicatorStorage.value = null
    }
}

class RiskyPostCodeIndicatorStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "RISKY_POST_CODE_INDICATOR_KEY"
    }
}
