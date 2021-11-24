package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ItemOptOutResponseBinding

class OptOutResponseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val binding = ItemOptOutResponseBinding.inflate(LayoutInflater.from(context), this, true)

    private var question: String = ""
        set(value) {
            field = value
            binding.optOutQuestion.text = question
        }

    private var response: Boolean = false
        set(value) {
            field = value
            val optOutAnswerIconResId = if (response) R.drawable.ic_tick_green else R.drawable.ic_cross_red
            binding.optOutAnswerIcon.setImageResource(optOutAnswerIconResId)
        }

    private var responseText: String = ""
        set(value) {
            field = value
            binding.optOutAnswerText.text = responseText
        }

    fun setShouldShowDivider(shouldShowDivider: Boolean) {
        binding.responseItemDivider.root.isVisible = shouldShowDivider
    }

    fun setResponse(entry: OptOutResponse) {
        question = entry.question
        response = entry.response
        responseText = entry.responseText
        contentDescription = entry.contentDescription
    }

    data class OptOutResponse(
        val question: String,
        val response: Boolean,
        val contentDescription: String,
        val responseText: String
    )
}
