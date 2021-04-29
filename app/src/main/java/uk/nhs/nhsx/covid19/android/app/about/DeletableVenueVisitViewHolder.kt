package uk.nhs.nhsx.covid19.android.app.about

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import kotlinx.android.synthetic.main.item_venue_visit.view.imageDeleteVenueVisit
import kotlinx.android.synthetic.main.item_venue_visit_content.view.venueItemContent
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.dimen
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitListItem.ContentItem
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible

class DeletableVenueVisitViewHolder(itemView: View, private val deletionListener: onVisitSelected) :
    VenueVisitViewHolder(itemView) {
    fun bind(venueVisitContentItem: ContentItem, showDeleteIcon: Boolean) = with(itemView) {
        val venueVisit = venueVisitContentItem.venueVisitItem.venueVisit
        super.bind(venueVisit)
        if (showDeleteIcon) {
            imageDeleteVenueVisit.visible()
            (venueItemContent.layoutParams as LinearLayout.LayoutParams).marginStart = 0
        } else {
            imageDeleteVenueVisit.gone()
            with(context.resources.getDimension(dimen.margin_horizontal_list_item).toInt()) {
                (venueItemContent.layoutParams as LayoutParams).marginStart = this
            }
        }

        imageDeleteVenueVisit.setOnSingleClickListener {
            deletionListener(venueVisit)
        }
    }

    companion object {
        fun from(parent: ViewGroup, deletionListener: (VenueVisit) -> Unit): DeletableVenueVisitViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_venue_visit, parent, false)

            return DeletableVenueVisitViewHolder(
                view, deletionListener
            )
        }
    }
}
