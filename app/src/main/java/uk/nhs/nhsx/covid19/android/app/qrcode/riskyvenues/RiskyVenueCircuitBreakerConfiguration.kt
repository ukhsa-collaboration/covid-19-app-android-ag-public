package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueMessageType.INFORM
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.listStorage
import java.time.Instant
import javax.inject.Inject

class RiskyVenueCircuitBreakerConfigurationProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private val lock = Object()

    var configs: List<RiskyVenueCircuitBreakerConfiguration> by listStorage(RISKY_VENUE_POLLING_CONFIGURATION_JSON_KEY, default = emptyList())

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
        const val RISKY_VENUE_POLLING_CONFIGURATION_JSON_KEY = "RISKY_VENUE_POLLING_CONFIGURATION_JSON_KEY"
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
