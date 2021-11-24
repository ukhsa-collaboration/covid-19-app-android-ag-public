package uk.nhs.nhsx.covid19.android.app.widgets

import android.R.attr
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewStatusOptionBinding
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpButtonType
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpLinkTypeWithBrowserWarning

class StatusOptionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding by lazy { ViewStatusOptionBinding.inflate(LayoutInflater.from(context), this) }

    var text: String? = ""
        set(value) {
            field = value
            binding.statusOptionText.text = value
        }

    private var attrIsExternalLink: Boolean = false

    init {
        configureLayout()
        applyAttributes(context, attrs)
        setUpAccessibility()
    }

    private fun setUpAccessibility() {
        if (attrIsExternalLink) {
            setUpLinkTypeWithBrowserWarning(binding.statusOptionText.text)
        } else {
            setUpButtonType(binding.statusOptionText.text)
        }
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.StatusOptionView,
            0,
            0
        ).apply {
            val attrOptionIcon = getDrawable(R.styleable.StatusOptionView_optionIcon)
            val attrOptionIconBackgroundColor =
                getColor(R.styleable.StatusOptionView_optionIconBackgroundColor, -1)

            text = getString(context, R.styleable.StatusOptionView_optionText)
            attrIsExternalLink = getBoolean(R.styleable.StatusOptionView_optionExternalLink, false)

            with(binding) {
                statusOptionText.text = text
                statusOptionText.setPaddingRelative(16.dpToPx.toInt(), 0, 0, 0)

                statusOptionIconContainer.backgroundTintList =
                    ColorStateList.valueOf(attrOptionIconBackgroundColor)

                attrOptionIcon?.isAutoMirrored = true
                statusOptionIcon.setImageDrawable(attrOptionIcon)

                val linkIndicator =
                    context.getDrawable(if (attrIsExternalLink) R.drawable.ic_link_status_option_view else R.drawable.ic_chevron_right)
                linkIndicator?.isAutoMirrored = true
                statusOptionLinkIndicator.setImageDrawable(linkIndicator)
            }
            recycle()
        }
    }

    private fun configureLayout() {
        minimumHeight = 56.dpToPx.toInt()
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.status_option_background)
        setSelectableItemForeground()
    }

    private fun setSelectableItemForeground() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
    }
}
