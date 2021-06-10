package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import uk.nhs.nhsx.covid19.android.app.R

class QrCodeHelpRobot {

    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.qr_code_help_more_information)
    }
}
