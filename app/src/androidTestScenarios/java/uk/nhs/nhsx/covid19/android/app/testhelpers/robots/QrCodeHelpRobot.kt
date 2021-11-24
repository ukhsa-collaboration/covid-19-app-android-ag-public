package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class QrCodeHelpRobot : HasActivity {

    override val containerId: Int
        get() = R.id.qrCodeHelpTitle
}
