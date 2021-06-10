package uk.nhs.nhsx.covid19.android.app.widgets

import android.R.attr
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import kotlinx.android.synthetic.main.view_area_risk.view.areaRiskChevron
import kotlinx.android.synthetic.main.view_area_risk.view.areaRiskIndicator
import kotlinx.android.synthetic.main.view_area_risk.view.areaRiskText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.AMBER
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.BLACK
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.MAROON
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.YELLOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.util.viewutils.dpToPx
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getThemeColor
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getThemeDrawableResId
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setUpButtonType

class AreaRiskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var text: String? = ""
        set(value) {
            field = value
            areaRiskText.text = value
        }

    var areaRisk: RiskIndicator? = null
        set(value) {
            field = value
            setAreaRiskStyle(areaRisk)
        }

    init {
        View.inflate(context, R.layout.view_area_risk, this)
        applyAttributes(context, attrs)
        configureLayout()
        setUpAccessibility()
    }

    private fun setUpAccessibility() {
        setUpButtonType(areaRiskText.text)
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AreaRiskView,
            0,
            0
        ).apply {
            text = getString(R.styleable.AreaRiskView_areaRiskText)

            recycle()
        }
    }

    private fun setAreaRiskStyle(riskIndicator: RiskIndicator?) {
        if (riskIndicator == null) return

        when (riskIndicator.colorSchemeV2 ?: riskIndicator.colorScheme) {
            NEUTRAL -> setNeutral()
            GREEN -> setGreen()
            YELLOW -> setYellow()
            AMBER -> setAmber()
            RED -> setRed()
            MAROON -> setMaroon()
            BLACK -> setBlack()
        }
    }

    private fun setNeutral() {
        setColors(
            R.attr.riskLevelNeutralPanelTextColor,
            R.attr.riskLevelNeutralPanelIconTint,
            R.attr.riskLevelNeutralPanelChevronColor,
            R.attr.riskLevelNeutralPanelBackgroundDrawable
        )
    }

    private fun setGreen() {
        setColors(
            R.attr.riskLevelGreenPanelTextColor,
            R.attr.riskLevelGreenPanelIconTint,
            R.attr.riskLevelGreenPanelChevronColor,
            R.attr.riskLevelGreenPanelBackgroundDrawable
        )
    }

    private fun setYellow() {
        setColors(
            R.attr.riskLevelYellowPanelTextColor,
            R.attr.riskLevelYellowPanelIconTint,
            R.attr.riskLevelYellowPanelChevronColor,
            R.attr.riskLevelYellowPanelBackgroundDrawable
        )
    }

    private fun setAmber() {
        setColors(
            R.attr.riskLevelAmberPanelTextColor,
            R.attr.riskLevelAmberPanelIconTint,
            R.attr.riskLevelAmberPanelChevronColor,
            R.attr.riskLevelAmberPanelBackgroundDrawable
        )
    }

    private fun setRed() {
        setColors(
            R.attr.riskLevelRedPanelTextColor,
            R.attr.riskLevelRedPanelIconTint,
            R.attr.riskLevelRedPanelChevronColor,
            R.attr.riskLevelRedPanelBackgroundDrawable
        )
    }

    private fun setMaroon() {
        setColors(
            R.attr.riskLevelMaroonPanelTextColor,
            R.attr.riskLevelMaroonPanelIconTint,
            R.attr.riskLevelMaroonPanelChevronColor,
            R.attr.riskLevelMaroonPanelBackgroundDrawable
        )
    }

    private fun setBlack() {
        setColors(
            R.attr.riskLevelBlackPanelTextColor,
            R.attr.riskLevelBlackPanelIconTint,
            R.attr.riskLevelBlackPanelChevronColor,
            R.attr.riskLevelBlackPanelBackgroundDrawable
        )
    }

    private fun setColors(
        @AttrRes textColorAttr: Int,
        @AttrRes iconTintColorAttr: Int,
        @AttrRes chevronColorAttr: Int,
        @AttrRes backgroundDrawableAttr: Int
    ) {
        val textColor = context.getThemeColor(textColorAttr)
        val iconTintColor = context.getThemeColor(iconTintColorAttr)
        val chevronColor = context.getThemeColor(chevronColorAttr)
        val backgroundDrawableResId = context.getThemeDrawableResId(backgroundDrawableAttr)

        areaRiskText.setTextColor(textColor)
        areaRiskIndicator.setImageResource(R.drawable.ic_location_white)
        areaRiskIndicator.setColorFilter(iconTintColor)
        areaRiskChevron.setColorFilter(chevronColor)
        setBackgroundResource(backgroundDrawableResId)
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
