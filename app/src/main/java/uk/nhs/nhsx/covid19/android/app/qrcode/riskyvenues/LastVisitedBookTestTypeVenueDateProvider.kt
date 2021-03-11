package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class LastVisitedBookTestTypeVenueDateProvider @Inject constructor(
    private val lastVisitedBookTestTypeVenueDateStorage: LastVisitedBookTestTypeVenueDateStorage,
    private val clock: Clock,
    moshi: Moshi
) {

    private val lastVisitedBookTestTypeVenueDateAdapter =
        moshi.adapter(LastVisitedBookTestTypeVenueDate::class.java)

    var lastVisitedVenue: LastVisitedBookTestTypeVenueDate?
        get() =
            lastVisitedBookTestTypeVenueDateStorage.value?.let {
                runCatching {
                    lastVisitedBookTestTypeVenueDateAdapter.fromJson(it)
                }
                    .getOrElse {
                        Timber.e(it)
                        null
                    } // TODO add crash analytics and come up with a more sophisticated solution
            }
        set(value) {
            if (value == null) {
                lastVisitedBookTestTypeVenueDateStorage.value = null
            } else {
                lastVisitedBookTestTypeVenueDateStorage.value =
                    lastVisitedBookTestTypeVenueDateAdapter.toJson(value)
            }
        }

    fun containsBookTestTypeVenueAtRisk(): Boolean {
        val now = LocalDate.now(clock)
        val lastVisitedVenue = lastVisitedVenue
        return lastVisitedVenue != null &&
            now.isEqualOrAfter(lastVisitedVenue.latestDate) &&
            now.isBefore(lastVisitedVenue.latestDate.plusDays(lastVisitedVenue.riskyVenueConfigurationDurationDays.optionToBookATest.toLong()))
    }
}

class LastVisitedBookTestTypeVenueDateStorage @Inject constructor(
    sharedPreferences: SharedPreferences
) {

    private val prefs = sharedPreferences.with<String>(LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY)

    var value: String? by prefs

    companion object {
        const val LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY = "LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY"
    }
}

@JsonClass(generateAdapter = true)
data class LastVisitedBookTestTypeVenueDate(
    val latestDate: LocalDate,
    val riskyVenueConfigurationDurationDays: RiskyVenueConfigurationDurationDays
)
