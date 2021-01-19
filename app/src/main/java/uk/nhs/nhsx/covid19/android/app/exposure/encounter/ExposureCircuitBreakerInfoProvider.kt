package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import javax.inject.Inject

class ExposureCircuitBreakerInfoProvider @Inject constructor(
    private val exposureCircuitBreakerInfoStorage: ExposureCircuitBreakerInfoStorage,
    moshi: Moshi
) {

    private val exposureCircuitBreakerInfoAdapter: JsonAdapter<List<ExposureCircuitBreakerInfo>> =
        moshi.adapter(exposureCircuitBreakerInfoType)

    private val lock = Object()

    var info: List<ExposureCircuitBreakerInfo>
        get() {
            return synchronized(lock) {
                exposureCircuitBreakerInfoStorage.value?.let {
                    runCatching {
                        exposureCircuitBreakerInfoAdapter.fromJson(it)
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
                exposureCircuitBreakerInfoStorage.value = exposureCircuitBreakerInfoAdapter.toJson(tokens)
            }
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
        val exposureCircuitBreakerInfoType: Type = Types.newParameterizedType(
            List::class.java,
            ExposureCircuitBreakerInfo::class.java
        )
    }
}

class ExposureCircuitBreakerInfoStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "EXPOSURE_CIRCUIT_BREAKER_INFO_KEY"
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
