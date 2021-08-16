@file:Suppress("DEPRECATION", "ClassName")

package uk.nhs.nhsx.covid19.android.app.state

import android.content.SharedPreferences
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Default4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9.ContactCase4_9
import uk.nhs.nhsx.covid19.android.app.state.State4_9.Isolation4_9.IndexCase4_9
import uk.nhs.nhsx.covid19.android.app.state.StateJson4_9.Companion.stateMoshiAdapter4_9
import uk.nhs.nhsx.covid19.android.app.state.StateJson4_9.DefaultJson4_9
import uk.nhs.nhsx.covid19.android.app.state.StateJson4_9.IsolationJson4_9
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.listStorage
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@Deprecated("Not used anymore since 4.10. Use StateStorage instead.")
class StateStorage4_9 @Inject constructor(
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    _moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {
    override val moshi: Moshi = _moshi
        .newBuilder()
        .add(stateMoshiAdapter4_9)
        .build()

    private var storedStates: List<StateJson4_9>? by listStorage(STATE_KEY)
    val state: State4_9?
        get() = storedStates?.lastOrNull()?.toState(isolationConfigurationProvider.durationDays)

    fun clear() {
        storedStates = null
    }

    companion object {
        const val assumedDaysFromOnsetToSelfAssessment4_9: Long = 2
        const val STATE_KEY = "STATE_KEY"
    }
}

@Deprecated("Not used anymore since 4.10")
sealed class StateJson4_9 {
    companion object {
        val stateMoshiAdapter4_9: PolymorphicJsonAdapterFactory<StateJson4_9> =
            PolymorphicJsonAdapterFactory.of(StateJson4_9::class.java, "type")
                .withSubtype(DefaultJson4_9::class.java, "Default")
                .withSubtype(IsolationJson4_9::class.java, "Isolation")
    }

    @Json(name = "version")
    abstract val version: Int

    @Deprecated("Not used anymore since 4.10")
    @JsonClass(generateAdapter = true)
    data class DefaultJson4_9(
        val previousIsolation: PreviousIsolationJson4_9?,
        override val version: Int = 2
    ) : StateJson4_9()

    @Deprecated("Not used anymore since 4.10")
    @JsonClass(generateAdapter = true)
    data class IsolationJson4_9(
        val isolationStart: Instant,
        @Deprecated("Please store expiry date inside specific indexCase or contactCase")
        val expiryDate: LocalDate,
        val indexCase: IndexCaseJson4_9?,
        val contactCase: ContactCaseJson4_9?,
        val isolationConfiguration: DurationDays?,
        override val version: Int = 4
    ) : StateJson4_9()

    @Deprecated("Not used anymore since 4.10")
    @Json(name = "IndexCase")
    @JsonClass(generateAdapter = true)
    data class IndexCaseJson4_9(
        val symptomsOnsetDate: LocalDate,
        val expiryDate: LocalDate?,
        val selfAssessment: Boolean?
    )

    @Deprecated("Not used anymore since 4.10")
    @Json(name = "ContactCase")
    @JsonClass(generateAdapter = true)
    data class ContactCaseJson4_9(
        val startDate: Instant,
        val notificationDate: Instant?,
        val expiryDate: LocalDate?,
        val dailyContactTestingOptInDate: LocalDate? = null,
    )

    @Deprecated("Not used anymore since 4.10")
    @JsonClass(generateAdapter = true)
    data class PreviousIsolationJson4_9(
        val isolationStart: Instant,
        @Deprecated("Please store expiry date inside specific indexCase or contactCase")
        val expiryDate: LocalDate,
        val indexCase: IndexCase4_9?,
        val contactCase: ContactCase4_9?,
        val isolationConfiguration: DurationDays
    )
}

private fun StateJson4_9.toState(latestIsolationConfiguration: DurationDays): State4_9 =
    when (this) {
        is DefaultJson4_9 -> when (this.version) {
            1 -> {
                val previousIsolation = if (previousIsolation != null) {
                    val updatedIndexCase =
                        previousIsolation.indexCase?.copy(expiryDate = previousIsolation.expiryDate)
                    val updatedContactCase =
                        previousIsolation.contactCase?.copy(expiryDate = previousIsolation.expiryDate)
                    Isolation4_9(
                        previousIsolation.isolationStart,
                        latestIsolationConfiguration,
                        updatedIndexCase,
                        updatedContactCase
                    )
                } else null
                Default4_9(previousIsolation = previousIsolation)
            }
            2 -> {
                val previousIsolation = if (previousIsolation != null) {
                    Isolation4_9(
                        previousIsolation.isolationStart,
                        previousIsolation.isolationConfiguration,
                        previousIsolation.indexCase,
                        previousIsolation.contactCase
                    )
                } else null
                Default4_9(previousIsolation = previousIsolation)
            }
            else -> {
                Default4_9(previousIsolation = null)
            }
        }

        is IsolationJson4_9 -> when (this.version) {
            1 -> {
                Isolation4_9(
                    isolationStart,
                    latestIsolationConfiguration,
                    indexCase?.let {
                        IndexCase4_9(
                            it.symptomsOnsetDate,
                            expiryDate,
                            true
                        )
                    },
                    contactCase?.let {
                        ContactCase4_9(
                            it.startDate,
                            it.notificationDate,
                            expiryDate,
                            it.dailyContactTestingOptInDate
                        )
                    }
                )
            }
            2 -> {
                Isolation4_9(
                    isolationStart,
                    isolationConfiguration ?: latestIsolationConfiguration,
                    indexCase?.let {
                        IndexCase4_9(
                            it.symptomsOnsetDate,
                            it.expiryDate ?: expiryDate,
                            true
                        )
                    },
                    contactCase?.let {
                        ContactCase4_9(
                            it.startDate,
                            it.notificationDate,
                            it.expiryDate ?: expiryDate,
                            it.dailyContactTestingOptInDate
                        )
                    }
                )
            }
            3 -> {
                Isolation4_9(
                    isolationStart,
                    isolationConfiguration ?: latestIsolationConfiguration,
                    indexCase?.let {
                        IndexCase4_9(
                            it.symptomsOnsetDate,
                            it.expiryDate ?: expiryDate,
                            it.selfAssessment ?: true
                        )
                    },
                    contactCase?.let {
                        ContactCase4_9(
                            it.startDate,
                            it.notificationDate,
                            it.expiryDate ?: expiryDate,
                            it.dailyContactTestingOptInDate
                        )
                    }
                )
            }
            4 -> {
                Isolation4_9(
                    isolationStart,
                    isolationConfiguration ?: latestIsolationConfiguration,
                    indexCase?.let {
                        IndexCase4_9(
                            it.symptomsOnsetDate,
                            it.expiryDate ?: expiryDate,
                            it.selfAssessment ?: true
                        )
                    },
                    contactCase?.let {
                        ContactCase4_9(
                            it.startDate,
                            it.notificationDate,
                            it.expiryDate ?: expiryDate,
                            it.dailyContactTestingOptInDate
                        )
                    }
                )
            }
            else -> {
                Default4_9(previousIsolation = null)
            }
        }
    }

@Deprecated("Not used anymore since 4.10. Use IsolationState instead.")
sealed class State4_9 {
    @Deprecated("Not used anymore since 4.10. Use IsolationState instead.")
    data class Default4_9(val previousIsolation: Isolation4_9? = null) : State4_9()

    @Deprecated("Not used anymore since 4.10. Use IsolationState instead.")
    data class Isolation4_9(
        val isolationStart: Instant,
        val isolationConfiguration: DurationDays,
        val indexCase: IndexCase4_9? = null,
        val contactCase: ContactCase4_9? = null
    ) : State4_9() {

        @Deprecated("Not used anymore since 4.10. Use IndexCase instead.")
        @JsonClass(generateAdapter = true)
        data class IndexCase4_9(
            val symptomsOnsetDate: LocalDate,
            val expiryDate: LocalDate,
            val selfAssessment: Boolean
        )

        @Deprecated("Not used anymore since 4.10. Use ContactCase instead.")
        @JsonClass(generateAdapter = true)
        data class ContactCase4_9(
            val startDate: Instant,
            val notificationDate: Instant?,
            val expiryDate: LocalDate,
            val dailyContactTestingOptInDate: LocalDate? = null,
        )

        private fun isContactCaseOnly(): Boolean =
            contactCase != null && indexCase == null

        private fun isIndexCaseOnly(): Boolean =
            indexCase != null && contactCase == null

        private fun isIndexCase(): Boolean =
            indexCase != null

        private fun isContactCase(): Boolean =
            contactCase != null

        private fun isBothCases(): Boolean =
            isIndexCase() && isContactCase()

        private fun capExpiryDate(potentialExpiryDate: LocalDate): LocalDate {
            val latestPossibleExpiryDate = isolationStart.plus(
                isolationConfiguration.maxIsolation.toLong(),
                ChronoUnit.DAYS
            ).toLocalDate(ZoneOffset.UTC)
            return selectEarliest(latestPossibleExpiryDate, potentialExpiryDate)
        }

        private fun latestExpiryDate(
            indexCase: IndexCase4_9,
            contactCase: ContactCase4_9
        ): LocalDate {
            return if (indexCase.expiryDate.isAfter(contactCase.expiryDate)) {
                indexCase.expiryDate
            } else {
                contactCase.expiryDate
            }
        }

        val expiryDate: LocalDate
            get() {
                val potentialExpiryDate = when {
                    isBothCases() -> {
                        latestExpiryDate(indexCase!!, contactCase!!)
                    }
                    isIndexCaseOnly() -> {
                        indexCase!!.expiryDate
                    }
                    isContactCaseOnly() -> {
                        contactCase!!.expiryDate
                    }
                    else -> {
                        Timber.e("Unknown expiryDate")
                        LocalDate.now().plusDays(1)
                    }
                }
                return capExpiryDate(potentialExpiryDate)
            }
    }
}
