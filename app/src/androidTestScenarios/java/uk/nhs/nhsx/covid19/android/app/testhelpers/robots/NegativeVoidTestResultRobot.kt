package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class NegativeVoidTestResultRobot : HasActivity {
    override val containerId: Int
        get() = id.selfReportNegativeVoidResultContainer

    fun checkEligibilityParagraphAndUrlIsVisible(shouldBeVisible: Boolean) {
        onView(withId(id.englandSelfReportedEligibleText))
            .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))

        onView(withId(id.englandSelfReportedLink))
            .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
    }

    fun clickBackToHome() {
        onView(withId(R.id.buttonReturnToHomeScreen))
            .perform(NestedScrollViewScrollToAction(), ViewActions.click())
    }
}
