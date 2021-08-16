package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.listStorage
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class EpidemiologyEventProvider @Inject constructor(
    private val clock: Clock,
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private val lock = Object()

    private var storedEpidemiologyEvents: List<EpidemiologyEvent> by listStorage(EPIDEMIOLOGY_EVENT, default = emptyList())

    var epidemiologyEvents: List<EpidemiologyEvent>
        get() = storedEpidemiologyEvents
        private set(epidemiologyEvents) {
            storedEpidemiologyEvents = epidemiologyEvents
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
        const val EPIDEMIOLOGY_EVENT = "EPIDEMIOLOGY_EVENT"
    }
}

@JsonClass(generateAdapter = true)
data class EpidemiologyEvent(
    val payload: EpidemiologyEventPayload
    // removed val version: Int
)
