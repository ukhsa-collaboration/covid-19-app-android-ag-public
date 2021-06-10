package uk.nhs.nhsx.covid19.android.app.status.localmessage

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class LocalMessagesProvider @Inject constructor(
    private val localMessagesStorage: LocalMessagesStorage,
    private val moshi: Moshi
) {
    private val lock = Object()

    var localMessages: LocalMessagesResponse?
        get() = synchronized(lock) {
            localMessagesStorage.value?.let {
                kotlin.runCatching {
                    moshi.adapter(LocalMessagesResponse::class.java).fromJson(it)
                }
                    .getOrElse {
                        Timber.e(it)
                        null
                    }
            }
        }
        set(value) = synchronized(lock) {
            localMessagesStorage.value = moshi.adapter(LocalMessagesResponse::class.java).toJson(value)
        }
}

class LocalMessagesStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "CONTENT_MODULE_KEY"
    }
}
