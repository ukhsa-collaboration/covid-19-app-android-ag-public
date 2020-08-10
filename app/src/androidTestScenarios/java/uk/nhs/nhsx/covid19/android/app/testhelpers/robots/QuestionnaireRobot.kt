package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.adapter.QuestionnaireAdapter.QuestionnaireViewHolder
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction

class QuestionnaireRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.questionnaireMainContainer))
            .check(matches(isDisplayed()))
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
            .perform(NestedScrollViewScrollToAction())
            .check(matches(isDisplayed()))
    }

    fun selectNoSymptoms() {
        onView(withId(R.id.textNoSymptoms))
            .perform(NestedScrollViewScrollToAction(), click())
    }
}
