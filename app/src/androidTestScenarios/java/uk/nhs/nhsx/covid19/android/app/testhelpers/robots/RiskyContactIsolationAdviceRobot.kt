package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.IconTextViewMatcher.Companion.withIconAndText
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.withStateColor
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.withStateStringResource

class RiskyContactIsolationAdviceRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.riskyContactIsolationAdviceContainer))
            .check(matches(isDisplayed()))
    }

    fun checkIsInNotIsolatingAsMinorViewState() {
        onView(withId(R.id.riskyContactIsolationAdviceTitle))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_minors_no_self_isolation_required)))

        onView(withId(R.id.riskyContactIsolationAdviceRemainingDaysInIsolation))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.riskyContactIsolationAdviceStateInfoView))
            .perform(scrollTo())
            .apply {
                check(matches(withStateStringResource(R.string.risky_contact_isolation_advice_minors_information)))
                check(matches(withStateColor(R.color.amber)))
            }
        checkAdvice(
            stringResId = R.string.risky_contact_isolation_advice_minors_testing_advice,
            drawableRes = R.drawable.ic_social_distancing
        )
        checkAdvice(
            stringResId = R.string.risky_contact_isolation_advice_minors_show_to_adult_advice,
            drawableRes = R.drawable.ic_family
        )
        onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_faq_button_title)))
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_book_pcr_test)))
        onView(withId(R.id.secondaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
    }

    fun checkIsInNotIsolatingAsFullyVaccinatedViewState() {
        onView(withId(R.id.riskyContactIsolationAdviceTitle))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_already_vaccinated_no_self_isolation_required)))

        onView(withId(R.id.riskyContactIsolationAdviceRemainingDaysInIsolation))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.riskyContactIsolationAdviceStateInfoView))
            .perform(scrollTo())
            .apply {
                check(matches(withStateStringResource(R.string.risky_contact_isolation_advice_already_vaccinated_information)))
                check(matches(withStateColor(R.color.amber)))
            }
        checkAdvice(
            stringResId = R.string.risky_contact_isolation_advice_already_vaccinated_vaccine_research,
            drawableRes = R.drawable.ic_info
        )
        checkAdvice(
            stringResId = R.string.risky_contact_isolation_advice_already_vaccinated_testing_advice,
            drawableRes = R.drawable.ic_social_distancing
        )
        onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_faq_button_title)))
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_book_pcr_test)))
        onView(withId(R.id.secondaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
    }

    fun checkIsInNotIsolatingAsMedicallyExemptViewState() {
        onView(withId(R.id.riskyContactIsolationAdviceTitle))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_medically_exempt_heading)))

        onView(withId(R.id.riskyContactIsolationAdviceRemainingDaysInIsolation))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.riskyContactIsolationAdviceStateInfoView))
            .perform(scrollTo())
            .apply {
                check(matches(withStateStringResource(R.string.risky_contact_isolation_advice_medically_exempt_information)))
                check(matches(withStateColor(R.color.amber)))
            }
        checkAdvice(
            stringResId = R.string.risky_contact_isolation_advice_medically_exempt_research,
            drawableRes = R.drawable.ic_info
        )
        checkAdvice(
            stringResId = R.string.risky_contact_isolation_advice_medically_exempt_advice,
            drawableRes = R.drawable.ic_social_distancing
        )
        onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_faq_button_title)))
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_book_pcr_test)))
        onView(withId(R.id.secondaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
    }

    fun checkIsInNewlyIsolatingViewState(remainingDaysInIsolation: Int) {
        onView(withId(R.id.riskyContactIsolationAdviceTitle))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_self_isolate_for)))
        val expectedDaysInIsolationText = context.resources.getQuantityString(
            R.plurals.state_isolation_days,
            remainingDaysInIsolation,
            remainingDaysInIsolation
        )
        onView(withId(R.id.riskyContactIsolationAdviceRemainingDaysInIsolation))
            .perform(scrollTo())
            .check(matches(withText(expectedDaysInIsolationText)))
        onView(withId(R.id.riskyContactIsolationAdviceStateInfoView))
            .perform(scrollTo())
            .apply {
                check(matches(withStateStringResource(R.string.risky_contact_isolation_advice_new_isolation_information)))
                check(matches(withStateColor(R.color.amber)))
            }
        checkAdvice(
            stringResId = R.string.risky_contact_isolation_advice_new_isolation_testing_advice,
            drawableRes = R.drawable.ic_get_free_test
        )
        checkAdvice(
            stringResId = R.string.risky_contact_isolation_advice_new_isolation_stay_at_home_advice,
            drawableRes = R.drawable.ic_stay_at_home
        )
        onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_book_pcr_test)))
        onView(withId(R.id.secondaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
    }

    fun checkIsInAlreadyIsolatingViewState(remainingDaysInIsolation: Int) {
        onView(withId(R.id.riskyContactIsolationAdviceTitle))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_continue_isolataion_for)))

        val expectedDaysInIsolationText = context.resources.getQuantityString(
            R.plurals.state_isolation_days,
            remainingDaysInIsolation,
            remainingDaysInIsolation
        )
        onView(withId(R.id.riskyContactIsolationAdviceRemainingDaysInIsolation))
            .perform(scrollTo())
            .check(matches(withText(expectedDaysInIsolationText)))

        onView(withId(R.id.riskyContactIsolationAdviceStateInfoView))
            .perform(scrollTo())
            .apply {
                check(matches(withStateStringResource(R.string.risky_contact_isolation_advice_already_isolating_information)))
                check(matches(withStateColor(R.color.amber)))
            }
        checkAdvice(
            stringResId = R.string.risky_contact_isolation_advice_already_isolating_stay_at_home_advice,
            drawableRes = R.drawable.ic_stay_at_home
        )
        onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_already_isolating_acknowledge_button_text)))
        onView(withId(R.id.secondaryActionButton))
            .check(matches(withEffectiveVisibility(GONE)))
    }

    private fun checkAdvice(@StringRes stringResId: Int, @DrawableRes drawableRes: Int) {
        onView(withId(R.id.adviceContainer))
            .perform(scrollTo())
            .check(matches(hasDescendant(withIconAndText(stringResId, drawableRes))))
    }

    fun clickPrimaryBackToHome() {
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_already_isolating_acknowledge_button_text)))
            .perform(click())
    }

    fun clickSecondaryBackToHome() {
        onView(withId(R.id.secondaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
            .perform(click())
    }
}
