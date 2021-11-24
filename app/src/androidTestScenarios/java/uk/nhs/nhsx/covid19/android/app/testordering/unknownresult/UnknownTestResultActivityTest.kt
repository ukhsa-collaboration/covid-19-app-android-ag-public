package uk.nhs.nhsx.covid19.android.app.testordering.unknownresult

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class UnknownTestResultActivityTest : EspressoTest(), HasActivity {
    override val containerId: Int
        get() = R.id.unknownTestResultContainer

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<UnknownTestResultActivity>()
        checkActivityIsDisplayed()
    }
}
