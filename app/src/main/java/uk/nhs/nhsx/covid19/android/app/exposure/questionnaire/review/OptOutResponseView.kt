package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_opt_out_response.view.optOutAnswerIcon
import kotlinx.android.synthetic.main.item_opt_out_response.view.optOutAnswerText
import kotlinx.android.synthetic.main.item_opt_out_response.view.optOutQuestion
import kotlinx.android.synthetic.main.item_opt_out_response.view.responseItemDivider
import uk.nhs.nhsx.covid19.android.app.R

class OptOutResponseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var question: String = ""
        set(value) {
            field = value
            optOutQuestion.text = question
        }

    private var response: Boolean = false
        set(value) {
            field = value
            val optOutAnswerIconResId = if (response) R.drawable.ic_tick_green else R.drawable.ic_cross_red
            optOutAnswerIcon.setImageResource(optOutAnswerIconResId)
        }

    private var responseText: String = ""
        set(value) {
            field = value
            optOutAnswerText.text = responseText
        }

    fun setShouldShowDivider(shouldShowDivider: Boolean) {
        responseItemDivider.isVisible = shouldShowDivider
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.item_opt_out_response, this)
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
