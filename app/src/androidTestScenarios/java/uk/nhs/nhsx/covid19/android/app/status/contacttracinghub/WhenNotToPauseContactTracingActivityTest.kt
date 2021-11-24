package uk.nhs.nhsx.covid19.android.app.status.contacttracinghub

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

internal class WhenNotToPauseContactTracingActivityTest : EspressoTest(), HasActivity {
    override val containerId: Int
        get() = R.id.whenNotToPauseContactTracingContainer

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<WhenNotToPauseContactTracingActivity>()
        checkActivityIsDisplayed()
    }
}
