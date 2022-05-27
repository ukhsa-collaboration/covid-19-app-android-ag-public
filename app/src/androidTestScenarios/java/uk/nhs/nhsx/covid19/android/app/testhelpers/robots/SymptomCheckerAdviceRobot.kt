package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.withDrawable

class SymptomCheckerAdviceRobot {
    fun checkActivityIsDisplayed() {
        onView(withId(R.id.symptomsCheckerAdviceContainer))
            .check(matches(isDisplayed()))
    }

    fun clickNoticeLink() {
        onView(withId(R.id.symptomsCheckerAdviceNoticeLink))
            .perform(
                scrollTo(),
                click()
            )
    }

    fun clickMedicalLink() {
        onView(withId(R.id.symptomsCheckerAdviceLink))
            .perform(
                scrollTo(),
                click()
            )
    }

    fun clickBackToHomeButton() {
        onView(withId(R.id.symptomsCheckerAdviceFinishButton))
            .perform(
                scrollTo(),
                click()
            )
    }

    fun checkContinueNormalActivitiesIsDisplayed() {
        onView(withId(R.id.symptomsCheckerAdviceTitle))
            .perform(scrollTo())
            .check(matches(withText(R.string.symptom_checker_advice_continue_normal_activities_header)))
        onView(withId(R.id.symptomsCheckerAdviceNoticeLink))
            .perform(scrollTo())
            .check(
                matches(
                    allOf(
                        withText(R.string.symptom_checker_advice_notice_continue_normal_activities_link_text),
                        isDisplayed()
                    )
                )
            )
        onView(withId(R.id.symptomsCheckerAdviceImage))
            .check(matches(withDrawable(R.drawable.ic_onboarding_welcome)))
    }

    fun checkTryToStayAtHomeIsDisplayed() {
        onView(withId(R.id.symptomsCheckerAdviceTitle))
            .check(matches(withText(R.string.symptom_checker_advice_stay_at_home_header)))
        onView(withId(R.id.symptomsCheckerAdviceNoticeLink))
            .check(
                matches(
                    allOf(
                        withText(R.string.symptom_checker_advice_notice_stay_at_home_link_text),
                        isDisplayed()
                    )
                )
            )
        onView(withId(R.id.symptomsCheckerAdviceImage))
            .check(matches(withDrawable(R.drawable.ic_isolation_book_test)))
    }
}
