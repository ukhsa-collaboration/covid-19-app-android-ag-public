package uk.nhs.nhsx.covid19.android.app.exposure.questionnaire.review

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ItemOptOutResponseGroupBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setOnSingleClickListener
import java.time.LocalDate

class OptOutResponseGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ItemOptOutResponseGroupBinding.inflate(LayoutInflater.from(context), this, true)

    var onChangeListener: () -> Unit = {}
        set(value) {
            field = value
            binding.responseGroupChanger.setOnSingleClickListener(value)
        }

    fun setResponses(
        responses: List<OptOutResponseEntry>,
        ageLimitDate: LocalDate,
        lastDoseDateLimit: LocalDate
    ) {
        binding.responseGroupUserInput.adapter = OptOutResponseAdapter(responses, ageLimitDate, lastDoseDateLimit)
    }

    init {
        initialize()
        stylise(context, attrs)
    }

    private fun initialize() {
        binding.responseGroupUserInput.layoutManager = LinearLayoutManager(context)
    }

    private fun stylise(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.OptOutResponseGroup, 0, 0)
            .apply {
                with(binding) {

                    responseGroupTitle.text = getString(context, R.styleable.OptOutResponseGroup_groupTitle)
                    responseGroupChanger.text =
                        getString(context, R.styleable.OptOutResponseGroup_groupChangeButtonText)
                    responseGroupChanger.contentDescription =
                        getString(context, R.styleable.OptOutResponseGroup_groupChangeButtonContentDescription)
                    recycle()
                }
            }
    }
}
