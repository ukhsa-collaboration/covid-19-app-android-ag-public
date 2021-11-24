package uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ItemReviewSymptomBinding
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener

class ReviewSymptomViewHolder(private val itemBinding: ItemReviewSymptomBinding) :
    RecyclerView.ViewHolder(itemBinding.root) {
    companion object {
        fun from(parent: ViewGroup): ReviewSymptomViewHolder {
            return ReviewSymptomViewHolder(
                ItemReviewSymptomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }

    fun bind(question: Question, listener: (Question) -> Unit) = with(itemBinding) {
        val symptomName = question.symptom.title.translate()
        textReviewSymptom.text = symptomName
        textChange.setOnSingleClickListener { listener(question) }
        textChange.contentDescription = root.context.getString(
            R.string.questionnaire_change_announcement,
            question.symptom.title.translate()
        )
    }
}
