package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matchers.not
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.R.id
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.SuccessfullySharedKeysAndNoNeedToReportTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.SuccessfullySharedKeysAndNHSTestNotReported
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.UnsuccessfullySharedKeysAndNoNeedToReportTest
import uk.nhs.nhsx.covid19.android.app.status.selfreporttest.SelfReportThankYouViewModel.FlowCase.UnsuccessfullySharedKeysAndNHSTestNotReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.NestedScrollViewScrollToAction
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class SelfReportThankYouRobot : HasActivity {
    override val containerId: Int
        get() = id.selfReportThankYouContainer

    fun checkCorrectParagraphIsShowing(screenFlow: FlowCase) {
        Espresso.onView(withId(id.paragraphText))
            .check(ViewAssertions.matches(
                when (screenFlow) {
                    SuccessfullySharedKeysAndNoNeedToReportTest -> withText(R.string.self_report_thank_you_para_successfully_shared_keys_and_no_need_to_report_test)
                    SuccessfullySharedKeysAndNHSTestNotReported -> withText(R.string.self_report_thank_you_para_sucessfully_shared_keys_and_nhs_test_not_reported)
                    UnsuccessfullySharedKeysAndNoNeedToReportTest -> withText(R.string.self_report_thank_you_para_unsuccessfully_shared_keys_and_no_need_to_report_test)
                    UnsuccessfullySharedKeysAndNHSTestNotReported -> withText(R.string.self_report_thank_you_para_unsuccessfully_shared_keys_and_nhs_test_not_reported)
                }
            ))
    }

    fun checkInfoViewIsVisible(shouldBeVisible: Boolean) {
        Espresso.onView(withId(id.eligibleInfoView))
            .check(ViewAssertions.matches(if (shouldBeVisible) isDisplayed() else not(isDisplayed())))
    }

    fun clickContinue() {
        Espresso.onView(withId(id.buttonContinue))
            .perform(NestedScrollViewScrollToAction(), ViewActions.click())
    }
}
