package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot

class SubmitKeysProgressActivityTest : EspressoTest() {

    private val submitKeysProgressRobot = ProgressRobot()

    @Test
    fun startActivityWithAllExtrasAndFailingDelayedSubmissionApi_showsLoading() {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL
        MockApiModule.behaviour.delayMillis = 500
        startTestActivity<SubmitKeysProgressActivity> {
            putParcelableArrayListExtra("EXPOSURE_KEYS_TO_SUBMIT", ArrayList<NHSTemporaryExposureKey>())
            putExtra("SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN", "test")
        }

        submitKeysProgressRobot.checkLoadingIsDisplayed()
        waitFor { submitKeysProgressRobot.checkErrorIsDisplayed() }
    }
}
