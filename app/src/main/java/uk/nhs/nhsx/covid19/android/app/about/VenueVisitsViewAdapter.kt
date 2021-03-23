package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.item_venue_visit.view.imageDeleteVenueVisit
import kotlinx.android.synthetic.main.item_venue_visit.view.textDate
import kotlinx.android.synthetic.main.item_venue_visit.view.textVenueId
import kotlinx.android.synthetic.main.item_venue_visit.view.textVenueName
import kotlinx.android.synthetic.main.item_venue_visit.view.textVenuePostCode
import kotlinx.android.synthetic.main.item_venue_visit_header.view.dateHeader
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitsViewAdapter.VenueVisitEntry.VenueVisitEntryHeader
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitsViewAdapter.VenueVisitEntry.VenueVisitEntryItem
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.uiDate
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.LocalDate

class VenueVisitsViewAdapter(
    private val venueVisitEntries: List<VenueVisitEntry>,
    private val showDeleteIcon: Boolean,
    private val deletionListener: (VenueVisit) -> Unit
) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> VenueVisitHeaderHolder.from(parent)
            ITEM_VIEW_TYPE_VENUE_VISIT -> VenueVisitViewHolder.from(parent, deletionListener)
            else -> throw RuntimeException("Unknown viewType $viewType")
        }
    }

    override fun getItemCount(): Int = venueVisitEntries.size

    override fun getItemViewType(position: Int): Int =
        when (venueVisitEntries[position]) {
            is VenueVisitEntryItem -> ITEM_VIEW_TYPE_VENUE_VISIT
            is VenueVisitEntryHeader -> ITEM_VIEW_TYPE_HEADER
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is VenueVisitViewHolder -> {
                val item = venueVisitEntries[position] as VenueVisitEntryItem
                holder.bind(item, showDeleteIcon)
            }
            is VenueVisitHeaderHolder -> {
                val item = venueVisitEntries[position] as VenueVisitEntryHeader
                holder.bind(item)
            }
        }
    }

    class VenueVisitViewHolder(itemView: View, private val deletionListener: (VenueVisit) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        fun bind(venueVisitItem: VenueVisitEntryItem, showDeleteIcon: Boolean) = with(itemView) {
            textVenueName.text = venueVisitItem.venueVisit.venue.organizationPartName
            textVenuePostCode.text =
                venueVisitItem.venueVisit.venue.postCode ?: context.getString(R.string.venue_history_postcode_unavailable)
            val spelledPostCode = venueVisitItem.venueVisit.venue.postCode?.toCharArray()?.joinToString(ZERO_WIDTH_NON_BREAKING_SPACE)
            textVenuePostCode.contentDescription =
                spelledPostCode ?: context.getString(R.string.venue_history_postcode_unavailable)
            textVenueId.text = venueVisitItem.venueVisit.venue.id
            textDate.text = venueVisitItem.venueVisit.uiDate(context)

            if (showDeleteIcon) {
                imageDeleteVenueVisit.visible()
                (textVenueName.layoutParams as ConstraintLayout.LayoutParams).marginStart = 0
                (textVenuePostCode.layoutParams as LinearLayout.LayoutParams).marginStart = 0
                (textDate.layoutParams as ConstraintLayout.LayoutParams).marginStart = 0
            } else {
                imageDeleteVenueVisit.gone()
                val marginStart = context.resources.getDimension(R.dimen.margin_horizontal_list_item).toInt()
                (textVenueName.layoutParams as ConstraintLayout.LayoutParams).marginStart = marginStart
                (textVenuePostCode.layoutParams as LinearLayout.LayoutParams).marginStart = marginStart
                (textDate.layoutParams as ConstraintLayout.LayoutParams).marginStart = marginStart
            }

            imageDeleteVenueVisit.setOnSingleClickListener {
                deletionListener(venueVisitItem.venueVisit)
            }
        }

        companion object {
            fun from(parent: ViewGroup, deletionListener: (VenueVisit) -> Unit): VenueVisitViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_venue_visit, parent, false)

                return VenueVisitViewHolder(
                    view, deletionListener
                )
            }
        }
    }

    class VenueVisitHeaderHolder(view: View, val context: Context) :
        RecyclerView.ViewHolder(view) {
        fun bind(venueVisitHeader: VenueVisitEntryHeader) = with(itemView) {
            dateHeader.text = venueVisitHeader.date.uiFormat(context)
            dateHeader.setUpAccessibilityHeading(
                context.getString(
                    R.string.accessibility_announcement_venue_checkin_date,
                    venueVisitHeader.date.uiLongFormat(context)
                )
            )
        }

        companion object {
            fun from(parent: ViewGroup): VenueVisitHeaderHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_venue_visit_header, parent, false)

                return VenueVisitHeaderHolder(
                    view,
                    parent.context
                )
            }
        }
    }

    sealed class VenueVisitEntry {
        data class VenueVisitEntryItem(val venueVisit: VenueVisit) : VenueVisitEntry()
        data class VenueVisitEntryHeader(val date: LocalDate) : VenueVisitEntry()
    }

    companion object {
        private const val ITEM_VIEW_TYPE_HEADER = 0
        private const val ITEM_VIEW_TYPE_VENUE_VISIT = 1
        const val ZERO_WIDTH_NON_BREAKING_SPACE = "\ufeff"
    }
}
