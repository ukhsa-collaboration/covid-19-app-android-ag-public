package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.app.Activity.RESULT_CANCELED
import androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest

class LocalAuthorityActivityResultTest : EspressoTest() {

    @get:Rule
    val activityRule = ActivityTestRule<LocalAuthorityActivity>(LocalAuthorityActivity::class.java)

    @Test
    fun startingActivityWhenPostCodeResolutionIsNotPossible_shouldCancelActivity() = notReported {
        waitFor { assertThat(activityRule.activityResult, hasResultCode(RESULT_CANCELED)) }
    }
}
