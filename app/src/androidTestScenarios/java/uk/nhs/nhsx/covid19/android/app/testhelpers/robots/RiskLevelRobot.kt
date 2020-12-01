package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.testhelpers.withDrawable

class RiskLevelRobot {
    private val riskToContent = mapOf(
        ColorScheme.GREEN to "Content low",
        ColorScheme.YELLOW to "Content medium",
        ColorScheme.RED to "Content high"
    )

    private val riskLocalAuthorityToContent = mapOf(
        ColorScheme.RED to "Local Authority content high"
    )

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.riskLevelContainer))
            .check(matches(isDisplayed()))
    }

    fun checkTitleIsDisplayed(title: String) {
        onView(withId(R.id.titleRiskLevel))
            .check(matches(withText(title)))
    }

    fun checkContentFromPostDistrictIsDisplayed(text: String) {
        onView(withText(text)).check(matches(isDisplayed()))
    }

    fun checkContentFromLocalAuthorityIsDisplayed(text: String) {
        onView(withText(text)).check(matches(isDisplayed()))
    }

    fun checkImageForLowRiskDisplayed() {
        onView(withId(R.id.imageRiskLevel)).check(
            matches(withDrawable(R.drawable.ic_map_risk_green))
        )
    }

    fun checkImageForMediumRiskDisplayed() {
        onView(withId(R.id.imageRiskLevel)).check(
            matches(withDrawable(R.drawable.ic_map_risk_yellow))
        )
    }

    fun checkImageForHighRiskDisplayed() {
        onView(withId(R.id.imageRiskLevel)).check(
            matches(withDrawable(R.drawable.ic_map_risk_red))
        )
    }

    fun checkForFooter() {
        onView(withId(R.id.riskLevelFooter))
            .perform(ViewActions.scrollTo()).check(matches(isDisplayed()))
    }
}
