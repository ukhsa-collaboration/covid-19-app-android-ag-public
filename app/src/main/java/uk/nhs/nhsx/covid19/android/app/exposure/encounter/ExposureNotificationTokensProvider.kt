package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

class ExposureNotificationTokensProvider @Inject constructor(
    private val exposureNotificationTokensStorage: ExposureNotificationTokensStorage,
    moshi: Moshi,
    private val clock: Clock
) {

    private val exposureNotificationTokensAdapter: JsonAdapter<List<TokenInfo>> =
        moshi.adapter(exposureNotificationTokenType)

    private val lock = Object()

    var tokens: List<TokenInfo>
        get() {
            return synchronized(lock) {
                exposureNotificationTokensStorage.value?.let {
                    runCatching {
                        exposureNotificationTokensAdapter.fromJson(it)
                    }
                        .getOrElse {
                            Timber.e(it)
                            listOf()
                        } // TODO add crash analytics and come up with a more sophisticated solution
                } ?: listOf()
            }
        }
        private set(tokens) {
            return synchronized(lock) {
                exposureNotificationTokensStorage.value =
                    exposureNotificationTokensAdapter.toJson(tokens)
            }
        }

    fun add(token: String) = synchronized(lock) {
        val updatedList = tokens.toMutableList().apply {
            add(TokenInfo(token, startedAt = Instant.now(clock).toEpochMilli()))
        }
        tokens = updatedList
    }

    fun remove(token: String) = synchronized(lock) {
        val updatedList = tokens.filter { it.token != token }
        tokens = updatedList
    }

    fun updateToPolling(token: String, exposureDate: Long) = synchronized(lock) {
        val updatedList = tokens.map {
            if (it.token == token) {
                it.copy(exposureDate = exposureDate)
            } else {
                it
            }
        }
        tokens = updatedList
    }

    companion object {
        val exposureNotificationTokenType: Type = Types.newParameterizedType(
            List::class.java,
            TokenInfo::class.java
        )
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

@JsonClass(generateAdapter = true)
data class TokenInfo(
    val token: String,
    val exposureDate: Long? = null,
    val startedAt: Long
)
