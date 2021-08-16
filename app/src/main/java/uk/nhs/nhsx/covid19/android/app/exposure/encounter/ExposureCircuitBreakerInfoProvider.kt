package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.listStorage
import javax.inject.Inject

class ExposureCircuitBreakerInfoProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private val lock = Object()

    private var storedInfo: List<ExposureCircuitBreakerInfo> by listStorage(EXPOSURE_CIRCUIT_BREAKER_INFO_KEY, default = emptyList())

    var info: List<ExposureCircuitBreakerInfo>
        get() = storedInfo
        private set(tokens) {
            storedInfo = tokens
        }

    fun add(exposureCircuitBreakerInfo: ExposureCircuitBreakerInfo) =
        synchronized(lock) {
            val updatedList = info.toMutableList().apply {
                add(exposureCircuitBreakerInfo)
            }
            info = updatedList
        }

    fun remove(exposureCircuitBreakerInfo: ExposureCircuitBreakerInfo) =
        synchronized(lock) {
            val updatedList = info.filterNot { it == exposureCircuitBreakerInfo }
            info = updatedList
        }

    fun setApprovalToken(exposureCircuitBreakerInfo: ExposureCircuitBreakerInfo, approvalToken: String) =
        synchronized(lock) {
            val updatedList = info.map {
                if (it == exposureCircuitBreakerInfo) it.copy(approvalToken = approvalToken) else it
            }
            info = updatedList
        }

    companion object {
        const val EXPOSURE_CIRCUIT_BREAKER_INFO_KEY = "EXPOSURE_CIRCUIT_BREAKER_INFO_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class ExposureCircuitBreakerInfo(
    val maximumRiskScore: Double,
    val startOfDayMillis: Long,
    val matchedKeyCount: Int,
    val riskCalculationVersion: Int,
    val exposureNotificationDate: Long,
    val approvalToken: String? = null
)
