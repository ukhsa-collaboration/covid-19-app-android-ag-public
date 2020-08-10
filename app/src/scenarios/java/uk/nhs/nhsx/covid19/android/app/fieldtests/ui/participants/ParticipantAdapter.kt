package uk.nhs.nhsx.covid19.android.app.fieldtests.ui.participants

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.nhs.nhsx.covid19.android.app.R

class ParticipantAdapter(private val participants: List<String>) :
    RecyclerView.Adapter<ParticipantAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_participant, parent, false) as TextView
        return ViewHolder(textView)
    }

    override fun getItemCount(): Int {
        return participants.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = participants[position]
    }

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
