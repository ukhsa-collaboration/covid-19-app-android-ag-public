package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.setChecked

class YourSymptomsRobot {

    private val nonCardinalYesButton = onView(
        Matchers.allOf(
            withId(id.binaryRadioButtonOption1),
            ViewMatchers.isDescendantOfA(withId(id.nonCardinalBinaryRadioGroup))
        )
    )

    private val nonCardinalNoButton = onView(
        Matchers.allOf(
            withId(id.binaryRadioButtonOption2),
            ViewMatchers.isDescendantOfA(withId(id.nonCardinalBinaryRadioGroup))
        )
    )

    private val cardinalYesButton = onView(
        Matchers.allOf(
            withId(id.binaryRadioButtonOption1),
            ViewMatchers.isDescendantOfA(withId(id.cardinalBinaryRadioGroup))
        )
    )

    private val cardinalNoButton = onView(
        Matchers.allOf(
            withId(id.binaryRadioButtonOption2),
            ViewMatchers.isDescendantOfA(withId(id.cardinalBinaryRadioGroup))
        )
    )
    fun checkActivityIsDisplayed() {
        onView(withId(id.yourSymptomsContainer))
            .check(matches(isDisplayed()))
    }

    fun checkLoadingSpinnerIsDisplayed() {
        onView(withId(id.yourSymptomsLoadingContainer))
            .check(matches(isDisplayed()))
    }

    fun checkErrorStateIsDisplayed() {
        onView(withId(id.yourSymptomsErrorStateContainer))
            .check(matches(isDisplayed()))
    }

    fun checkYourSymptomsIsDisplayed() {
        onView(withId(id.yourSymptomsScrollViewContainer))
            .check(matches(isDisplayed()))
    }

    fun clickTryAgainButton() {
        onView(withId(id.buttonTryAgain))
            .perform(ViewActions.click())
    }

    fun checkErrorVisible(shouldBeVisible: Boolean) {
        onView(withId(id.yourSymptomsErrorView)).apply {
            if (shouldBeVisible) perform(nestedScrollTo())
                .check(matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
        }
    }

    fun checkNothingSelected() {
        onView(withId(id.nonCardinalBinaryRadioGroup))
            .check(matches(not(isChecked())))
        onView(withId(id.cardinalBinaryRadioGroup))
            .check(matches(not(isChecked())))
    }

    fun clickContinueButton() {
        onView(withId(id.yourSymptomsContinueButton))
            .perform(ViewActions.scrollTo(), ViewActions.click())
    }

    fun clickNonCardinalYesButton() {
        nonCardinalYesButton.perform(nestedScrollTo(), setChecked(true))
    }

    fun clickNonCardinalNoButton() {
        nonCardinalNoButton.perform(nestedScrollTo(), setChecked(true))
    }

    fun clickCardinalYesButton() {
        cardinalYesButton.perform(nestedScrollTo(), setChecked(true))
    }

    fun clickCardinalNoButton() {
        cardinalNoButton.perform(nestedScrollTo(), setChecked(true))
    }

    fun checkNonCardinalYesButtonIsSelected() {
        nonCardinalYesButton.check(matches(isChecked()))
    }

    fun checkNonCardinalNoButtonIsSelected() {
        nonCardinalNoButton.check(matches(isChecked()))
    }

    fun checkCardinalYesButtonIsSelected() {
        cardinalYesButton.check(matches(isChecked()))
    }

    fun checkCardinalNoButtonIsSelected() {
        cardinalNoButton.check(matches(isChecked()))
    }
}
