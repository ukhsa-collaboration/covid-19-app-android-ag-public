package uk.nhs.nhsx.covid19.android.app.about

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_venue_visit_content.view.textDate
import kotlinx.android.synthetic.main.item_venue_visit_content.view.textVenueId
import kotlinx.android.synthetic.main.item_venue_visit_content.view.textVenueName
import kotlinx.android.synthetic.main.item_venue_visit_content.view.textVenuePostCode
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.uiDate
import uk.nhs.nhsx.covid19.android.app.util.viewutils.toSpelledString

abstract class VenueVisitViewHolder(itemView: View) :
    RecyclerView.ViewHolder(itemView) {

    protected fun bind(venueVisit: VenueVisit) = with(itemView) {
        textVenueName.text = venueVisit.venue.organizationPartName
        textVenuePostCode.text =
            venueVisit.venue.postCode ?: context.getString(R.string.venue_history_postcode_unavailable)
        val spelledPostCode =
            venueVisit.venue.postCode?.toSpelledString()
        textVenuePostCode.contentDescription =
            spelledPostCode ?: context.getString(R.string.venue_history_postcode_unavailable)
        textVenueId.text = venueVisit.venue.id
        textDate.text = venueVisit.uiDate(context)
    }
}
