package uk.nhs.nhsx.covid19.android.app.edgecases

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class AgeRestrictionActivityTest : EspressoTest(), HasActivity {
    override val containerId: Int
        get() = R.id.edgeCaseContainer

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<AgeRestrictionActivity>()
        checkActivityIsDisplayed()
    }
}
