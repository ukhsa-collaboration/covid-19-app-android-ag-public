package uk.nhs.nhsx.covid19.android.app.widgets

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.view_status_option.view.statusOptionIcon
import kotlinx.android.synthetic.main.view_status_option.view.statusOptionText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.util.dpToPx

class StatusOptionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var attrText: String? = ""

    init {
        initializeViews()
        applyAttributes(context, attrs)
    }

    override fun announceForAccessibility(error: CharSequence) {
        super.announceForAccessibility(attrText)
    }

    private fun initializeViews() {
        View.inflate(context, R.layout.view_status_option, this)
        configureLayout()
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StatusOptionView,
            0,
            0
        ).apply {
            val attrOptionIcon = getDrawable(R.styleable.StatusOptionView_optionIcon)
            attrText = getString(R.styleable.StatusOptionView_optionText)

            statusOptionText.text = attrText
            statusOptionIcon.setImageDrawable(attrOptionIcon)

            recycle()
        }
    }

    fun setText(@StringRes resId: Int) {
        statusOptionText.setText(resId)
    }

    private fun configureLayout() {
        minimumHeight =
            VIEW_HEIGHT_IN_PX
        gravity = Gravity.CENTER_VERTICAL
        setPadding(
            HORIZONTAL_PADDING_IN_PX, 0,
            HORIZONTAL_PADDING_IN_PX, 0
        )
        setBackgroundResource(R.drawable.status_option_background)
        setSelectableItemForeground()
    }

    private fun setSelectableItemForeground() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
    }

    companion object {
        val VIEW_HEIGHT_IN_PX: Int = 56.dpToPx.toInt()
        val HORIZONTAL_PADDING_IN_PX: Int = 16.dpToPx.toInt()
    }
}
