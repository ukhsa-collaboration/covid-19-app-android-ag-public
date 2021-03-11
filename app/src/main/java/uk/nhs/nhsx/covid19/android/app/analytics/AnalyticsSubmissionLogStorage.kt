package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.analytics.CalculateMissingSubmissionDays.Companion.SUBMISSION_LOG_CHECK_RANGE_MAX
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.lang.reflect.Type
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsSubmissionLogStorage @Inject constructor(
    private val analyticsSubmissionLogJsonStorage: AnalyticsSubmissionLogJsonStorage,
    private val clock: Clock,
    moshi: Moshi
) {
    private val analyticsSubmissionLogSerializationAdapter: JsonAdapter<Set<LocalDate>> = moshi.adapter(setOfLocalDates)

    var value: Set<LocalDate>
        get() {
            return analyticsSubmissionLogJsonStorage.value?.let {
                runCatching {
                    analyticsSubmissionLogSerializationAdapter.fromJson(it)
                }.getOrElse {
                    Timber.e(it)
                    createAndStoreDefaultSet()
                } // TODO add crash analytics and come up with a more sophisticated solution
            } ?: createAndStoreDefaultSet()
        }
        private set(value) {
            analyticsSubmissionLogJsonStorage.value = analyticsSubmissionLogSerializationAdapter.toJson(value)
        }

    fun add(localDate: LocalDate) {
        value = value.toMutableSet().apply { add(localDate) }
    }

    fun removeBeforeOrEqual(date: LocalDate) {
        value = value.filter { it.isEqualOrAfter(date) }.toSet()
    }

    // Initially populate the set with dates for the previous 8 days so that we start with missingPacketsLast7Days=0
    // and only start counting missed submissions after the app was installed
    private fun createAndStoreDefaultSet(): Set<LocalDate> {
        val defaultSet = mutableSetOf<LocalDate>().apply {
            for (i in 1..SUBMISSION_LOG_CHECK_RANGE_MAX + 1) {
                // LocalDate.now() uses UTC time from provided clock no matter what timezone the clock is associated with
                val nowInUtc = LocalDate.now(clock)
                add(nowInUtc.minusDays(i.toLong()))
            }
        }
        value = defaultSet
        return defaultSet
    }

    companion object {
        val setOfLocalDates: Type = Types.newParameterizedType(
            Set::class.java,
            LocalDate::class.java
        )
    }
}

class AnalyticsSubmissionLogJsonStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value: String? by prefs

    companion object {
        const val VALUE_KEY = "ANALYTICS_SUBMISSION_LOG"
    }
}
