package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.app.Activity.RESULT_CANCELED
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.contrib.ActivityResultMatchers.hasResultCode
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest

class LocalAuthorityActivityResultTest : EspressoTest() {

    @Test
    fun startingActivityWhenPostCodeResolutionIsNotPossible_shouldCancelActivity() {
        val scenario = ActivityScenario.launch(LocalAuthorityActivity::class.java)

        waitFor { assertThat(scenario.result, hasResultCode(RESULT_CANCELED)) }
    }
}
