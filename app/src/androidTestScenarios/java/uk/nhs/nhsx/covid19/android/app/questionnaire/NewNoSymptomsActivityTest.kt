package uk.nhs.nhsx.covid19.android.app.questionnaire

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class NewNoSymptomsActivityTest : EspressoTest(), HasActivity {
    override val containerId: Int
        get() = R.id.newNoSymptomsContainer

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<NewNoSymptomsActivity>()
        checkActivityIsDisplayed()
    }
}
