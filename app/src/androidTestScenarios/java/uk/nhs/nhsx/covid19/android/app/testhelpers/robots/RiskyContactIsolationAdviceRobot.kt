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
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.EvaluateTestingAdviceToShow.TestingAdviceToShow.WalesWithinAdviceWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.WALES
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.IconTextViewMatcher.Companion.withIconAndText
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.TextViewDrawableMatcher.Companion.withTextViewHasDrawableEnd
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.TextViewDrawableMatcher.Companion.withTextViewNoDrawable
import uk.nhs.nhsx.covid19.android.app.testhelpers.matcher.isAnnouncedAsOpenInBrowser
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

    fun checkIsNotIsolatingAsMinorViewState(country: SupportedCountry, testingAdviceToShow: TestingAdviceToShow) {
        val adviceValues = when (country) {
            ENGLAND -> RiskyContactIsolationAdviceValues(
                title = R.string.risky_contact_isolation_advice_minors_no_self_isolation_required,
                banner = R.string.risky_contact_isolation_advice_minors_information,
                adviceOne =
                RiskyContactIsolationAdviceEntry(
                    text = R.string.risky_contact_isolation_advice_minors_show_to_adult_advice,
                    image = R.drawable.ic_family
                ),
                adviceTwo =
                RiskyContactIsolationAdviceEntry(
                    text = R.string.contact_case_no_isolation_under_age_limit_list_item_social_distancing_england,
                    image = R.drawable.ic_social_distancing
                ),
                adviceThree = RiskyContactIsolationAdviceEntry(
                    text = R.string.contact_case_no_isolation_under_age_limit_list_item_get_tested_before_meeting_vulnerable_people_england,
                    image = R.drawable.ic_get_free_test
                ),
                adviceFour = RiskyContactIsolationAdviceEntry(
                    text = R.string.contact_case_no_isolation_under_age_limit_list_item_wear_a_mask_england,
                    image = R.drawable.ic_mask
                )
            )
            WALES -> RiskyContactIsolationAdviceValues(
                title = R.string.risky_contact_isolation_advice_minors_no_self_isolation_required_wls,
                banner = R.string.risky_contact_isolation_advice_minors_information_wls,
                adviceOne =
                RiskyContactIsolationAdviceEntry(
                    text = R.string.risky_contact_isolation_advice_minors_testing_advice_wls,
                    image = R.drawable.ic_social_distancing
                ),
                adviceTwo =
                RiskyContactIsolationAdviceEntry(
                    text = R.string.risky_contact_isolation_advice_minors_show_to_adult_advice_wls,
                    image = R.drawable.ic_family
                )
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
            with(adviceValues) {
                checkAdvice(
                    stringResId = adviceOne.text,
                    drawableRes = adviceOne.image
                )
                checkAdvice(
                    stringResId = adviceTwo.text,
                    drawableRes = adviceTwo.image
                )
                if (adviceThree != null)
                    checkAdvice(
                        stringResId = adviceThree.text,
                        drawableRes = adviceThree.image
                    )
                if (adviceFour != null)
                    checkAdvice(
                        stringResId = adviceFour.text,
                        drawableRes = adviceFour.image
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
        }

        when (country) {
            ENGLAND -> {
                onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
                    .check(matches(not(isDisplayed())))
                onView(withId(R.id.furtherAdviceTextView))
                    .check(matches(not(isDisplayed())))
                onView(withId(R.id.nhsGuidanceLinkTextView))
                    .check(matches(not(isDisplayed())))
                onView(withId(R.id.primaryActionButton))
                    .perform(scrollTo())
                    .check(matches(withText(R.string.contact_case_no_isolation_under_age_limit_primary_button_title_read_guidance_england)))
                onView(withId(R.id.primaryActionButton)).check(
                    matches(allOf(withTextViewHasDrawableEnd(), isAnnouncedAsOpenInBrowser()))
                )
                onView(withId(R.id.secondaryActionButton))
                    .perform(scrollTo())
                    .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
                assertBrowserIsOpened(R.string.contact_case_guidance_for_contacts_in_england_url) {
                    clickPrimaryButton()
                }
            }
            else -> {
                onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
                    .perform(scrollTo())
                    .check(
                        matches(
                            allOf(
                                withText(R.string.risky_contact_isolation_advice_faq_button_title),
                                isDisplayed()
                            )
                        )
                    )
                onView(withId(R.id.furtherAdviceTextView))
                    .perform(scrollTo())
                    .check(
                        matches(
                            allOf(
                                withText(R.string.risky_contact_isolation_advice_further_nhs_guidance),
                                isDisplayed()
                            )
                        )
                    )
                onView(withId(R.id.nhsGuidanceLinkTextView))
                    .perform(scrollTo())
                    .check(
                        matches(
                            allOf(
                                withText(R.string.risky_contact_isolation_advice_nhs_guidance_link_text),
                                isDisplayed()
                            )
                        )
                    )
                onView(withId(R.id.primaryActionButton))
                    .perform(scrollTo())
                    .check(matches(withText(R.string.risky_contact_isolation_advice_book_pcr_test)))
                onView(withId(R.id.primaryActionButton)).check(matches(withTextViewNoDrawable()))
                onView(withId(R.id.secondaryActionButton))
                    .perform(scrollTo())
                    .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
            }
        }
    }

    fun checkIsInNotIsolatingAsFullyVaccinatedViewState(
        country: SupportedCountry,
        testingAdviceToShow: TestingAdviceToShow
    ) {
        val adviceValues = when (country) {
            ENGLAND -> RiskyContactIsolationAdviceValues(
                title = R.string.risky_contact_isolation_advice_already_vaccinated_no_self_isolation_required,
                banner = R.string.risky_contact_isolation_advice_already_vaccinated_information,
                adviceOne =
                RiskyContactIsolationAdviceEntry(
                    text = R.string.contact_case_no_isolation_fully_vaccinated_list_item_social_distancing_england,
                    image = R.drawable.ic_social_distancing
                ),
                adviceTwo =
                RiskyContactIsolationAdviceEntry(
                    text = R.string.contact_case_no_isolation_fully_vaccinated_list_item_get_tested_before_meeting_vulnerable_people_england,
                    image = R.drawable.ic_get_free_test
                ),
                adviceThree =
                RiskyContactIsolationAdviceEntry(
                    text = R.string.contact_case_no_isolation_fully_vaccinated_list_item_wear_a_mask_england,
                    image = R.drawable.ic_mask
                ),
                adviceFour =
                RiskyContactIsolationAdviceEntry(
                    text = R.string.contact_case_no_isolation_fully_vaccinated_list_item_work_from_home_england,
                    image = R.drawable.ic_work_from_home
                ),
            )
            WALES -> RiskyContactIsolationAdviceValues(
                title = R.string.risky_contact_isolation_advice_already_vaccinated_no_self_isolation_required_wls,
                banner = R.string.risky_contact_isolation_advice_already_vaccinated_information_wls,
                adviceOne =
                RiskyContactIsolationAdviceEntry(
                    text = R.string.risky_contact_isolation_advice_already_vaccinated_vaccine_research_wls,
                    image = R.drawable.ic_info
                ),
                adviceTwo =
                RiskyContactIsolationAdviceEntry(
                    text = R.string.risky_contact_isolation_advice_already_vaccinated_testing_advice_wls,
                    image = R.drawable.ic_social_distancing
                )
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
            with(adviceValues) {
                checkAdvice(
                    stringResId = adviceOne.text,
                    drawableRes = adviceOne.image
                )
                checkAdvice(
                    stringResId = adviceTwo.text,
                    drawableRes = adviceTwo.image
                )
                if (adviceThree != null)
                    checkAdvice(
                        stringResId = adviceThree.text,
                        drawableRes = adviceThree.image
                    )
                if (adviceFour != null)
                    checkAdvice(
                        stringResId = adviceFour.text,
                        drawableRes = adviceFour.image
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
        }

        when (country) {
            ENGLAND -> {
                onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
                    .check(matches(not(isDisplayed())))
                onView(withId(R.id.furtherAdviceTextView))
                    .check(matches(not(isDisplayed())))
                onView(withId(R.id.nhsGuidanceLinkTextView))
                    .check(matches(not(isDisplayed())))

                onView(withId(R.id.primaryActionButton))
                    .perform(scrollTo())
                    .check(
                        matches(
                            allOf(
                                withText(R.string.contact_case_no_isolation_fully_vaccinated_primary_button_title_read_guidance_england),
                                withTextViewHasDrawableEnd(),
                                isAnnouncedAsOpenInBrowser()
                            )
                        )
                    )
                onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
                    .check(matches(not(isDisplayed())))
            }
            WALES -> {
                onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
                    .perform(scrollTo())
                    .check(
                        matches(
                            allOf(
                                withText(R.string.risky_contact_isolation_advice_faq_button_title),
                                isDisplayed()
                            )
                        )
                    )
                onView(withId(R.id.furtherAdviceTextView))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))
                onView(withId(R.id.nhsGuidanceLinkTextView))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))

                onView(withId(R.id.primaryActionButton))
                    .perform(scrollTo())
                    .check(
                        matches(
                            allOf(
                                withText(R.string.risky_contact_isolation_advice_book_pcr_test),
                                withTextViewNoDrawable()
                            )
                        )
                    )
            }
        }

        onView(withId(R.id.secondaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
    }

    fun checkIsInNotIsolatingAsMedicallyExemptViewStateForEngland() {
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
                stringResId = R.string.risky_contact_isolation_advice_medically_exempt_social_distancing_england,
                drawableRes = R.drawable.ic_social_distancing
            )
            checkAdvice(
                stringResId = R.string.risky_contact_isolation_advice_medically_exempt_get_tested_before_meeting_vulnerable_people_england,
                drawableRes = R.drawable.ic_get_free_test
            )
            checkAdvice(
                stringResId = R.string.risky_contact_isolation_advice_medically_exempt_wear_a_mask_england,
                drawableRes = R.drawable.ic_mask
            )
            checkAdvice(
                stringResId = R.string.risky_contact_isolation_advice_medically_exempt_work_from_home_england,
                drawableRes = R.drawable.ic_work_from_home
            )
        }
        onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.furtherAdviceTextView))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.nhsGuidanceLinkTextView))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(
                matches(
                    allOf(
                        withText(R.string.risky_contact_isolation_advice_medically_exempt_primary_button_title_read_guidance_england),
                        withTextViewHasDrawableEnd(),
                        isAnnouncedAsOpenInBrowser()
                    )
                )
            )
        onView(withId(R.id.secondaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
    }

    fun checkIsInNewlyIsolatingViewState(
        supportedCountry: SupportedCountry,
        remainingDaysInIsolation: Int,
        testingAdviceToShow: TestingAdviceToShow
    ) {

        data class Values(
            val adviceOne: String,
            @StringRes val buttonTitle: Int
        )

        val values = when (supportedCountry) {
            ENGLAND -> Values(
                adviceOne = context.getString(R.string.risky_contact_isolation_advice_new_isolation_testing_advice),
                buttonTitle = R.string.risky_contact_isolation_advice_book_pcr_test
            )
            WALES -> {
                if (testingAdviceToShow is WalesWithinAdviceWindow) {
                    val formattedDate = testingAdviceToShow.date.uiLongFormat(context)
                    val expectedString =
                        context.getString(
                            R.string.contact_case_start_isolation_list_item_testing_with_date,
                            formattedDate
                        )
                    Values(
                        adviceOne = expectedString,
                        buttonTitle = R.string.contact_case_start_isolation_primary_button_title_wales
                    )
                } else {
                    Values(
                        adviceOne = context.getString(R.string.contact_case_start_isolation_list_item_testing_once_asap_wales),
                        buttonTitle = R.string.contact_case_start_isolation_primary_button_title_wales
                    )
                }
            }
        }

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
            checkAdvice(
                text = values.adviceOne,
                drawableRes = R.drawable.ic_get_free_test
            )
            checkAdvice(
                stringResId = R.string.risky_contact_isolation_advice_new_isolation_stay_at_home_advice,
                drawableRes = R.drawable.ic_stay_at_home
            )
        }
        onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
            .check(matches(not(isDisplayed())))

        onView(withId(R.id.furtherAdviceTextView))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.nhsGuidanceLinkTextView))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(matches(allOf(withText(values.buttonTitle), withTextViewNoDrawable())))

        onView(withId(R.id.secondaryActionButton))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_go_back_to_home)))
    }

    fun checkIsInAlreadyIsolatingViewState(remainingDaysInIsolation: Int, testingAdviceToShow: TestingAdviceToShow) {
        val expectedDaysInIsolationText = context.resources.getQuantityString(
            R.plurals.state_isolation_days,
            remainingDaysInIsolation,
            remainingDaysInIsolation
        )
        onView(withId(R.id.riskyContactIsolationAdviceRemainingDaysInIsolation))
            .perform(scrollTo())
            .check(matches(withText(expectedDaysInIsolationText)))

        if (testingAdviceToShow is WalesWithinAdviceWindow) {
            checkIsInAlreadyIsolatingViewStateWales()
        } else {
            checkIsInAlreadyIsolatingViewStateEngland()
        }

        onView(withId(R.id.riskyContactIsolationAdviceCommonQuestions))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.furtherAdviceTextView))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withId(R.id.nhsGuidanceLinkTextView))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
            .check(
                matches(
                    allOf(
                        withText(R.string.risky_contact_isolation_advice_already_isolating_acknowledge_button_text),
                        withTextViewNoDrawable()
                    )
                )
            )
        onView(withId(R.id.secondaryActionButton))
            .check(matches(withEffectiveVisibility(GONE)))
    }

    private fun checkIsInAlreadyIsolatingViewStateWales() {
        onView(withId(R.id.riskyContactIsolationAdviceTitle))
            .perform(scrollTo())
            .check(matches(withText(R.string.contact_case_continue_isolation_title_wls)))

        onView(withId(R.id.riskyContactIsolationAdviceStateInfoView))
            .perform(scrollTo())
            .apply {
                check(matches(withStateStringResource(R.string.contact_case_continue_isolation_info_box_wls)))
                check(matches(withStateColor(R.color.amber)))
            }
        checkAdviceList {
            checkAdvice(
                stringResId = R.string.contact_case_continue_isolation_list_item_isolation_wls,
                drawableRes = R.drawable.ic_stay_at_home
            )
        }
    }

    private fun checkIsInAlreadyIsolatingViewStateEngland() {
        checkAdviceList {
            checkAdvice(
                stringResId = R.string.risky_contact_isolation_advice_already_isolating_stay_at_home_advice,
                drawableRes = R.drawable.ic_stay_at_home
            )
        }

        onView(withId(R.id.riskyContactIsolationAdviceTitle))
            .perform(scrollTo())
            .check(matches(withText(R.string.risky_contact_isolation_advice_continue_isolataion_for)))

        onView(withId(R.id.riskyContactIsolationAdviceStateInfoView))
            .perform(scrollTo())
            .apply {
                check(matches(withStateStringResource(R.string.risky_contact_isolation_advice_already_isolating_information)))
                check(matches(withStateColor(R.color.amber)))
            }
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

    fun clickPrimaryButton() {
        onView(withId(R.id.primaryActionButton))
            .perform(scrollTo())
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
        @StringRes val adviceOne: RiskyContactIsolationAdviceEntry,
        @StringRes val adviceTwo: RiskyContactIsolationAdviceEntry,
        @StringRes val adviceThree: RiskyContactIsolationAdviceEntry? = null,
        @StringRes val adviceFour: RiskyContactIsolationAdviceEntry? = null,
    )

    private data class RiskyContactIsolationAdviceEntry(
        @StringRes val text: Int,
        @DrawableRes val image: Int
    )
}
