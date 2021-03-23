package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import uk.nhs.nhsx.covid19.android.app.R

class MyAreaRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.myAreaContainer))
            .check(matches(isDisplayed()))
    }

    fun clickEditButton() {
        onView(withId(R.id.menuEditAction))
            .perform(click())
    }

    fun checkPostCodeIsEmpty() {
        checkPostCodeEquals("")
    }

    fun checkLocalAuthorityIsEmpty() {
        checkPostCodeEquals("")
    }

    fun checkPostCodeEquals(postCode: String) {
        onView(
            Matchers.allOf(
                withId(R.id.settingsItemSubtitle),
                ViewMatchers.isDescendantOfA(withId(R.id.postCodeDistrictOption))
            )
        )
            .check(matches(withText(postCode)))
    }

    fun checkLocalAuthorityEquals(localAuthority: String) {
        onView(
            Matchers.allOf(
                withId(R.id.settingsItemSubtitle),
                ViewMatchers.isDescendantOfA(withId(R.id.localAuthorityOption))
            )
        )
            .check(matches(withText(localAuthority)))
    }
}
