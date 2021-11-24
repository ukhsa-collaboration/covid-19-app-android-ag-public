package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ItemVenueVisitHeaderBinding
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import java.time.LocalDate

class VenueVisitHeaderViewHolder(private val itemBinding: ItemVenueVisitHeaderBinding, val context: Context) :
    RecyclerView.ViewHolder(itemBinding.root) {

    fun bind(venueVisitDate: LocalDate) = with(itemBinding) {
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
            val binding = ItemVenueVisitHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)

            return VenueVisitHeaderViewHolder(
                binding,
                parent.context
            )
        }
    }
}
