package uk.nhs.nhsx.covid19.android.app.about

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import androidx.recyclerview.widget.RecyclerView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.dimen
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitListItem.ContentItem
import uk.nhs.nhsx.covid19.android.app.databinding.ItemVenueVisitBinding
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.uiDate
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class DeletableVenueVisitViewHolder(
    private val itemBinding: ItemVenueVisitBinding,
    private val deletionListener: onVisitSelected
) : RecyclerView.ViewHolder(itemBinding.root) {

    fun bind(venueVisitContentItem: ContentItem, showDeleteIcon: Boolean) = with(itemView) {
        val venueVisit = venueVisitContentItem.venueVisitItem.venueVisit

        with(itemBinding.venueVisitContentItem) {
            textVenueName.text = venueVisit.venue.organizationPartName
            textVenuePostCode.text = venueVisit.venue.formattedPostCode
                ?: context.getString(R.string.venue_history_postcode_unavailable)
            textVenueId.text = venueVisit.venue.id
            textDate.text = venueVisit.uiDate(context)
        }

        if (showDeleteIcon) {
            itemBinding.imageDeleteVenueVisit.visible()
            (itemBinding.venueVisitContentItem.venueItemContent.layoutParams as LinearLayout.LayoutParams).marginStart =
                0
        } else {
            itemBinding.imageDeleteVenueVisit.gone()
            with(context.resources.getDimension(dimen.margin_horizontal_list_item).toInt()) {
                (itemBinding.venueVisitContentItem.venueItemContent.layoutParams as LayoutParams).marginStart = this
            }
        }

        itemBinding.imageDeleteVenueVisit.setOnSingleClickListener {
            deletionListener(venueVisit)
        }
    }

    companion object {
        fun from(parent: ViewGroup, deletionListener: (VenueVisit) -> Unit): DeletableVenueVisitViewHolder {
            val binding = ItemVenueVisitBinding.inflate(LayoutInflater.from(parent.context), parent, false)

            return DeletableVenueVisitViewHolder(binding, deletionListener)
        }
    }
}
