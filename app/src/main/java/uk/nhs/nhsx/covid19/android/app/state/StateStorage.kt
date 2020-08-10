package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.StateJson.DefaultJson
import uk.nhs.nhsx.covid19.android.app.state.StateJson.IsolationJson
import uk.nhs.nhsx.covid19.android.app.state.StateJson.PreviousIsolationJson
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class StateStorage @Inject constructor(
    private val stateStringStorage: StateStringStorage,
    moshi: Moshi
) {

    private val type = Types.newParameterizedType(
        List::class.java,
        StateJson::class.java
    )
    private val stateSerializationAdapter: JsonAdapter<List<StateJson>> = moshi.adapter(type)

    var state: State
        get() =
            stateStringStorage.prefsValue?.let {
                runCatching {
                    val fromJson = stateSerializationAdapter.fromJson(it)
                    fromJson?.lastOrNull()?.toState() ?: Default()
                }
                    .getOrElse {
                        Timber.e(it)
                        Default()
                    } // TODO add crash analytics and come up with a more sophisticated solution
            } ?: Default()
        set(newState) {
            val updatedHistory = mutableListOf<State>().apply {
                addAll(getHistory())
                add(newState)
            }.map { it.toStateJson() }
            stateStringStorage.prefsValue = stateSerializationAdapter.toJson(updatedHistory)
        }

    fun getHistory(): List<State> {
        return stateStringStorage.prefsValue?.let {
            runCatching {
                val fromJson = stateSerializationAdapter.fromJson(it)
                fromJson?.map { it.toState() }
            }
                .getOrElse {
                    Timber.e(it)
                    listOf(Default())
                }
        } ?: listOf(Default())
    }

    fun updateHistory(updatedHistory: List<State>) {
        updatedHistory.map { it.toStateJson() }
            .apply {
                stateStringStorage.prefsValue = stateSerializationAdapter.toJson(this)
            }
    }
}

class StateStringStorage @Inject constructor(sharedPreferences: SharedPreferences) {

    companion object {
        private const val VALUE_KEY = "STATE_KEY"
    }

    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var prefsValue: String? by prefs
}

sealed class StateJson {
    companion object {
        val stateMoshiAdapter: PolymorphicJsonAdapterFactory<StateJson> =
            PolymorphicJsonAdapterFactory.of(StateJson::class.java, "type")
                .withSubtype(DefaultJson::class.java, "Default")
                .withSubtype(IsolationJson::class.java, "Isolation")
    }

    @Json(name = "version")
    abstract val version: Int

    @JsonClass(generateAdapter = true)
    data class DefaultJson(
        val previousIsolation: PreviousIsolationJson?,
        override val version: Int = 1
    ) : StateJson()

    @JsonClass(generateAdapter = true)
    data class IsolationJson(
        val isolationStart: Instant,
        val expiryDate: LocalDate,
        val indexCase: IndexCase?,
        val contactCase: ContactCase?,
        override val version: Int = 1
    ) : StateJson()

    @JsonClass(generateAdapter = true)
    data class PreviousIsolationJson(
        val isolationStart: Instant,
        val expiryDate: LocalDate,
        val indexCase: IndexCase?,
        val contactCase: ContactCase?
    )
}

private fun State.toStateJson(): StateJson = when (this) {
    is Default -> {
        val isolationJson = if (previousIsolation != null) {
            PreviousIsolationJson(
                previousIsolation.isolationStart,
                previousIsolation.expiryDate,
                previousIsolation.indexCase,
                previousIsolation.contactCase
            )
        } else null
        DefaultJson(isolationJson)
    }
    is Isolation -> IsolationJson(
        isolationStart,
        expiryDate,
        indexCase,
        contactCase
    )
}

private fun StateJson.toState(): State =
    when (this) {
        is DefaultJson -> {
            val previousIsolation = if (previousIsolation != null) {
                Isolation(
                    previousIsolation.isolationStart,
                    previousIsolation.expiryDate,
                    previousIsolation.indexCase,
                    previousIsolation.contactCase
                )
            } else null
            Default(previousIsolation = previousIsolation)
        }
        is IsolationJson -> Isolation(
            isolationStart,
            expiryDate,
            indexCase,
            contactCase
        )
    }
