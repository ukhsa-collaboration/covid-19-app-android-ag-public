package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.withDrawable

class RiskLevelRobot {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.riskLevelContainer))
            .check(matches(isDisplayed()))
    }

    fun checkTitleForLowRiskDisplayed(postCode: String) {
        onView(withId(R.id.titleRiskLevel))
            .check(
                matches(
                    withText(
                        context.getString(
                            R.string.status_area_risk_level,
                            postCode,
                            context.getString(R.string.status_area_risk_level_low)
                        )
                    )
                )
            )
    }

    fun checkTitleForMediumRiskDisplayed(postCode: String) {
        onView(withId(R.id.titleRiskLevel))
            .check(
                matches(
                    withText(
                        context.getString(
                            R.string.status_area_risk_level,
                            postCode,
                            context.getString(R.string.status_area_risk_level_medium)
                        )
                    )
                )
            )
    }

    fun checkTitleForHighRiskDisplayed(postCode: String) {
        onView(withId(R.id.titleRiskLevel))
            .check(
                matches(
                    withText(
                        context.getString(
                            R.string.status_area_risk_level,
                            postCode,
                            context.getString(R.string.status_area_risk_level_high)
                        )
                    )
                )
            )
    }

    fun checkTextForLowRiskDisplayed() {
        onView(withText(R.string.low_risk_level_text))
            .check(matches(isDisplayed()))
    }

    fun checkTextForMediumRiskDisplayed() {
        onView(withText(R.string.medium_risk_level_text))
            .check(matches(isDisplayed()))
    }

    fun checkTextForHighRiskDisplayed() {
        onView(withText(R.string.high_risk_level_text))
            .check(matches(isDisplayed()))
    }

    fun checkImageForLowRiskDisplayed() {
        onView(withId(R.id.imageRiskLevel)).check(
            matches(withDrawable(R.drawable.ic_low_risk_map))
        )
    }

    fun checkImageForMediumRiskDisplayed() {
        onView(withId(R.id.imageRiskLevel)).check(
            matches(withDrawable(R.drawable.ic_medium_risk_map))
        )
    }

    fun checkImageForHighRiskDisplayed() {
        onView(withId(R.id.imageRiskLevel)).check(
            matches(withDrawable(R.drawable.ic_high_risk_map))
        )
    }
}
