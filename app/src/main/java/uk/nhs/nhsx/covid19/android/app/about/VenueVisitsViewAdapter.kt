package uk.nhs.nhsx.covid19.android.app.about

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitListItem.ContentItem
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit

class VenueVisitsViewAdapter(
    venueVisitListItems: List<VenueVisitListItem>,
    private val showDeleteIcon: Boolean,
    private val deletionListener: onVisitSelected
) : VenueAdapter<DeletableVenueVisitViewHolder>(venueVisitListItems, DeletableVenueVisitViewHolder::from) {

    override val itemListener: (VenueVisit) -> Unit
        get() = deletionListener

    override fun bindContent(holder: ViewHolder, item: ContentItem) {
        (holder as DeletableVenueVisitViewHolder).bind(item, showDeleteIcon)
    }
}
