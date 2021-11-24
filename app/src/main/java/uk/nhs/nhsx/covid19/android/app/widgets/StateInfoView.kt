package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewStateInfoBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading

class StateInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewStateInfoBinding.inflate(LayoutInflater.from(context), this)

    var stateText: String? = ""
        set(value) {
            field = value
            binding.stateTextView.text = stateText
        }

    var stateColor: Int = 0
        set(value) {
            field = value
            binding.stateColorView.setBackgroundColor(stateColor)
        }

    init {
        applyAttributes(context, attrs)
        binding.stateTextView.setUpAccessibilityHeading()
    }

    fun setStateInfoParams(stateInfoParams: StateInfoParams) {
        stateText = context.getString(stateInfoParams.text)
        stateColor = context.getColor(stateInfoParams.color)
    }

    override fun announceForAccessibility(error: CharSequence) {
        super.announceForAccessibility(stateText)
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StateInfoView,
            0,
            0
        ).apply {
            stateColor = getColor(R.styleable.StateInfoView_stateColor, 0)
            stateText = getString(context, R.styleable.StateInfoView_stateText)

            with(binding) {
                stateColorView.setBackgroundColor(stateColor)
                stateTextView.text = stateText
            }

            recycle()
        }
    }
}

data class StateInfoParams(
    @StringRes val text: Int,
    @ColorRes val color: Int
)
