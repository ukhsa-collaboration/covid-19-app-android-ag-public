package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import java.time.Instant
import javax.inject.Inject

class RiskyVenueCircuitBreakerConfigurationProvider @Inject constructor(
    private val riskyVenuePollingConfigurationJsonStorage: RiskyVenuePollingConfigurationJsonStorage,
    moshi: Moshi
) {
    private val circuitBreakerConfigSerializationAdapter: JsonAdapter<List<RiskyVenueCircuitBreakerConfiguration>> =
        moshi.adapter(riskyVenuePollingConfigurationType)

    private val lock = Object()

    var configs: List<RiskyVenueCircuitBreakerConfiguration>
        get() {
            return synchronized(lock) {
                riskyVenuePollingConfigurationJsonStorage.value?.let {
                    runCatching {
                        circuitBreakerConfigSerializationAdapter.fromJson(it)
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
                    circuitBreakerConfigSerializationAdapter.toJson(listOfTokenPairs)
            }
        }

    fun add(circuitBreakerConfig: RiskyVenueCircuitBreakerConfiguration) = synchronized(lock) {
        val updatedList = configs.toMutableList().apply {
            add(circuitBreakerConfig)
        }
        configs = updatedList
    }

    fun addAll(circuitBreakerConfigs: List<RiskyVenueCircuitBreakerConfiguration>) = synchronized(lock) {
        val updatedList = configs.toMutableList().apply {
            addAll(circuitBreakerConfigs)
        }
        configs = updatedList
    }

    fun remove(circuitBreakerConfig: RiskyVenueCircuitBreakerConfiguration) = synchronized(lock) {
        val updatedList = configs.toMutableList().apply {
            remove(circuitBreakerConfig)
        }
        configs = updatedList
    }

    companion object {
        val riskyVenuePollingConfigurationType: Type = Types.newParameterizedType(
            List::class.java,
            RiskyVenueCircuitBreakerConfiguration::class.java
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
data class RiskyVenueCircuitBreakerConfiguration(
    val startedAt: Instant,
    val venueId: VenueId,
    val approvalToken: String?,
    val isPolling: Boolean = true,
    val messageType: RiskyVenueMessageType = INFORM
)
