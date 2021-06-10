package uk.nhs.nhsx.covid19.android.app.about

import uk.nhs.nhsx.covid19.android.app.about.VenueVisitListItem.ContentItem
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitListItem.HeaderItem
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

class ClusterVenueVisits @Inject constructor(
    private val clock: Clock
) {

    operator fun invoke(venueVisitListItems: List<VenueVisitItem>): List<VenueVisitListItem> =
        venueVisitListItems.sortedWith(
            compareByDescending<VenueVisitItem> { it.venueVisit.from }
                .thenBy { it.venueVisit.venue.organizationPartName }
        )
            .groupBy { item -> item.venueVisit.from.toLocalDate(clock.zone) }
            .flatMap { (key, values) ->
                listOf(HeaderItem(key)).plus(
                    values.map {
                        ContentItem(it)
                    }
                )
            }
}

sealed class VenueVisitListItem {
    data class HeaderItem(val date: LocalDate) : VenueVisitListItem()
    data class ContentItem(val venueVisitItem: VenueVisitItem) : VenueVisitListItem()
}

interface VenueVisitItem {
    val venueVisit: VenueVisit
}

data class VenueVisitHistory(override val venueVisit: VenueVisit) : VenueVisitItem
