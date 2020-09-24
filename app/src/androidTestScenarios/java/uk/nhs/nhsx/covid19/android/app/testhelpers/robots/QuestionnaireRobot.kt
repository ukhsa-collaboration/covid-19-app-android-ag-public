package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.adapter.QuestionnaireAdapter.QuestionnaireViewHolder

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
        clickOn(R.id.buttonReviewSymptoms)
    }

    fun confirmErrorScreenIsDisplayed() {
        assertDisplayed(R.id.errorPanel)
    }

    fun selectNoSymptoms() {
        clickOn(R.id.textNoSymptoms)
    }

    fun discardSymptomsDialogIsDisplayed() {
        onView(withText(R.string.questionnaire_discard_symptoms_dialog_title))
            .check(matches(isDisplayed()))
        onView(withText(R.string.questionnaire_discard_symptoms_dialog_message))
            .check(matches(isDisplayed()))
    }

    fun continueOnDiscardSymptomsDialog() {
        onView(withText(R.string.remove))
            .perform(click())
    }

    fun cancelOnDiscardSymptomsDialog() {
        onView(withText(R.string.cancel))
            .perform(click())
    }
}
