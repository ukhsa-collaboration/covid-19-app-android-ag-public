package uk.nhs.nhsx.covid19.android.app.qrcode

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class QrScannerActivityTest : EspressoTest(), HasActivity {
    override val containerId: Int
        get() = R.id.container

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<QrScannerActivity>()
        checkActivityIsDisplayed()
    }
}
