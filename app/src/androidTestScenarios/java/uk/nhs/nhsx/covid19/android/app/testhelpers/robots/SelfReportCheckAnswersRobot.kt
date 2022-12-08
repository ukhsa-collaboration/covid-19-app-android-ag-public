package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import uk.nhs.nhsx.covid19.android.app.R
import org.hamcrest.Matchers.not
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.assertion.ViewAssertions.matches

class SelfReportCheckAnswersRobot : HasActivity {
    override val containerId: Int
        get() = id.selfReportCheckAnswersContainer

    private val testKitTypeAnswer = onView(
        Matchers.allOf(
            withId(id.checkAnswerQuestionAnswer),
            ViewMatchers.isDescendantOfA(withId(id.selfReportCheckAnswersTestKitType))
        )
    )

    private val testOriginAnswer = onView(
        Matchers.allOf(
            withId(id.checkAnswerQuestionAnswer),
            ViewMatchers.isDescendantOfA(withId(id.selfReportCheckAnswersTestOrigin))
        )
    )

    private val testDateAnswer = onView(
        Matchers.allOf(
            withId(id.checkAnswerQuestionAnswer),
            ViewMatchers.isDescendantOfA(withId(id.selfReportCheckAnswersTestDate))
        )
    )

    private val symptomsAnswer = onView(
        Matchers.allOf(
            withId(id.checkAnswerQuestionAnswer),
            ViewMatchers.isDescendantOfA(withId(id.selfReportCheckAnswersSymptoms))
        )
    )

    private val symptomsOnsetAnswer = onView(
        Matchers.allOf(
            withId(id.checkAnswerQuestionAnswer),
            ViewMatchers.isDescendantOfA(withId(id.selfReportCheckAnswersSymptomsOnset))
        )
    )

    private val reportedTestAnswer = onView(
        Matchers.allOf(
            withId(id.checkAnswerQuestionAnswer),
            ViewMatchers.isDescendantOfA(withId(id.selfReportCheckAnswersReportedTest))
        )
    )

    private fun changeButton(answerElementId: Int): ViewInteraction {
        return onView(
            Matchers.allOf(
                withId(id.checkAnswerQuestionChangeButton),
                ViewMatchers.isDescendantOfA(withId(answerElementId))
            )
        )
    }

    fun checkTestKitTypeHasLFDAnswer() {
        testKitTypeAnswer.perform(nestedScrollTo()).check(matches(withText(R.string.self_report_test_kit_type_radio_button_option_lfd)))
    }

    fun checkTestKitTypeHasPCRAnswer() {
        testKitTypeAnswer.perform(nestedScrollTo()).check(matches(withText(R.string.self_report_test_kit_type_radio_button_option_pcr)))
    }

    fun clickTestKitTypeChangeButton() {
        changeButton(id.selfReportCheckAnswersTestKitType).perform(nestedScrollTo(), click())
    }

    fun checkTestOriginIsHidden() {
        onView(withId(id.selfReportCheckAnswersTestKitType)).perform(nestedScrollTo())
        onView(withId(id.selfReportCheckAnswersTestOrigin)).check(matches(not(isDisplayed())))
    }

    fun checkTestOriginHasYesAnswer() {
        testOriginAnswer.perform(nestedScrollTo()).check(matches(withText(R.string.self_report_test_origin_radio_button_option_yes)))
    }

    fun checkTestOriginHasNoAnswer() {
        testOriginAnswer.perform(nestedScrollTo()).check(matches(withText(R.string.self_report_test_origin_radio_button_option_no)))
    }

    fun clickTestOriginChangeButton() {
        changeButton(id.selfReportCheckAnswersTestOrigin).perform(nestedScrollTo(), click())
    }

    fun checkTestDateHasCannotRememberAnswer() {
        testDateAnswer.perform(nestedScrollTo()).check(matches(withText(R.string.self_report_test_date_no_date)))
    }

    fun checkTestDateHasSpecificDateAnswer() {
        testDateAnswer.perform(nestedScrollTo()).check(matches(not(withText(R.string.self_report_test_date_no_date))))
    }

    fun clickTestDateChangeButton() {
        changeButton(id.selfReportCheckAnswersTestDate).perform(nestedScrollTo(), click())
    }

    fun checkSymptomsIsHidden() {
        onView(withId(id.selfReportCheckAnswersTestDate)).perform(nestedScrollTo())
        onView(withId(id.selfReportCheckAnswersSymptoms)).check(matches(not(isDisplayed())))
    }

    fun checkSymptomsHasYesAnswer() {
        symptomsAnswer.perform(nestedScrollTo()).check(matches(withText(R.string.self_report_symptoms_radio_button_option_yes)))
    }

    fun checkSymptomsHasNoAnswer() {
        symptomsAnswer.perform(nestedScrollTo()).check(matches(withText(R.string.self_report_symptoms_radio_button_option_no)))
    }

    fun clickSymptomsChangeButton() {
        changeButton(id.selfReportCheckAnswersSymptoms).perform(nestedScrollTo(), click())
    }

    fun checkSymptomsOnsetIsHidden() {
        onView(withId(id.selfReportCheckAnswersContinueButton)).perform(nestedScrollTo())
        onView(withId(id.selfReportCheckAnswersSymptomsOnset)).check(matches(not(isDisplayed())))
    }

    fun checkSymptomsOnsetHasCannotRememberAnswer() {
        symptomsOnsetAnswer.perform(nestedScrollTo()).check(matches(withText(R.string.self_report_symptoms_date_no_date)))
    }

    fun checkSymptomsOnsetHasSpecificDateAnswer() {
        symptomsOnsetAnswer.perform(nestedScrollTo()).check(matches(not(withText(R.string.self_report_symptoms_date_no_date))))
    }

    fun clickSymptomsOnsetChangeButton() {
        changeButton(id.selfReportCheckAnswersSymptomsOnset).perform(nestedScrollTo(), click())
    }

    fun checkReportedTestIsHidden() {
        onView(withId(id.selfReportCheckAnswersContinueButton)).perform(nestedScrollTo())
        onView(withId(id.selfReportCheckAnswersReportedTest)).check(matches(not(isDisplayed())))
    }

    fun checkReportedTestHasYesAnswer() {
        reportedTestAnswer.perform(nestedScrollTo()).check(matches(withText(R.string.self_report_reported_test_radio_button_option_yes)))
    }

    fun checkReportedTestHasNoAnswer() {
        reportedTestAnswer.perform(nestedScrollTo()).check(matches(withText(R.string.self_report_reported_test_radio_button_option_no)))
    }

    fun clickReportedTestChangeButton() {
        changeButton(id.selfReportCheckAnswersReportedTest).perform(nestedScrollTo(), click())
    }

    fun clickContinueButton() {
        onView(withId(id.selfReportCheckAnswersContinueButton))
            .perform(nestedScrollTo(), click())
    }
}
