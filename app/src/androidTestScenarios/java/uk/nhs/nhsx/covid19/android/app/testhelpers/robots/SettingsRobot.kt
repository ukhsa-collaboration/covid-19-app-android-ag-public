package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.allOf
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.positional.isNthChildOf
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class SettingsRobot : HasActivity {

    override val containerId: Int
        get() = R.id.settingsContainer

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

    fun hasLanguageSetting() {
        onView(withId(R.id.languageOption))
            .check(
                matches(
                    allOf(
                        isDisplayed(),
                        isNthChildOf(withId(id.settingsList), 0)
                    )
                )
            )
    }

    fun hasMyAreaSetting() {
        onView(withId(R.id.myAreaOption))
            .check(
                matches(
                    allOf(
                        isDisplayed(),
                        isNthChildOf(withId(id.settingsList), 1)
                    )
                )
            )
    }

    fun hasMyDataSetting() {
        onView(withId(R.id.myDataOption))
            .check(
                matches(
                    allOf(
                        isDisplayed(),
                        isNthChildOf(withId(id.settingsList), 2)
                    )
                )
            )
    }

    fun hasVenueHistorySetting() {
        onView(withId(R.id.venueHistoryOption))
            .check(
                matches(
                    allOf(
                        isDisplayed(),
                        isNthChildOf(withId(id.settingsList), 3)
                    )
                )
            )
    }

    fun venueHistorySettingIsHidden() {
        onView(withId(R.id.venueHistoryOption))
            .check(matches(not(isDisplayed())))
    }

    fun hasAnimationsSetting() {
        onView(withId(R.id.animationsOption))
            .check(
                matches(
                    allOf(
                        isDisplayed(),
                        isNthChildOf(withId(id.settingsList), 4)
                    )
                )
            )
    }

    fun hasDeleteDataOption() {
        onView(withId(R.id.actionDeleteAllData))
            .check(
                matches(
                    allOf(
                        isDisplayed(),
                        withText(R.string.settings_delete_data)
                    )
                )
            )
    }

    fun clickMyDataSetting() {
        onView(withId(R.id.myDataOption))
            .perform(click())
    }

    fun clickMyAreaSetting() {
        onView(withId(R.id.myAreaOption))
            .perform(click())
    }

    fun clickVenueHistorySetting() {
        onView(withId(R.id.venueHistoryOption))
            .perform(click())
    }

    fun clickAnimationSetting() {
        onView(withId(R.id.animationsOption))
            .perform(click())
    }

    fun clickDeleteSetting() {
        onView(withId(R.id.actionDeleteAllData))
            .perform(click())
    }

    fun userClicksDeleteDataOnDialog() {
        onView(withText(R.string.about_delete_positive_text))
            .perform(click())
    }
}
