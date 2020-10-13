package uk.nhs.nhsx.covid19.android.app.widgets

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.annotation.AttrRes
import kotlinx.android.synthetic.main.view_status_option.view.statusOptionIcon
import kotlinx.android.synthetic.main.view_status_option.view.statusOptionIconContainer
import kotlinx.android.synthetic.main.view_status_option.view.statusOptionLinkIndicator
import kotlinx.android.synthetic.main.view_status_option.view.statusOptionText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.drawable
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.AMBER
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.YELLOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskIndicator
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.HIGH
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.LOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevel.MEDIUM
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getThemeColor

class AreaRiskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : StatusOptionView(context, attrs, defStyleAttr) {

    var areaRisk: RiskIndicator? = null
        set(value) {
            field = value
            setAreaRiskIndicator(areaRisk)
        }

    var oldAreaRisk: String? = null
        set(value) {
            field = value
            setOldAreaRiskIndicator(oldAreaRisk)
        }

    init {
        applyAttributes(context, attrs)
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AreaRiskView,
            0,
            0
        ).apply {
            text = getString(R.styleable.AreaRiskView_areaRiskText)

            statusOptionText.setPaddingRelative(0, 0, 0, 0)

            recycle()
        }
    }

    private fun setAreaRiskIndicator(riskIndicator: RiskIndicator?) {
        when (riskIndicator?.colorScheme) {
            NEUTRAL -> setNeutral()
            GREEN -> setGreen()
            YELLOW -> setYellow()
            AMBER -> setAmber()
            RED -> setRed()
        }
    }

    private fun setOldAreaRiskIndicator(areaRisk: String?) {
        when (areaRisk) {
            LOW.name -> setGreen()
            MEDIUM.name -> setYellow()
            HIGH.name -> setRed()
            else -> statusOptionIcon.setImageDrawable(null)
        }
    }

    private fun setNeutral() {
        setColors(
            R.attr.riskLevelNeutralPanelBackgroundColor,
            R.attr.riskLevelNeutralPanelTextColor,
            R.attr.riskLevelNeutralPanelIconTint,
            R.attr.riskLevelNeutralPanelChevronColor
        )
    }

    private fun setGreen() {
        setColors(
            R.attr.riskLevelGreenPanelBackgroundColor,
            R.attr.riskLevelGreenPanelTextColor,
            R.attr.riskLevelGreenPanelIconTint,
            R.attr.riskLevelGreenPanelChevronColor
        )
    }

    private fun setYellow() {
        setColors(
            R.attr.riskLevelYellowPanelBackgroundColor,
            R.attr.riskLevelYellowPanelTextColor,
            R.attr.riskLevelYellowPanelIconTint,
            R.attr.riskLevelYellowPanelChevronColor
        )
    }

    private fun setAmber() {
        setColors(
            R.attr.riskLevelAmberPanelBackgroundColor,
            R.attr.riskLevelAmberPanelTextColor,
            R.attr.riskLevelAmberPanelIconTint,
            R.attr.riskLevelAmberPanelChevronColor
        )
    }

    private fun setRed() {
        setColors(
            R.attr.riskLevelRedPanelBackgroundColor,
            R.attr.riskLevelRedPanelTextColor,
            R.attr.riskLevelRedPanelIconTint,
            R.attr.riskLevelRedPanelChevronColor
        )
    }

    private fun setColors(
        @AttrRes backgroundColorAttr: Int,
        @AttrRes textColorAttr: Int,
        @AttrRes iconTintColorAttr: Int,
        @AttrRes chevronColorAttr: Int
    ) {
        val backgroundColor = context.getThemeColor(backgroundColorAttr)
        val textColor = context.getThemeColor(textColorAttr)
        val iconTintColor = context.getThemeColor(iconTintColorAttr)
        val chevronColor = context.getThemeColor(chevronColorAttr)

        statusOptionIconContainer.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        this.backgroundTintList = ColorStateList.valueOf(backgroundColor)

        statusOptionText.setTextColor(textColor)
        statusOptionLinkIndicator.setColorFilter(chevronColor)

        statusOptionIcon.setImageResource(drawable.ic_location_white)
        statusOptionIcon.setColorFilter(iconTintColor)
    }
}
