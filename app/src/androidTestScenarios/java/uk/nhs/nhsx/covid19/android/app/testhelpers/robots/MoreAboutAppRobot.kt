package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.R

class MoreAboutAppRobot {
    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.about_this_app_title)
    }

    fun checkAppReleaseDateIsCorrect() {
        onView(withId(R.id.softwareInformationContainer))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withId(R.id.textSoftwareDateOfRelease))
            .check(matches(withText(BuildConfig.RELEASE_DATE)))
    }
}
