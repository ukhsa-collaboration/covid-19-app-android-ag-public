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
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsSubmissionLogStorage @Inject constructor(
    private val analyticsSubmissionLogJsonStorage: AnalyticsSubmissionLogJsonStorage,
    moshi: Moshi
) {
    private val analyticsSubmissionLogSerializationAdapter: JsonAdapter<Set<LocalDate>> = moshi.adapter(setOfLocalDates)

    private var value: Set<LocalDate>?
        get() {
            return analyticsSubmissionLogJsonStorage.value?.let {
                runCatching {
                    analyticsSubmissionLogSerializationAdapter.fromJson(it)
                }.getOrElse {
                    Timber.e(it)
                    null
                } // TODO add crash analytics and come up with a more sophisticated solution
            }
        }
        set(value) {
            analyticsSubmissionLogJsonStorage.value = analyticsSubmissionLogSerializationAdapter.toJson(value)
        }

    fun getLogForAnalyticsWindow(analyticsWindowDate: LocalDate): Set<LocalDate> {
        // Analytics window is only needed for initialization
        return value ?: createAndStoreDefaultSet(analyticsWindowDate)
    }

    fun addDate(localDate: LocalDate) {
        value = value?.toMutableSet().apply { this?.add(localDate) }
    }

    fun removeBeforeOrEqual(date: LocalDate) {
        value = value?.filter { it.isEqualOrAfter(date) }?.toSet()
    }

    // Initially populate the set with dates for the previous days so that we start with missingPacketsLast7Days=0
    // and only start counting missed submissions after the app was installed and onboarded
    private fun createAndStoreDefaultSet(analyticsWindowDate: LocalDate): Set<LocalDate> {
        val defaultSet = mutableSetOf<LocalDate>().apply {
            for (i in 1..SUBMISSION_LOG_CHECK_RANGE_MAX) {
                add(analyticsWindowDate.minusDays(i.toLong()))
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
