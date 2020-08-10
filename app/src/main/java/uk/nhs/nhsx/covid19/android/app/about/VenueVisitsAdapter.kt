package uk.nhs.nhsx.covid19.android.app.about

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_user_data.view.textDate
import kotlinx.android.synthetic.main.item_user_data.view.textVenueId
import kotlinx.android.synthetic.main.item_user_data.view.textVenueName
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.about.VenueVisitsAdapter.VenueVisitViewHolder
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.util.uiFormat
import java.time.ZoneId

class VenueVisitsAdapter(private val venueVisits: List<VenueVisit>) :
    RecyclerView.Adapter<VenueVisitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueVisitViewHolder =
        VenueVisitViewHolder(parent.inflate(R.layout.item_user_data))

    override fun getItemCount(): Int = venueVisits.size

    override fun onBindViewHolder(holder: VenueVisitViewHolder, position: Int) {
        holder.bind(venueVisits[position])
    }

    class VenueVisitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(venueVisit: VenueVisit) = with(itemView) {
            textVenueName.text = venueVisit.venue.organizationPartName
            textVenueId.text = venueVisit.venue.id
            val systemDefaultZone = ZoneId.systemDefault()

            val dateFrom = venueVisit.from.atZone(systemDefaultZone).toLocalDate().uiFormat()
            val dateTo = venueVisit.to.atZone(systemDefaultZone).toLocalDate().uiFormat()

            val timeFrom = venueVisit.from.atZone(systemDefaultZone).toLocalTime().uiFormat()
            val timeTo = venueVisit.to.atZone(systemDefaultZone).toLocalTime().uiFormat()

            val timeSpan = "$timeFrom - $timeTo"

            textDate.text = if (dateTo == dateFrom) {
                "$dateFrom $timeSpan"
            } else {
                "$dateFrom $timeSpan $dateTo"
            }
        }
    }
}

fun ViewGroup.inflate(layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, false)
}
