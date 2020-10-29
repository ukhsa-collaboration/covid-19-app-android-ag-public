package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R

class AppAvailabilityRobot {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    fun checkActivityDisplaysCantRunApp() {
        onView(withText(context.getString(R.string.cant_run_app)))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysUpdateOS() {
        onView(withText(context.getString(R.string.update_os)))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysUpdateApp() {
        onView(withText(context.getString(R.string.update_app_title)))
            .check(matches(isDisplayed()))
    }

    fun checkActivityDisplaysMessage(message: String) {
        onView(withText(message))
            .check(matches(isDisplayed()))
    }

    fun checkActivityGoToPlayStoreDisplayed() {
        onView(withId(R.id.goToPlayStore)).check(matches(isDisplayed()))
    }

    fun checkActivityGoToPlayStoreNotDisplayed() {
        onView(withId(R.id.goToPlayStore)).check(matches(not(isDisplayed())))
    }
}
