package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.view_state_info.view.stateColorView
import kotlinx.android.synthetic.main.view_state_info.view.stateTextView
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpAccessibilityHeading
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString

class StateInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var stateText: String? = ""
        set(value) {
            field = value
            stateTextView.text = stateText
        }

    var stateColor: Int = 0
        set(value) {
            field = value
            stateColorView.setBackgroundColor(stateColor)
        }

    init {
        initializeViews()
        applyAttributes(context, attrs)
        stateTextView.setUpAccessibilityHeading()
    }

    fun setStateInfoParams(stateInfoParams: StateInfoParams) {
        stateText = context.getString(stateInfoParams.text)
        stateColor = context.getColor(stateInfoParams.color)
    }

    override fun announceForAccessibility(error: CharSequence) {
        super.announceForAccessibility(stateText)
    }

    private fun initializeViews() {
        LayoutInflater.from(context).inflate(R.layout.view_state_info, this, true)
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

            stateColorView.setBackgroundColor(stateColor)
            stateTextView.text = stateText

            recycle()
        }
    }
}

data class StateInfoParams(
    @StringRes val text: Int,
    @ColorRes val color: Int
)
