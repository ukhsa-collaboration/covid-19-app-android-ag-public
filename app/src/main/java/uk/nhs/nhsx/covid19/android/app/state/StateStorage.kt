package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.StateJson.ContactCaseJson
import uk.nhs.nhsx.covid19.android.app.state.StateJson.DefaultJson
import uk.nhs.nhsx.covid19.android.app.state.StateJson.IndexCaseJson
import uk.nhs.nhsx.covid19.android.app.state.StateJson.IsolationJson
import uk.nhs.nhsx.covid19.android.app.state.StateJson.PreviousIsolationJson
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class StateStorage @Inject constructor(
    private val stateStringStorage: StateStringStorage,
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
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
                    fromJson?.lastOrNull()?.toState(isolationConfigurationProvider.durationDays)
                        ?: Default()
                }
                    .getOrElse {
                        Timber.e(it)
                        Default()
                    }
            } ?: Default()
        set(newState) {
            val updatedHistory = mutableListOf<State>().apply {
                add(newState)
            }.map { it.toStateJson() }
            stateStringStorage.prefsValue = stateSerializationAdapter.toJson(updatedHistory)
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
        override val version: Int = 2
    ) : StateJson()

    @JsonClass(generateAdapter = true)
    data class IsolationJson(
        val isolationStart: Instant,
        @Deprecated("Please store expiry date inside specific indexCase or contactCase")
        val expiryDate: LocalDate,
        val indexCase: IndexCaseJson?,
        val contactCase: ContactCaseJson?,
        val isolationConfiguration: DurationDays?,
        override val version: Int = 4
    ) : StateJson()

    @Json(name = "IndexCase")
    @JsonClass(generateAdapter = true)
    data class IndexCaseJson(
        val symptomsOnsetDate: LocalDate,
        val expiryDate: LocalDate?,
        val selfAssessment: Boolean?
    )

    @Json(name = "ContactCase")
    @JsonClass(generateAdapter = true)
    data class ContactCaseJson(
        val startDate: Instant,
        val notificationDate: Instant?,
        val expiryDate: LocalDate?
    )

    @JsonClass(generateAdapter = true)
    data class PreviousIsolationJson(
        val isolationStart: Instant,
        @Deprecated("Please store expiry date inside specific indexCase or contactCase")
        val expiryDate: LocalDate,
        val indexCase: IndexCase?,
        val contactCase: ContactCase?,
        val isolationConfiguration: DurationDays
    )
}

private fun State.toStateJson(): StateJson = when (this) {
    is Default -> {
        val isolationJson = if (previousIsolation != null) {
            PreviousIsolationJson(
                previousIsolation.isolationStart,
                previousIsolation.expiryDate,
                previousIsolation.indexCase,
                previousIsolation.contactCase,
                previousIsolation.isolationConfiguration
            )
        } else null
        DefaultJson(isolationJson)
    }
    is Isolation -> IsolationJson(
        isolationStart,
        expiryDate,
        indexCase?.let { IndexCaseJson(it.symptomsOnsetDate, it.expiryDate, it.selfAssessment) },
        contactCase?.let { ContactCaseJson(it.startDate, it.notificationDate, it.expiryDate) },
        isolationConfiguration
    )
}

private fun StateJson.toState(latestIsolationConfiguration: DurationDays): State =
    when (this) {
        is DefaultJson -> when (this.version) {
            1 -> {
                val previousIsolation = if (previousIsolation != null) {
                    val updatedIndexCase =
                        previousIsolation.indexCase?.copy(expiryDate = previousIsolation.expiryDate)
                    val updatedContactCase =
                        previousIsolation.contactCase?.copy(expiryDate = previousIsolation.expiryDate)
                    Isolation(
                        previousIsolation.isolationStart,
                        latestIsolationConfiguration,
                        updatedIndexCase,
                        updatedContactCase
                    )
                } else null
                Default(previousIsolation = previousIsolation)
            }
            2 -> {
                val previousIsolation = if (previousIsolation != null) {
                    Isolation(
                        previousIsolation.isolationStart,
                        previousIsolation.isolationConfiguration,
                        previousIsolation.indexCase,
                        previousIsolation.contactCase
                    )
                } else null
                Default(previousIsolation = previousIsolation)
            }
            else -> {
                Default(previousIsolation = null)
            }
        }

        is IsolationJson -> when (this.version) {
            1 -> {
                Isolation(
                    isolationStart,
                    latestIsolationConfiguration,
                    indexCase?.let {
                        IndexCase(
                            it.symptomsOnsetDate,
                            expiryDate,
                            true
                        )
                    },
                    contactCase?.let {
                        ContactCase(
                            it.startDate,
                            it.notificationDate,
                            expiryDate
                        )
                    }
                )
            }
            2 -> {
                Isolation(
                    isolationStart,
                    isolationConfiguration ?: latestIsolationConfiguration,
                    indexCase?.let {
                        IndexCase(
                            it.symptomsOnsetDate,
                            it.expiryDate ?: expiryDate,
                            true
                        )
                    },
                    contactCase?.let {
                        ContactCase(
                            it.startDate,
                            it.notificationDate,
                            it.expiryDate ?: expiryDate
                        )
                    }
                )
            }
            3 -> {
                Isolation(
                    isolationStart,
                    isolationConfiguration ?: latestIsolationConfiguration,
                    indexCase?.let {
                        IndexCase(
                            it.symptomsOnsetDate,
                            it.expiryDate ?: expiryDate,
                            it.selfAssessment ?: true
                        )
                    },
                    contactCase?.let {
                        ContactCase(
                            it.startDate,
                            it.notificationDate,
                            it.expiryDate ?: expiryDate
                        )
                    }
                )
            }
            4 -> {
                Isolation(
                    isolationStart,
                    isolationConfiguration ?: latestIsolationConfiguration,
                    indexCase?.let {
                        IndexCase(
                            it.symptomsOnsetDate,
                            it.expiryDate ?: expiryDate,
                            it.selfAssessment ?: true
                        )
                    },
                    contactCase?.let {
                        ContactCase(
                            it.startDate,
                            it.notificationDate,
                            it.expiryDate ?: expiryDate
                        )
                    }
                )
            }
            else -> {
                Default(previousIsolation = null)
            }
        }
    }
