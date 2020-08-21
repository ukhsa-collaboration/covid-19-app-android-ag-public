package uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_review_symptom.view.textChange
import kotlinx.android.synthetic.main.item_review_symptom.view.textReviewSymptom
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question

class ReviewSymptomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    companion object {
        fun from(parent: ViewGroup): ReviewSymptomViewHolder {
            return ReviewSymptomViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_review_symptom, parent, false)
            )
        }
    }

    fun bind(question: Question, listener: (Question) -> Unit) = with(itemView) {
        val symptomName = question.symptom.title.translate()
        val reviewSymptomMessage = if (question.isChecked) {
            context.getString(R.string.questionnaire_yes_i_have_symptom, symptomName)
        } else {
            context.getString(R.string.questionnaire_no_i_dont_have_symptom, symptomName)
        }
        textReviewSymptom.text = reviewSymptomMessage
        textChange.setOnClickListener { listener(question) }
        textChange.contentDescription =
            context.getString(
                R.string.questionnaire_change_announcement,
                question.symptom.title.translate()
            )
    }
}
