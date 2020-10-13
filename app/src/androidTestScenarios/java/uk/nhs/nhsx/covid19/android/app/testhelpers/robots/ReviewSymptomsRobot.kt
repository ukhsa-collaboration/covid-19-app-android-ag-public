package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.schibsted.spain.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.schibsted.spain.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.adapter.ReviewSymptomViewHolder

class ReviewSymptomsRobot {

    fun confirmReviewSymptomsScreenIsDisplayed() {
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
            .check(matches(hasDescendant(withText(R.string.questionnaire_review_symptoms))))
    }

    fun selectCannotRememberDate() {
        clickOn(R.id.checkboxNoDate)
    }

    fun clickSelectDate() {
        clickOn(R.id.selectDateContainer)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    fun selectDayOfMonth(dayOfMonth: Int) {
        onView(
            allOf(
                isDescendantOfA(withTagValue(equalTo("MONTHS_VIEW_GROUP_TAG"))),
                isCompletelyDisplayed(),
                withText(dayOfMonth.toString())
            )
        )
            .perform(click())
        onView(withTagValue(Matchers.`is`("CONFIRM_BUTTON_TAG"))).perform(click())
    }

    fun confirmSelection() {
        clickOn(R.id.buttonConfirmSymptoms)
    }

    fun changeFirstNegativeSymptom() {
        onView(withId(R.id.listReviewSymptoms))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<ReviewSymptomViewHolder>(
                    1,
                    clickOnViewChild(R.id.textChange)
                )
            )
    }

    fun checkReviewSymptomsErrorIsDisplayed() {
        onView(withText(R.string.questionnaire_input_date_error))
            .check(matches(isDisplayed()))
    }

    fun checkDoNotRememberDateIsChecked() {
        assertChecked(R.id.checkboxNoDate)
    }

    fun checkDoNotRememberDateIsNotChecked() {
        assertUnchecked(R.id.checkboxNoDate)
    }

    fun clickOnViewChild(viewId: Int) = object : ViewAction {
        override fun getConstraints() = null

        override fun getDescription() = "Click on a child view with specified id."

        override fun perform(uiController: UiController, view: View) =
            click().perform(uiController, view.findViewById(viewId))
    }
}
