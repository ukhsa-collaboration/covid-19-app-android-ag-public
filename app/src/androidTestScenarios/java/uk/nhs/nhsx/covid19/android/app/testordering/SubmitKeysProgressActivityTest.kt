package uk.nhs.nhsx.covid19.android.app.testordering

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SubmitKeysProgressRobot

class SubmitKeysProgressActivityTest : EspressoTest() {

    private val submitKeysProgressRobot = SubmitKeysProgressRobot()

    @Test
    fun startActivityWithoutExtras() = notReported {
        startTestActivity<SubmitKeysProgressActivity>()
    }

    @Test
    fun startActivityWithExposureKeysExtra() = notReported {
        startTestActivity<SubmitKeysProgressActivity> {
            putParcelableArrayListExtra("EXPOSURE_KEYS_TO_SUBMIT", ArrayList<NHSTemporaryExposureKey>())
        }
    }

    @Test
    fun startActivityWithAllExtrasAndFailingSubmissionApi_showsError() = notReported {
        MockApiModule.behaviour.responseType = ALWAYS_FAIL
        startTestActivity<SubmitKeysProgressActivity> {
            putParcelableArrayListExtra("EXPOSURE_KEYS_TO_SUBMIT", ArrayList<NHSTemporaryExposureKey>())
            putExtra("SHARE_KEY_DIAGNOSIS_SUBMISSION_TOKEN", "test")
        }

        submitKeysProgressRobot.errorIsDisplayed()
    }
}
