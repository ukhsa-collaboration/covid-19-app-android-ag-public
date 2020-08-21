package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import java.time.Instant
import javax.inject.Inject

class RiskyVenuePollingConfigurationProvider @Inject constructor(
    private val riskyVenuePollingConfigurationJsonStorage: RiskyVenuePollingConfigurationJsonStorage,
    moshi: Moshi
) {
    private val pollingConfigSerializationAdapter: JsonAdapter<List<RiskyVenuePollingConfiguration>> =
        moshi.adapter(riskyVenuePollingConfigurationType)

    private val lock = Object()

    var configs: List<RiskyVenuePollingConfiguration>
        get() {
            return synchronized(lock) {
                riskyVenuePollingConfigurationJsonStorage.value?.let {
                    runCatching {
                        pollingConfigSerializationAdapter.fromJson(it)
                    }
                        .getOrElse {
                            Timber.e(it)
                            listOf()
                        } // TODO add crash analytics and come up with a more sophisticated solution
                } ?: listOf()
            }
        }
        set(listOfTokenPairs) {
            return synchronized(lock) {
                riskyVenuePollingConfigurationJsonStorage.value =
                    pollingConfigSerializationAdapter.toJson(listOfTokenPairs)
            }
        }

    fun add(pollingConfig: RiskyVenuePollingConfiguration) = synchronized(lock) {
        val updatedList = configs.toMutableList().apply {
            add(pollingConfig)
        }
        configs = updatedList
    }

    fun remove(pollingConfig: RiskyVenuePollingConfiguration) = synchronized(lock) {
        val updatedList = configs.toMutableList().apply {
            remove(pollingConfig)
        }
        configs = updatedList
    }

    companion object {
        val riskyVenuePollingConfigurationType: Type = Types.newParameterizedType(
            List::class.java,
            RiskyVenuePollingConfiguration::class.java
        )
    }
}

class RiskyVenuePollingConfigurationJsonStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "RISKY_VENUE_POLLING_CONFIGURATION_JSON_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class RiskyVenuePollingConfiguration(
    val startedAt: Instant,
    val venueId: VenueId,
    val approvalToken: String
)
