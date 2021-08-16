package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.analytics.CalculateMissingSubmissionDays.Companion.SUBMISSION_LOG_CHECK_RANGE_MAX
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import uk.nhs.nhsx.covid19.android.app.util.setStorage
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsSubmissionLogStorage @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private var value: Set<LocalDate>? by setStorage(ANALYTICS_SUBMISSION_LOG)

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
        const val ANALYTICS_SUBMISSION_LOG = "ANALYTICS_SUBMISSION_LOG"
    }
}
