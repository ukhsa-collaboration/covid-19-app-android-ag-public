package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.setChecked

class ExposureNotificationVaccinationStatusRobot {

    private val dosesYesButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption1),
            isDescendantOfA(withId(R.id.allDosesBinaryRadioGroup))
        )
    )

    private val dosesNoButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption2),
            isDescendantOfA(withId(R.id.allDosesBinaryRadioGroup))
        )
    )

    private val dateYesButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption1),
            isDescendantOfA(withId(R.id.vaccineDateBinaryRadioGroup))
        )
    )

    private val dateNoButton = onView(
        allOf(
            withId(R.id.binaryRadioButtonOption2),
            isDescendantOfA(withId(R.id.vaccineDateBinaryRadioGroup))
        )
    )

    fun checkActivityIsDisplayed() {
        onView(withText(R.string.exposure_notification_vaccination_status_title))
            .check(matches(isDisplayed()))
    }

    fun checkErrorVisible(visible: Boolean) {
        onView(withId(R.id.vaccinationStatusErrorView))
            .check(matches(if (visible) isDisplayed() else not(isDisplayed())))
    }

    fun clickDosesYesButton() {
        dosesYesButton.perform(NestedScrollViewScrollToAction(), setChecked(true))
    }

    fun clickDosesNoButton() {
        dosesNoButton.perform(NestedScrollViewScrollToAction(), setChecked(true))
    }

    fun checkDosesNothingSelected() {
        dosesYesButton.check(matches(isNotChecked()))
        dosesNoButton.check(matches(isNotChecked()))
    }

    fun checkDosesYesSelected() {
        dosesYesButton
            .perform(NestedScrollViewScrollToAction())
            .check(matches(isChecked()))
        dosesNoButton
            .perform(NestedScrollViewScrollToAction())
            .check(matches(isNotChecked()))
    }

    fun checkDosesNoSelected() {
        dosesNoButton
            .perform(NestedScrollViewScrollToAction())
            .check(matches(isChecked()))
        dosesYesButton
            .perform(NestedScrollViewScrollToAction())
            .check(matches(isNotChecked()))
    }

    fun clickDateYesButton() {
        dateYesButton.perform(NestedScrollViewScrollToAction(), click())
    }

    fun clickDateNoButton() {
        dateNoButton.perform(NestedScrollViewScrollToAction(), click())
    }

    fun checkDateNothingSelected() {
        dateYesButton
            .perform(NestedScrollViewScrollToAction())
            .check(matches(isNotChecked()))
        dateNoButton
            .perform(NestedScrollViewScrollToAction())
            .check(matches(isNotChecked()))
    }

    fun clickContinueButton() {
        onView(withId(R.id.vaccinationStatusContinueButton))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun checkDosesDateQuestionContainerDisplayed(displayed: Boolean) {
        onView(withId(R.id.vaccineDateQuestionContainer)).apply {
            if (displayed) {
                perform(NestedScrollViewScrollToAction())
            }
        }
            .check(matches(if (displayed) isDisplayed() else not(isDisplayed())))
    }

    fun checkDosesDateQuestionDisplayedWithDate(expectedDate: String) {
        onView(withId(R.id.vaccineDateQuestion))
            .perform(NestedScrollViewScrollToAction())
            .check(matches(withText(
                context.getString(R.string.exposure_notification_vaccination_status_date_question, expectedDate)
            )))
    }

    fun clickApprovedVaccinesLink() {
        onView(withId(R.id.approvedVaccinesLink))
            .perform(NestedScrollViewScrollToAction(), click())
    }
}
