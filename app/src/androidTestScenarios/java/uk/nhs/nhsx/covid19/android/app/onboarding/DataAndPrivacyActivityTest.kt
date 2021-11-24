package uk.nhs.nhsx.covid19.android.app.onboarding

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class DataAndPrivacyActivityTest : EspressoTest(), HasActivity {
    override val containerId: Int
        get() = R.id.dateAndPrivacyContainer

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<DataAndPrivacyActivity>()
        checkActivityIsDisplayed()
    }
}
