package uk.nhs.nhsx.covid19.android.app.common

import junit.framework.Assert.fail
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity
import uk.nhs.nhsx.covid19.android.app.receiver.AndroidBluetoothStateProvider
import kotlin.test.assertTrue

class EnableBluetoothActivityTest : EspressoTest(), HasActivity {

    override val containerId: Int
        get() = R.id.edgeCaseContainer

    /**
     * This test bypasses the logic within [AndroidBluetoothStateProvider.getState], in favour of using
     * [AndroidBluetoothStateProvider.onReceive].
     */
    @Test
    fun activityIsDisplayed() {
        testAppContext.setBluetoothEnabled(false)
        startTestActivity<EnableBluetoothActivity>()
        checkActivityIsDisplayed()
    }

    @Test
    fun activityFinishesDueToEnabledBluetooth() {
        testAppContext.setBluetoothEnabled(true)
        startTestActivity<EnableBluetoothActivity>()?.let {
            assertTrue(it.isFinishing)
        } ?: fail("The activity should have been created!")
    }
}
