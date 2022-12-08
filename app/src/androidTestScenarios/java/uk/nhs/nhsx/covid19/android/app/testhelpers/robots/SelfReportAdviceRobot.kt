package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.matcher.ViewMatchers
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.testhelpers.nestedScrollTo
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.not
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.plurals
import uk.nhs.nhsx.covid19.android.app.R.string
import uk.nhs.nhsx.covid19.android.app.testhelpers.withDrawable
import uk.nhs.nhsx.covid19.android.app.util.uiFullFormat
import java.time.LocalDate

class SelfReportAdviceRobot : HasActivity {
    override val containerId: Int
        get() = id.selfReportAdviceContainer

    private val notReportedContainerSubHeading = onView(
        Matchers.allOf(
            withId(id.selfReportAdviceNotReportedSubTitle),
            ViewMatchers.isDescendantOfA(withId(id.nowReportContainer))
        )
    )

    fun checkDisplaysHasReportedNoNeedToIsolate() {
        onView(withId(id.selfReportAdviceImage))
            .check((matches(withDrawable(R.drawable.ic_onboarding_welcome))))

        onView(withId(id.selfReportAdviceMainTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText(string.self_report_advice_reported_result_out_of_isolation_header)))

        onView(withId(id.selfReportAdviceNoIsolationInfoBox))
            .check(matches(isDisplayed()))

        onView(withId(id.primaryActionButton))
            .perform(nestedScrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkDisplaysHasNotReportedNoNeedToIsolate() {
        onView(withId(id.selfReportAdviceImage))
            .check((matches(withDrawable(R.drawable.ic_isolation_book_test))))

        onView(withId(id.nowReportContainer))
            .perform(nestedScrollTo())
            .check(matches(isDisplayed()))

        notReportedContainerSubHeading.perform(nestedScrollTo())
            .check(matches(withText(string.self_report_advice_reported_result_out_of_isolation_header)))

        onView(withId(id.selfReportAdviceNoIsolationInfoBox))
            .perform(nestedScrollTo())
            .check(matches(isDisplayed()))

        onView(withId(id.primaryLinkActionButton))
            .perform(nestedScrollTo())
            .check(matches(isDisplayed()))

        onView(withId(id.secondaryActionButton))
            .perform(nestedScrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkDisplaysHasReportedIsolate(expectedDaysLeft: Int) {
        onView(withId(id.selfReportAdviceImage))
            .check((matches(withDrawable(R.drawable.ic_isolation_continue))))

        val isolateHeaderText = context.resources.getQuantityString(
            plurals.self_report_advice_isolate_header,
            expectedDaysLeft,
            expectedDaysLeft
        )

        onView(withId(id.selfReportAdviceMainTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText(isolateHeaderText)))

        onView(withId(id.selfReportIsolateIconBulletSection))
            .check(matches(isDisplayed()))

        onView(withId(id.primaryActionButton))
            .perform(nestedScrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkDisplaysHasNotReportedIsolate(expectedDaysLeft: Int, expectedEndDate: LocalDate) {
        onView(withId(id.selfReportAdviceImage))
            .check((matches(withDrawable(R.drawable.ic_isolation_book_test))))

        onView(withId(id.nowReportContainer))
            .perform(nestedScrollTo())
            .check(matches(isDisplayed()))

        val isolateHeaderText = context.resources.getQuantityString(
            plurals.self_report_advice_isolate_subheader,
            expectedDaysLeft,
            expectedDaysLeft,
            expectedEndDate.minusDays(1).uiFullFormat(context)
        )

        notReportedContainerSubHeading.perform(nestedScrollTo())
            .check(matches(withText(isolateHeaderText)))

        onView(withId(id.primaryLinkActionButton))
            .perform(nestedScrollTo())
            .check(matches(isDisplayed()))

        onView(withId(id.secondaryActionButton))
            .perform(nestedScrollTo())
            .check(matches(isDisplayed()))
    }

    fun checkReadMoreLinkEngland() {
        onView(withId(id.covidLinkTextView))
            .perform(nestedScrollTo())
            .check(matches(withText(string.self_report_advice_read_more_url_label)))
    }

    fun checkReadMoreLinkWales() {
        onView(withId(id.covidLinkTextView))
            .perform(nestedScrollTo())
            .check(matches(withText(string.self_report_advice_read_more_url_label_wls)))
    }

    fun clickPrimaryBackToHomeButton() {
        onView(withId(id.primaryActionButton))
            .perform(nestedScrollTo(), ViewActions.click())
    }

    fun clickSecondaryBackToHomeButton() {
        onView(withId(id.secondaryActionButton))
            .perform(nestedScrollTo(), ViewActions.click())
    }
}
