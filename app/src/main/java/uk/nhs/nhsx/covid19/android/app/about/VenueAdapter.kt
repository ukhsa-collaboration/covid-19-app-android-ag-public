package uk.nhs.nhsx.covid19.android.app.about

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitListItem.ContentItem
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitListItem.HeaderItem
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit

typealias onVisitSelected = (VenueVisit) -> Unit

abstract class VenueAdapter<T : ViewHolder>(
    protected val venueVisitListItems: List<VenueVisitListItem>,
    val createItemInstance: (ViewGroup, onVisitSelected) -> ViewHolder
) :
    RecyclerView.Adapter<ViewHolder>() {

    override fun getItemCount(): Int = venueVisitListItems.size
    abstract val itemListener: onVisitSelected

    override fun getItemViewType(position: Int): Int =
        when (venueVisitListItems[position]) {
            is ContentItem -> ITEM_VIEW_TYPE_VENUE_VISIT
            is HeaderItem -> ITEM_VIEW_TYPE_HEADER
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> VenueVisitHeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_VENUE_VISIT -> createItemInstance(parent, itemListener)
            else -> throw RuntimeException("Unknown viewType $viewType")
        }
    }

    abstract fun bindContent(holder: ViewHolder, item: ContentItem)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is VenueVisitHeaderViewHolder -> {
                val item = venueVisitListItems[position] as HeaderItem
                holder.bind(item.date)
            }
            else -> {
                bindContent(holder, venueVisitListItems[position] as ContentItem)
            }
        }
    }

    companion object {
        const val ITEM_VIEW_TYPE_HEADER = 0
        const val ITEM_VIEW_TYPE_VENUE_VISIT = 1
    }
}
