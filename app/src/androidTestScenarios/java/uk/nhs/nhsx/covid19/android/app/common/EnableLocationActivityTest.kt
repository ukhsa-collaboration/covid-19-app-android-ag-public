package uk.nhs.nhsx.covid19.android.app.common

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class EnableLocationActivityTest : EspressoTest(), HasActivity {

    override val containerId: Int
        get() = R.id.edgeCaseContainer

    @Test
    fun isActivityShownWhenLocationIsDisabled() {
        testAppContext.setLocationEnabled(false)
        startTestActivity<EnableLocationActivity>()
        checkActivityIsDisplayed()
    }

    @Test
    fun isActivityFinishedWhenLocationIsEnabled() {
        testAppContext.setLocationEnabled(true)
        startTestActivity<EnableLocationActivity>()?.let {
            assertTrue(it.isFinishing)
        } ?: fail("Activity should have been created successfully!")
    }
}
