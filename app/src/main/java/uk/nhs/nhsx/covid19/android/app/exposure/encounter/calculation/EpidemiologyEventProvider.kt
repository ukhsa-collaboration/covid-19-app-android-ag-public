package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.lang.reflect.Type
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class EpidemiologyEventProvider @Inject constructor(
    private val epidemiologyEventStorage: EpidemiologyEventStorage,
    private val clock: Clock,
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

    fun addRiskyEpidemiologyEvents(events: List<EpidemiologyEvent>) = synchronized(lock) {
        val updatedList = epidemiologyEvents.toMutableList().apply {
            addAll(events)
        }
        epidemiologyEvents = updatedList
    }

    fun addNonRiskyEpidemiologyEvents(events: List<EpidemiologyEvent>, storageLimit: Int) = synchronized(lock) {
        val updatedList = epidemiologyEvents
            .partition { it.payload.isConsideredRisky }
            .let { (riskyEpidemiologyEvents, nonRiskyEpidemiologyEvents) ->
                riskyEpidemiologyEvents + (nonRiskyEpidemiologyEvents + events).takeLast(storageLimit)
            }

        epidemiologyEvents = updatedList
    }

    fun clearOnAndBefore(date: LocalDate) = synchronized(lock) {
        val updatedList = epidemiologyEvents.filter {
            it.payload.date.toLocalDate(clock.zone).isAfter(date)
        }
        epidemiologyEvents = updatedList
    }

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
    val payload: EpidemiologyEventPayload
    // removed val version: Int
)
