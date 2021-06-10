package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.adapter.QuestionnaireViewAdapter.QuestionnaireViewHolder
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction

class QuestionnaireRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.questionnaireMainContainer))
            .check(matches(isDisplayed()))
    }

    fun checkQuestionnaireIsDisplayed() {
        onView(withId(R.id.questionListContainer))
            .check(matches(isDisplayed()))
    }

    fun checkErrorStateIsDisplayed() {
        onView(withId(R.id.errorStateContainer))
            .check(matches(isDisplayed()))
    }

    fun clickTryAgainButton() {
        onView(withId(R.id.buttonTryAgain))
            .perform(click())
    }

    fun selectSymptomsAtPositions(vararg position: Int) {
        position.forEach {
            onView(withId(R.id.questionsRecyclerView))
                .perform(
                    RecyclerViewActions.actionOnItemAtPosition<QuestionnaireViewHolder>(
                        it,
                        scrollTo()
                    ),
                    RecyclerViewActions.actionOnItemAtPosition<QuestionnaireViewHolder>(
                        it,
                        click()
                    )
                )
        }
    }

    fun reviewSymptoms() {
        onView(withId(R.id.buttonReviewSymptoms))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun confirmErrorScreenIsDisplayed() {
        onView(withId(R.id.errorPanel))
            .check(matches(isDisplayed()))
    }

    fun selectNoSymptoms() {
        onView(withId(R.id.textNoSymptoms))
            .perform(NestedScrollViewScrollToAction(), click())
    }

    fun discardSymptomsDialogIsDisplayed() {
        onView(withText(R.string.questionnaire_discard_symptoms_dialog_title))
            .check(matches(isDisplayed()))
        onView(withText(R.string.questionnaire_discard_symptoms_dialog_message))
            .check(matches(isDisplayed()))
    }

    fun continueOnDiscardSymptomsDialog() {
        clickDialogPositiveButton()
    }

    fun cancelOnDiscardSymptomsDialog() {
        clickDialogNegativeButton()
    }
}
