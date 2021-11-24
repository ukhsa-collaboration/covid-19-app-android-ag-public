package uk.nhs.nhsx.covid19.android.app

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class AnalyticsReportActivityTest : EspressoTest(), HasActivity {
    override val containerId: Int
        get() = R.id.scenarios

    @Test
    fun isActivityDisplayed() {
        startTestActivity<AnalyticsReportActivity>()
        checkActivityIsDisplayed()
    }
}
