package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_venue_visit_header.view.dateHeader
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import java.time.LocalDate

class VenueVisitHeaderViewHolder(view: View, val context: Context) :
    RecyclerView.ViewHolder(view) {
    fun bind(venueVisitDate: LocalDate) = with(itemView) {
        dateHeader.text = venueVisitDate.uiFormat(context)
        dateHeader.setUpAccessibilityHeading(
            context.getString(
                R.string.accessibility_announcement_venue_checkin_date,
                venueVisitDate.uiLongFormat(context)
            )
        )
    }

    companion object {
        fun from(parent: ViewGroup): VenueVisitHeaderViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_venue_visit_header, parent, false)

            return VenueVisitHeaderViewHolder(
                view,
                parent.context
            )
        }
    }
}
