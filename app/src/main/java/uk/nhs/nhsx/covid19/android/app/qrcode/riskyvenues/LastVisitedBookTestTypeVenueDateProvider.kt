package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskyVenueConfigurationDurationDays
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import uk.nhs.nhsx.covid19.android.app.util.storage
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class LastVisitedBookTestTypeVenueDateProvider @Inject constructor(
    private val clock: Clock,
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    var lastVisitedVenue: LastVisitedBookTestTypeVenueDate? by storage(LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY)

    companion object {
        const val LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY = "LAST_VISITED_BOOK_TEST_TYPE_VENUE_DATE_KEY"
    }

    fun containsBookTestTypeVenueAtRisk(): Boolean {
        val now = LocalDate.now(clock)
        val lastVisitedVenue = lastVisitedVenue
        return lastVisitedVenue != null &&
            now.isEqualOrAfter(lastVisitedVenue.latestDate) &&
            now.isBefore(lastVisitedVenue.latestDate.plusDays(lastVisitedVenue.riskyVenueConfigurationDurationDays.optionToBookATest.toLong()))
    }
}

@JsonClass(generateAdapter = true)
data class LastVisitedBookTestTypeVenueDate(
    val latestDate: LocalDate,
    val riskyVenueConfigurationDurationDays: RiskyVenueConfigurationDurationDays
)
