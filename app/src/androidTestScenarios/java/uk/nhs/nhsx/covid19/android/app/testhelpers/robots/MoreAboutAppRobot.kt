package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class MoreAboutAppRobot : HasActivity {
    override val containerId: Int
        get() = R.id.moreAboutAppContainer

    fun checkAppReleaseDateIsCorrect() {
        onView(withId(R.id.softwareInformationContainer))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.textSoftwareDateOfRelease))
            .check(matches(withText(context.getString(R.string.about_this_app_software_information_date_of_release_description))))
    }
}
