package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type
import javax.inject.Inject
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventPayload
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with

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
