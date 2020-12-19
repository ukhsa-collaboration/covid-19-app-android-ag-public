package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData.ExposureWindowWithRisk
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayloadScanInstance
import uk.nhs.nhsx.covid19.android.app.remote.data.Infectiousness
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.lang.reflect.Type
import java.time.Instant
import javax.inject.Inject

class EpidemiologyEventProvider @Inject constructor(
    private val epidemiologyEventStorage: EpidemiologyEventStorage,
    moshi: Moshi
) {
    private val epidemiologyEventsAdapter: JsonAdapter<List<EpidemiologyEvent>> =
        moshi.adapter(epidemiologyEventType)

    private val lock = Object()

    var epidemiologyEvents: List<EpidemiologyEvent>
        get() {
            return synchronized(lock) {
                epidemiologyEventStorage.value?.let {
                    runCatching {
                        epidemiologyEventsAdapter.fromJson(it)
                    }
                        .getOrElse {
                            Timber.e(it)
                            listOf()
                        } // TODO add crash analytics and come up with a more sophisticated solution
                } ?: listOf()
            }
        }
        private set(epidemiologyEvents) {
            return synchronized(lock) {
                epidemiologyEventStorage.value =
                    epidemiologyEventsAdapter.toJson(epidemiologyEvents)
            }
        }

    fun add(events: List<EpidemiologyEvent>) = synchronized(lock) {
        val updatedList = epidemiologyEvents.toMutableList().apply {
            addAll(events)
        }
        epidemiologyEvents = updatedList
    }

    fun clear() = synchronized(lock) {
        epidemiologyEventStorage.value = null
    }

    val epidemiologyEventCount: Int
        get() = epidemiologyEvents.size

    companion object {
        val epidemiologyEventType: Type = Types.newParameterizedType(
            List::class.java,
            EpidemiologyEvent::class.java
        )
    }
}

class EpidemiologyEventStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        private const val VALUE_KEY =
            "EPIDEMIOLOGY_EVENT"
    }
}

@JsonClass(generateAdapter = true)
data class EpidemiologyEvent(
    val version: Int,
    val payload: EpidemiologyEventPayload
)

fun ExposureWindowWithRisk.toEpidemiologyEvent(): EpidemiologyEvent {
    return EpidemiologyEvent(
        version = 1,
        payload = EpidemiologyEventPayload(
            date = Instant.ofEpochMilli(this.dayRisk.startOfDayMillis),
            infectiousness = Infectiousness.fromInt(this.exposureWindow.infectiousness),
            scanInstances = this.exposureWindow.scanInstances.map { scanInstance ->
                EpidemiologyEventPayloadScanInstance(
                    minimumAttenuation = scanInstance.minAttenuationDb,
                    secondsSinceLastScan = scanInstance.secondsSinceLastScan,
                    typicalAttenuation = scanInstance.typicalAttenuationDb
                )
            },
            riskScore = this.dayRisk.calculatedRisk,
            riskCalculationVersion = this.dayRisk.riskCalculationVersion
        )
    )
}
