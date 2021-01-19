package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import uk.nhs.nhsx.covid19.android.app.R

class SettingsRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.settingsContainer))
            .check(matches(isDisplayed()))
    }

    fun checkLanguageSubtitleMatches(@StringRes languageName: Int) {
        onView(
            allOf(
                withId(R.id.settingsItemSubtitle),
                isDescendantOfA(withId(R.id.languageOption))
            )
        )
            .check(matches(withText(languageName)))
    }

    fun clickLanguageSetting() {
        onView(withId(R.id.languageOption))
            .perform(click())
    }
}
