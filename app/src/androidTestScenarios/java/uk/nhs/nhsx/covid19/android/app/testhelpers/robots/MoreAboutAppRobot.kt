package uk.nhs.nhsx.covid19.android.app.testhelpers.robots

import uk.nhs.nhsx.covid19.android.app.R

class MoreAboutAppRobot {
    fun checkActivityIsDisplayed() {
        checkActivityTitleIsDisplayed(R.string.about_this_app_title)
    }
}
