package uk.nhs.nhsx.covid19.android.app.exposure

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class EnableExposureNotificationActivityTest : EspressoTest(), HasActivity {
    override val containerId: Int
        get() = R.id.yesButton

    @Test
    fun canActivityBeLaunchedSuccessfully() {
        startTestActivity<EnableExposureNotificationActivity>()
        checkActivityIsDisplayed()
    }
}
