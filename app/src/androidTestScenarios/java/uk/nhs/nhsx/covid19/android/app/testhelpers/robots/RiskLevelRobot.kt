package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.TextViewDrawableMatcher
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.isAnnouncedAsOpenInBrowser
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
        onView(withText(text))
            .check(matches(isDisplayed()))
    }

    fun checkContentFromLocalAuthorityIsDisplayed(text: String) {
        onView(withText(text))
            .check(matches(isDisplayed()))
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

    fun checkImageForTierFourRiskDisplayed() {
        onView(withId(R.id.imageRiskLevel)).check(
            matches(withDrawable(R.drawable.ic_map_risk_maroon))
        )
    }

    fun checkImageForTierFiveRiskDisplayed() {
        onView(withId(R.id.imageRiskLevel)).check(
            matches(withDrawable(R.drawable.ic_map_risk_black))
        )
    }

    fun checkForFooter() {
        onView(withId(R.id.riskLevelFooter))
            .perform(scrollTo()).check(matches(isDisplayed()))
    }

    fun checkExternalUrlSectionIsDisplayed() {
        onView(withId(R.id.externalLinkListContainer))
            .perform(scrollTo()).check(matches(isDisplayed()))
    }

    fun checkExternalUrlSectionIsHidden() {
        onView(withId(R.id.buttonRiskLevelLink))
            .perform(scrollTo())

        onView(withId(R.id.externalLinkListContainer))
            .check(matches(not(isDisplayed())))
    }

    fun checkUrlIsDisplayed(urlTitle: String) {
        onView(withText(urlTitle))
            .perform(scrollTo()).check(matches(isDisplayed()))
    }

    fun checkExternalUrlHeaderIsDisplayed(headerText: String) {
        onView(withText(headerText))
            .perform(scrollTo()).check(matches(isDisplayed()))
    }

    fun checkExternalUrlSectionHasCorrectNumberOfChildElements(size: Int) {
        onView(withId(R.id.externalLinkListContainer))
            .check(matches(hasChildCount(size)))
    }

    fun clickExternalUrlSectionLink_opensInExternalBrowser(urlTitle: String) {
        onView(withText(urlTitle)).check(
            matches(Matchers.allOf(TextViewDrawableMatcher.withTextViewHasDrawableEnd(), isAnnouncedAsOpenInBrowser()))
        )
        onView(withText(urlTitle))
            .perform(scrollTo())
            .perform(ViewActions.click())
    }
}
