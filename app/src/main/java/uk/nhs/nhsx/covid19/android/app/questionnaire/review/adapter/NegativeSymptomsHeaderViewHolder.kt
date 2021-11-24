package uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ReviewSymptomsHeaderBinding

class NegativeSymptomsHeaderViewHolder(itemBinding: ReviewSymptomsHeaderBinding) :
    RecyclerView.ViewHolder(itemBinding.root) {

    companion object {
        fun from(parent: ViewGroup): NegativeSymptomsHeaderViewHolder {
            val itemBinding = ReviewSymptomsHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)

            with(itemBinding) {
                imageSymptomMark.setImageResource(R.drawable.ic_negative_symptom)
                textSymptomMessage.setText(R.string.questionnaire_negative_symptoms_review_message)

                return NegativeSymptomsHeaderViewHolder(
                    this
                )
            }
        }
    }
}
