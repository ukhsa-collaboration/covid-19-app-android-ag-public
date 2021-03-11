package uk.nhs.nhsx.covid19.android.app.about

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_user_data.view.imageDeleteVenueVisit
import kotlinx.android.synthetic.main.item_user_data.view.textDate
import kotlinx.android.synthetic.main.item_user_data.view.textVenueId
import kotlinx.android.synthetic.main.item_user_data.view.textVenueName
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitsViewAdapter.VenueVisitViewHolder
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.inflate
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import uk.nhs.nhsx.covid19.android.app.util.viewutils.visible
import java.time.ZoneId

class VenueVisitsViewAdapter(
    private val venueVisits: List<VenueVisit>,
    private val showDeleteIcon: Boolean,
    private val deletionListener: (VenueVisit) -> Unit
) : RecyclerView.Adapter<VenueVisitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueVisitViewHolder =
        VenueVisitViewHolder(parent.inflate(R.layout.item_user_data), deletionListener)

    override fun getItemCount(): Int = venueVisits.size

    override fun onBindViewHolder(holder: VenueVisitViewHolder, position: Int) {
        holder.bind(venueVisits[position], showDeleteIcon, position)
    }

    class VenueVisitViewHolder(itemView: View, private val deletionListener: (VenueVisit) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        fun bind(venueVisit: VenueVisit, showDeleteIcon: Boolean, position: Int) = with(itemView) {
            textVenueName.text = venueVisit.venue.organizationPartName
            textVenueId.text = venueVisit.venue.id
            val systemDefaultZone = ZoneId.systemDefault()

            // Subtract 1 second in order to show time of 23:59 instead of 00:00
            val uiTo = venueVisit.to.minusSeconds(1L)

            val dateFrom = venueVisit.from.atZone(systemDefaultZone).toLocalDate().uiFormat(context)
            val dateTo = uiTo.atZone(systemDefaultZone).toLocalDate().uiFormat(context)

            val timeFrom = venueVisit.from.atZone(systemDefaultZone).toLocalTime().uiFormat()
            val timeTo = uiTo.atZone(systemDefaultZone).toLocalTime().uiFormat()

            val timeSpan = "$timeFrom - $timeTo"

            textDate.text = if (dateTo == dateFrom) {
                "$dateFrom $timeSpan"
            } else {
                "$dateFrom $timeSpan $dateTo"
            }

            if (showDeleteIcon) {
                imageDeleteVenueVisit.visible()
                (textVenueName.layoutParams as LinearLayout.LayoutParams).marginStart = 0
                (textDate.layoutParams as ConstraintLayout.LayoutParams).marginStart = 0
            } else {
                imageDeleteVenueVisit.gone()
                (textVenueName.layoutParams as LinearLayout.LayoutParams).marginStart =
                    context.resources.getDimension(R.dimen.margin_horizontal_list_item).toInt()
                (textDate.layoutParams as ConstraintLayout.LayoutParams).marginStart =
                    context.resources.getDimension(R.dimen.margin_horizontal_list_item).toInt()
            }

            imageDeleteVenueVisit.setOnSingleClickListener {
                deletionListener(venueVisit)
            }
        }
    }
}
