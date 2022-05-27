package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class CheckYourAnswersRobot : HasActivity {
    override val containerId: Int
        get() = id.checkYourAnswersScrollViewContainer

    private val nonCardinalAnswer = onView(
        Matchers.allOf(
            withId(id.textSymptomMessage),
            ViewMatchers.isDescendantOfA(withId(id.checkYourAnswersNonCardinalAnswer))
        )
    )

    private val cardinalAnswer = onView(
        Matchers.allOf(
            withId(id.textSymptomMessage),
            ViewMatchers.isDescendantOfA(withId(id.checkYourAnswersCardinalAnswer))
        )
    )

    private val howYouFeelAnswer = onView(
        Matchers.allOf(
            withId(id.textSymptomMessage),
            ViewMatchers.isDescendantOfA(withId(id.checkYourAnswersHowDoYouFeelAnswer))
        )
    )

    private val yourSymptomsChange = onView(
        Matchers.allOf(
            withId(id.textChange),
            ViewMatchers.isDescendantOfA(withId(id.checkYourAnswersYourSymptoms))
        )
    )

    private val howYouFeelChange = onView(
        Matchers.allOf(
            withId(id.textChange),
            ViewMatchers.isDescendantOfA(withId(id.checkYourAnswersHowDoYouFeel))
        )
    )

    fun checkNonCardinalSymptomsHasYesAnswer() {
        nonCardinalAnswer.check(ViewAssertions.matches(withText(R.string.check_answers_yes_answer)))
    }

    fun checkNonCardinalSymptomsHasNoAnswer() {
        nonCardinalAnswer.check(ViewAssertions.matches(withText(R.string.check_answers_no_answer)))
    }

    fun checkCardinalSymptomsHasYesAnswer() {
        cardinalAnswer.check(ViewAssertions.matches(withText(R.string.check_answers_yes_answer)))
    }

    fun checkCardinalSymptomsHasNoAnswer() {
        cardinalAnswer.check(ViewAssertions.matches(withText(R.string.check_answers_no_answer)))
    }

    fun checkHowYouFeelHasYesAnswer() {
        howYouFeelAnswer.check(ViewAssertions.matches(withText(R.string.check_answers_yes_answer)))
    }

    fun checkHowYouFeelHasNoAnswer() {
        howYouFeelAnswer.check(ViewAssertions.matches(withText(R.string.check_answers_no_answer)))
    }

    fun clickChangeYourSymptoms() {
        yourSymptomsChange.perform(scrollTo(), click())
    }

    fun clickChangeHowYouFeel() {
        howYouFeelChange.perform(scrollTo(), click())
    }

    fun clickSubmitAnswers() {
        onView(withId(id.checkYourAnswersContinueButton))
            .perform(scrollTo(), click())
    }
}
