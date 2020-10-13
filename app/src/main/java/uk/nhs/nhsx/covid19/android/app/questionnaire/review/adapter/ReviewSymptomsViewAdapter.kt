package uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter

import android.os.Parcelable
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.parcel.Parcelize
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.NegativeHeader
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.PositiveHeader
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomItem.Question
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom

class SymptomsReviewViewAdapter(
    private val reviewSymptomItemsList: List<ReviewSymptomItem>,
    private val listener: (Question) -> Unit
) :
    RecyclerView.Adapter<ViewHolder>() {
    companion object {
        private const val ITEM_VIEW_TYPE_POSITIVE_HEADER = 0
        private const val ITEM_VIEW_TYPE_NEGATIVE_HEADER = 1
        private const val ITEM_VIEW_TYPE_SYMPTOM = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_POSITIVE_HEADER -> PositiveSymptomsHeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_NEGATIVE_HEADER -> NegativeSymptomsHeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_SYMPTOM -> ReviewSymptomViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun getItemCount() = reviewSymptomItemsList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is ReviewSymptomViewHolder -> {
                val symptomItem = reviewSymptomItemsList[position] as Question
                holder.bind(symptomItem, listener)
            }
        }
    }

    override fun getItemViewType(position: Int) =
        when (reviewSymptomItemsList[position]) {
            is PositiveHeader -> ITEM_VIEW_TYPE_POSITIVE_HEADER
            is NegativeHeader -> ITEM_VIEW_TYPE_NEGATIVE_HEADER
            else -> ITEM_VIEW_TYPE_SYMPTOM
        }
}

sealed class ReviewSymptomItem {
    @Parcelize
    data class Question(val symptom: Symptom, val isChecked: Boolean) :
        ReviewSymptomItem(),
        Parcelable

    object PositiveHeader : ReviewSymptomItem()
    object NegativeHeader : ReviewSymptomItem()
}
