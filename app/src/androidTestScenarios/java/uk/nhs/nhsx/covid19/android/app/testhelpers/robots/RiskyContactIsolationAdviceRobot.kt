package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.Default
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.WalesWithinAdviceWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.IconTextViewMatcher.Companion.withIconAndText
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.withStateColor
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.withStateStringResource
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor
import uk.nhs.nhsx.covid19.android.app.testhelpers.withDrawable
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat

class RiskyContactIsolationAdviceRobot {

    fun checkActivityIsDisplayed() {
        waitFor {
            onView(withId(R.id.riskyContactIsolationAdviceContainer))
                .check(matches(isDisplayed()))
        }
    }

    fun checkIsInNotIsolatingAsMinorViewState(country: SupportedCountry, testingAdviceToShow: TestingAdviceToShow) {
        val adviceValues = when (country) {
            ENGLAND -> RiskyContactIsolationAdviceValues(
                title = R.string.risky_contact_isolation_advice_minors_no_self_isolation_required,
                banner = R.string.risky_contact_isolation_advice_minors_information,
                adviceOne = R.string.risky_contact_isolation_advice_minors_testing_advice,
                adviceTwo = R.string.risky_contact_isolation_advice_minors_show_to_adult_advice
            )
            WALES -> RiskyContactIsolationAdviceValues(
                title = R.string.risky_contact_isolation_advice_minors_no_self_isolation_required_wls,
                banner = R.string.risky_contact_isolation_advice_minors_information_wls,
                adviceOne = R.string.risky_contact_isolation_advice_minors_testing_advice_wls,
                adviceTwo = R.string.risky_contact_isolation_advice_minors_show_to_adult_advice_wls
            )
        }

        onView(withId(R.id.riskyContactIsolationAdviceTitle))
            .perform(scrollTo())
            .check(matches(withText(adviceValues.title)))

        onView(withId(R.id.riskyContactIsolationAdviceRemainingDaysInIsolation))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.riskyContactIsolationAdviceStateInfoView))
            .perform(scrollTo())
            .apply {
                check(matches(withStateStringResource(adviceValues.banner)))
                check(matches(withStateColor(R.color.amber)))
            }

        checkAdviceList {
            checkAdvice(
                stringResId = adviceValues.adviceOne,
                drawableRes = R.drawable.ic_social_distancing
            )
            checkAdvice(
                stringResId = adviceValues.adviceTwo,
                drawableRes = R.drawable.ic_family
            )
            if (testingAdviceToShow is WalesWithinAdviceWindow) {
                val formattedDate = testingAdviceToShow.date.uiLongFormat(context)
                val expectedString = context.getString(
                    R.string.contact_case_no_isolation_under_age_limit_list_item_testing_with_date,
                    formattedDate
                )
                checkAdvice(text = expectedString, drawableRes = R.drawable.ic_get_free_test)
            }
        }

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

    fun checkIsInNotIsolatingAsFullyVaccinatedViewState(country: SupportedCountry, testingAdviceToShow: TestingAdviceToShow) {
        val adviceValues = when (country) {
            ENGLAND -> RiskyContactIsolationAdviceValues(
                title = R.string.risky_contact_isolation_advice_already_vaccinated_no_self_isolation_required,
                banner = R.string.risky_contact_isolation_advice_already_vaccinated_information,
                adviceOne = R.string.risky_contact_isolation_advice_already_vaccinated_vaccine_research,
                adviceTwo = R.string.risky_contact_isolation_advice_already_vaccinated_testing_advice
            )
            WALES -> RiskyContactIsolationAdviceValues(
                title = R.string.risky_contact_isolation_advice_already_vaccinated_no_self_isolation_required_wls,
                banner = R.string.risky_contact_isolation_advice_already_vaccinated_information_wls,
                adviceOne = R.string.risky_contact_isolation_advice_already_vaccinated_vaccine_research_wls,
                adviceTwo = R.string.risky_contact_isolation_advice_already_vaccinated_testing_advice_wls
            )
        }

        onView(withId(R.id.riskyContactIsolationAdviceTitle))
            .perform(scrollTo())
            .check(matches(withText(adviceValues.title)))

        onView(withId(R.id.riskyContactIsolationAdviceRemainingDaysInIsolation))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.riskyContactIsolationAdviceStateInfoView))
            .perform(scrollTo())
            .apply {
                check(matches(withStateStringResource(adviceValues.banner)))
                check(matches(withStateColor(R.color.amber)))
            }

        checkAdviceList {
            checkAdvice(
                stringResId = adviceValues.adviceOne,
                drawableRes = R.drawable.ic_info
            )
            checkAdvice(
                stringResId = adviceValues.adviceTwo,
                drawableRes = R.drawable.ic_social_distancing
            )
            if (testingAdviceToShow is WalesWithinAdviceWindow) {
                val formattedDate = testingAdviceToShow.date.uiLongFormat(context)
                val expectedString = context.getString(
                    R.string.contact_case_no_isolation_fully_vaccinated_list_item_testing_with_date,
                    formattedDate
                )
                checkAdvice(text = expectedString, drawableRes = R.drawable.ic_get_free_test)
            }
        }
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
        checkAdviceList {
            checkAdvice(
                stringResId = R.string.risky_contact_isolation_advice_medically_exempt_research,
                drawableRes = R.drawable.ic_info
            )
            checkAdvice(
                stringResId = R.string.risky_contact_isolation_advice_medically_exempt_group,
                drawableRes = R.drawable.ic_group_of_people
            )
            checkAdvice(
                stringResId = R.string.risky_contact_isolation_advice_medically_exempt_advice,
                drawableRes = R.drawable.ic_social_distancing
            )
        }
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

    fun checkIsInNewlyIsolatingViewState(remainingDaysInIsolation: Int, testingAdviceToShow: TestingAdviceToShow) {
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
        checkAdviceList {
            if (testingAdviceToShow is WalesWithinAdviceWindow) {
                val formattedDate = testingAdviceToShow.date.uiLongFormat(context)
                val expectedString =
                    context.getString(R.string.contact_case_start_isolation_list_item_testing_with_date, formattedDate)
                checkAdvice(text = expectedString, drawableRes = R.drawable.ic_get_free_test)
            } else if (testingAdviceToShow == Default) {
                checkAdvice(
                    text = context.getString(R.string.risky_contact_isolation_advice_new_isolation_testing_advice),
                    drawableRes = R.drawable.ic_get_free_test
                )
            }
            checkAdvice(
                stringResId = R.string.risky_contact_isolation_advice_new_isolation_stay_at_home_advice,
                drawableRes = R.drawable.ic_stay_at_home
            )
        }
        onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_book_pcr_test)))
        onView(withId(R.id.secondaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
    }

    fun checkIsInAlreadyIsolatingViewState(remainingDaysInIsolation: Int, testingAdviceToShow: TestingAdviceToShow) {
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
        checkAdviceList {
            if (testingAdviceToShow is WalesWithinAdviceWindow) {
                val formattedDate = testingAdviceToShow.date.uiLongFormat(context)
                val expectedString = context.getString(
                    R.string.contact_case_continue_isolation_list_item_testing_with_date,
                    formattedDate
                )
                checkAdvice(text = expectedString, drawableRes = R.drawable.ic_get_free_test)
            }
            checkAdvice(
                stringResId = R.string.risky_contact_isolation_advice_already_isolating_stay_at_home_advice,
                drawableRes = R.drawable.ic_stay_at_home
            )
        }
        onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_already_isolating_acknowledge_button_text)))
        onView(withId(R.id.secondaryActionButton))
            .check(matches(withEffectiveVisibility(GONE)))
    }

    private fun checkAdviceList(checks: AdviceChecker.() -> Unit) {
        val adviceChecker = AdviceChecker()
        adviceChecker.checks()
        onView(withId(R.id.adviceContainer)).check(matches(hasChildCount(adviceChecker.itemsChecked)))
    }

    class AdviceChecker {

        private val checkedTexts = mutableSetOf<String>()

        val itemsChecked: Int
            get() = checkedTexts.size

        fun checkAdvice(@StringRes stringResId: Int, @DrawableRes drawableRes: Int) {
            checkAdvice(context.getString(stringResId), drawableRes)
        }

        fun checkAdvice(text: String, @DrawableRes drawableRes: Int) {
            onView(withId(R.id.adviceContainer))
                .perform(scrollTo())
                .check(matches(hasDescendant(withIconAndText(text, drawableRes))))
            checkedTexts.add(text)
        }
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

    fun verifyCloseButton() {
        onView(allOf(instanceOf(AppCompatImageButton::class.java), isDescendantOfA(withId(R.id.primaryToolbar))))
            .check(matches(withDrawable(tint = R.color.nhs_blue, id = R.drawable.ic_close_primary)))
    }

    private data class RiskyContactIsolationAdviceValues(
        @StringRes val title: Int,
        @StringRes val banner: Int,
        @StringRes val adviceOne: Int,
        @StringRes val adviceTwo: Int
    )
}
