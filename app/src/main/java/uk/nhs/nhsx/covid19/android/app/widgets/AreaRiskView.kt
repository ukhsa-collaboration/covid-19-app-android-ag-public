package uk.nhs.nhsx.covid19.android.app.widgets

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.databinding.ViewAreaRiskBinding
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorSchemeToRiskLevelPanel
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevelPanelTheme
import uk.nhs.nhsx.covid19.android.app.remote.data.convert
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getString
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpButtonType

class AreaRiskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewAreaRiskBinding.inflate(LayoutInflater.from(context), this)

    var text: String? = ""
        set(value) {
            field = value
            binding.areaRiskText.text = value
        }

    var areaRisk: RiskIndicator? = null
        set(value) {
            field = value
            setAreaRiskStyle(areaRisk)
        }

    private val transformer = ColorSchemeToRiskLevelPanel()

    init {
        applyAttributes(context, attrs)
        configureLayout()
        setUpAccessibility()
    }

    private fun setUpAccessibility() {
        setUpButtonType(binding.areaRiskText.text)
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AreaRiskView,
            0,
            0
        ).apply {
            text = getString(context, R.styleable.AreaRiskView_areaRiskText)

            recycle()
        }
    }

    private fun setAreaRiskStyle(riskIndicator: RiskIndicator?) {
        if (riskIndicator == null) return

        val riskPanelAttributes = transformer(riskIndicator.colorSchemeV2 ?: riskIndicator.colorScheme)
        setColors(context.convert(riskPanelAttributes))
    }

    private fun setColors(theme: RiskLevelPanelTheme) {
        with(theme) {
            with(binding) {
                areaRiskText.setTextColor(textColor)
                areaRiskIndicator.setImageResource(R.drawable.ic_location_white)
                areaRiskIndicator.setColorFilter(iconTintColor)
                areaRiskChevron.setColorFilter(chevronColor)
            }
            setBackgroundResource(backgroundDrawable)
        }
    }

    private fun configureLayout() {
        minimumHeight = 56.dpToPx.toInt()
        gravity = Gravity.CENTER_VERTICAL
        setSelectableItemForeground()
    }

    private fun setSelectableItemForeground() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(attr.selectableItemBackground, outValue, true)
        foreground = context.getDrawable(outValue.resourceId)
    }
}
