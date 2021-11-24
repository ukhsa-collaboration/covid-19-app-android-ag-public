package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.settings.languages.LanguagesViewAdapter.LanguagesViewHolder
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction
import uk.nhs.nhsx.covid19.android.app.testhelpers.allRecyclerViewItemsMatch
import uk.nhs.nhsx.covid19.android.app.testhelpers.compose
import uk.nhs.nhsx.covid19.android.app.testhelpers.recyclerViewItemDiscriminatorMatcher
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class LanguagesRobot : HasActivity {

    override val containerId: Int
        get() = R.id.languagesContainer

    fun checkSystemLanguageNativeNameMatches(languageNativeName: String) {
        onView(
            allOf(
                withId(R.id.languageNativeName),
                isDescendantOfA(withId(R.id.systemLanguage))
            )
        )
            .check(matches(withText(languageNativeName)))
    }

    fun checkSystemLanguageTranslatedNameMatches(@StringRes languageTranslatedName: Int) {
        onView(
            allOf(
                withId(R.id.languageTranslatedName),
                isDescendantOfA(withId(R.id.systemLanguage))
            )
        )
            .check(matches(withText(languageTranslatedName)))
    }

    fun checkSystemLanguageIsChecked() {
        onView(
            allOf(
                withId(R.id.languageRadio),
                isDescendantOfA(withId(R.id.systemLanguage))
            )
        )
            .check(matches(isChecked()))
    }

    fun checkSystemLanguageIsNotChecked() {
        onView(
            allOf(
                withId(R.id.languageRadio),
                isDescendantOfA(withId(R.id.systemLanguage))
            )
        )
            .check(matches(not(isChecked())))
    }

    fun checkOtherLanguageIsChecked(@StringRes languageName: Int) {
        onView(withId(R.id.languagesRecyclerView))
            .check(
                matches(
                    recyclerViewItemDiscriminatorMatcher(
                        hasDescendant(withText(languageName)),
                        hasDescendant(
                            allOf(
                                withId(R.id.languageRadio),
                                isChecked()
                            )
                        ),
                        hasDescendant(
                            allOf(
                                withId(R.id.languageRadio),
                                not(isChecked())
                            )
                        )
                    )
                )
            )
    }

    fun checkNoOtherLanguageIsChecked() {
        onView(withId(R.id.languagesRecyclerView))
            .check(
                matches(
                    allRecyclerViewItemsMatch(
                        hasDescendant(
                            allOf(
                                withId(R.id.languageRadio),
                                not(isChecked())
                            )
                        )
                    )
                )
            )
    }

    fun selectLanguage(@StringRes languageName: Int) {
        onView(withId(R.id.languagesRecyclerView))
            .perform(
                actionOnItem<LanguagesViewHolder>(
                    hasDescendant(withText(languageName)),
                    compose(
                        NestedScrollViewScrollToAction(),
                        click()
                    )
                )
            )
    }

    fun selectSystemLanguage() {
        onView(
            allOf(
                withId(R.id.languageRadio),
                isDescendantOfA(withId(R.id.systemLanguage))
            )
        ).perform(click())
    }

    fun checkConfirmationDialogIsDisplayed(languageName: String) {
        onView(withText(context.getString(R.string.change_language_dialog, languageName)))
            .check(matches(isDisplayed()))
    }

    fun clickConfirmPositive() {
        clickDialogPositiveButton()
    }

    fun clickConfirmNegative() {
        clickDialogNegativeButton()
    }
}
