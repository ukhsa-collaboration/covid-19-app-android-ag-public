package uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.review_symptoms_header.view.imageSymptomMark
import kotlinx.android.synthetic.main.review_symptoms_header.view.textSymptomMessage
import uk.nhs.nhsx.covid19.android.app.R

class PositiveSymptomsHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    companion object {
        fun from(parent: ViewGroup): PositiveSymptomsHeaderViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.review_symptoms_header, parent, false)

            with(view) {
                imageSymptomMark.setImageResource(R.drawable.ic_positive_symptom)
                textSymptomMessage.setText(R.string.questionnaire_positive_symptoms_review_message)
            }

            return PositiveSymptomsHeaderViewHolder(view)
        }
    }
}
