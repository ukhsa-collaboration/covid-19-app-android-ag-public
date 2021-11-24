package uk.nhs.nhsx.covid19.android.app.remote.data

import android.content.Context
import androidx.annotation.AttrRes
import uk.nhs.nhsx.covid19.android.app.R.attr
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.AMBER
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.BLACK
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.MAROON
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.YELLOW
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevelPanelAttributes.Amber
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevelPanelAttributes.Black
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevelPanelAttributes.Green
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevelPanelAttributes.Maroon
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevelPanelAttributes.Neutral
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevelPanelAttributes.Red
import uk.nhs.nhsx.covid19.android.app.remote.data.RiskLevelPanelAttributes.Yellow
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getThemeColor
import uk.nhs.nhsx.covid19.android.app.util.viewutils.getThemeDrawableResId
import javax.inject.Inject

class ColorSchemeToRiskLevelPanel @Inject constructor() {

    operator fun invoke(scheme: ColorScheme): RiskLevelPanelAttributes = when (scheme) {
        NEUTRAL -> Neutral
        GREEN -> Green
        YELLOW -> Yellow
        AMBER -> Amber
        RED -> Red
        MAROON -> Maroon
        BLACK -> Black
    }
}

internal fun Context.convert(panelAttributes: RiskLevelPanelAttributes): RiskLevelPanelTheme = RiskLevelPanelTheme(
    textColor = getThemeColor(panelAttributes.textColor),
    iconTintColor = getThemeColor(panelAttributes.iconTintColor),
    chevronColor = getThemeColor(panelAttributes.chevronColor),
    backgroundDrawable = getThemeDrawableResId(panelAttributes.backgroundDrawable)
)

data class RiskLevelPanelTheme(
    val textColor: Int,
    val iconTintColor: Int,
    val chevronColor: Int,
    val backgroundDrawable: Int
)

sealed class RiskLevelPanelAttributes(
    @AttrRes val textColor: Int,
    @AttrRes val iconTintColor: Int,
    @AttrRes val chevronColor: Int,
    @AttrRes val backgroundDrawable: Int
) {
    object Neutral : RiskLevelPanelAttributes(
        textColor = attr.riskLevelNeutralPanelTextColor,
        iconTintColor = attr.riskLevelNeutralPanelIconTint,
        chevronColor = attr.riskLevelNeutralPanelChevronColor,
        backgroundDrawable = attr.riskLevelNeutralPanelBackgroundDrawable
    )

    object Green : RiskLevelPanelAttributes(
        textColor = attr.riskLevelGreenPanelTextColor,
        iconTintColor = attr.riskLevelGreenPanelIconTint,
        chevronColor = attr.riskLevelGreenPanelChevronColor,
        backgroundDrawable = attr.riskLevelGreenPanelBackgroundDrawable
    )

    object Yellow : RiskLevelPanelAttributes(
        textColor = attr.riskLevelYellowPanelTextColor,
        iconTintColor = attr.riskLevelYellowPanelIconTint,
        chevronColor = attr.riskLevelYellowPanelChevronColor,
        backgroundDrawable = attr.riskLevelYellowPanelBackgroundDrawable
    )

    object Amber : RiskLevelPanelAttributes(
        textColor = attr.riskLevelAmberPanelTextColor,
        iconTintColor = attr.riskLevelAmberPanelIconTint,
        chevronColor = attr.riskLevelAmberPanelChevronColor,
        backgroundDrawable = attr.riskLevelAmberPanelBackgroundDrawable
    )

    object Red : RiskLevelPanelAttributes(
        textColor = attr.riskLevelRedPanelTextColor,
        iconTintColor = attr.riskLevelRedPanelIconTint,
        chevronColor = attr.riskLevelRedPanelChevronColor,
        backgroundDrawable = attr.riskLevelRedPanelBackgroundDrawable
    )

    object Maroon : RiskLevelPanelAttributes(
        textColor = attr.riskLevelMaroonPanelTextColor,
        iconTintColor = attr.riskLevelMaroonPanelIconTint,
        chevronColor = attr.riskLevelMaroonPanelChevronColor,
        backgroundDrawable = attr.riskLevelMaroonPanelBackgroundDrawable
    )

    object Black : RiskLevelPanelAttributes(
        textColor = attr.riskLevelBlackPanelTextColor,
        iconTintColor = attr.riskLevelBlackPanelIconTint,
        chevronColor = attr.riskLevelBlackPanelChevronColor,
        backgroundDrawable = attr.riskLevelBlackPanelBackgroundDrawable
    )
}
