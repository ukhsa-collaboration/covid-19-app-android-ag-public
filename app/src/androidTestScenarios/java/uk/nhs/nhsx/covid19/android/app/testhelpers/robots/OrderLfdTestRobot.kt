package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id

class OrderLfdTestRobot {

    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.book_free_lfd_test_title)
    }

    fun clickOrderTestButton() {
        onView(ViewMatchers.withId(id.orderTestButton))
            .perform(scrollTo(), click())
    }

    fun clickIAlreadyHaveKitButton() {
        onView(ViewMatchers.withId(id.alreadyHaveTestKitButton))
            .perform(scrollTo(), click())
    }
}
