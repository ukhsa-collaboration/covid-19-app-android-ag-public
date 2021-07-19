package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R

class LocalAuthorityRobot {

    fun checkActivityIsDisplayed() {
        onView(withId(R.id.localAuthorityContainer))
            .check(matches(isDisplayed()))
    }

    fun checkSingleAuthorityIsDisplayed(postCode: String, localAuthorityName: String) {
        onView(withId(R.id.titleLocalAuthority))
            .check(
                matches(
                    withText(
                        context.getString(R.string.single_local_authority_title, postCode, localAuthorityName)
                    )
                )
            )
        onView(withId(R.id.descriptionLocalAuthority))
            .perform(scrollTo())
            .check(matches(withText(context.getString(R.string.single_local_authority_description))))
        onView(withId(R.id.moreInfoLink))
            .check(matches(not(isDisplayed())))
        onView(withId(R.id.localAuthoritiesRadioGroup))
            .check(matches(not(isDisplayed())))
    }

    fun checkMultipleAuthoritiesAreDisplayed(postCode: String) {
        onView(withId(R.id.titleLocalAuthority))
            .check(matches(withText(context.getString(R.string.multiple_local_authorities_title))))
        onView(withId(R.id.descriptionLocalAuthority))
            .perform(scrollTo())
            .check(
                matches(
                    withText(
                        context.getString(R.string.multiple_local_authorities_description, postCode)
                    )
                )
            )
        onView(withId(R.id.moreInfoLink))
            .check(matches(isDisplayed()))
            .perform(scrollTo())
        onView(withId(R.id.localAuthoritiesRadioGroup))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkErrorIsNotDisplayed() {
        onView(withId(R.id.descriptionLocalAuthority))
            .perform(scrollTo())
        onView(withId(R.id.errorView))
            .check(matches(not(isDisplayed())))
    }

    fun checkNoLocalAuthoritySelectedErrorIsDisplayed() {
        onView(withId(R.id.errorView))
            .perform(scrollTo())
        onView(withId(R.id.errorView))
            .check(matches(isDisplayed()))
        onView(withId(R.id.errorTitleView))
            .check(matches(withText(context.getString(R.string.local_authority_error_no_authority_selected_title))))
        onView(withId(R.id.errorDescriptionView))
            .check(matches(withText(context.getString(R.string.local_authority_error_no_authority_selected_description))))
    }

    fun checkLocalAuthorityNotSupportedErrorIsDisplayed() {
        onView(withId(R.id.errorView))
            .perform(scrollTo())
        onView(withId(R.id.errorView))
            .check(matches(isDisplayed()))
        onView(withId(R.id.errorTitleView))
            .check(matches(withText(context.getString(R.string.local_authority_error_authority_not_supported_title))))
        onView(withId(R.id.errorDescriptionView))
            .check(matches(withText(context.getString(R.string.local_authority_error_authority_not_supported_description))))
    }

    fun clickConfirm() {
        onView(withId(R.id.buttonConfirmLink))
            .perform(scrollTo(), click())
    }

    fun selectLocalAuthority(localAuthorityName: String) {
        onView(withText(localAuthorityName))
            .perform(scrollTo(), click())
    }
}
