package uk.nhs.nhsx.covid19.android.app.questionnaire.review

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class NoSymptomsActivityTest : EspressoTest(), HasActivity {

    override val containerId: Int
        get() = R.id.imageNoSymptom

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<NoSymptomsActivity>()
        checkActivityIsDisplayed()
    }
}
