package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import com.schibsted.spain.barista.interaction.BaristaSleepInteractions
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.DataAndPrivacyRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PermissionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PostCodeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.WelcomeRobot

class CompleteOnboarding {

    private val welcomeRobot = WelcomeRobot()
    private val dataAndPrivacyRobot = DataAndPrivacyRobot()
    private val postCodeRobot = PostCodeRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()
    private val permissionRobot = PermissionRobot()

    fun onboard() {
        welcomeRobot.checkActivityIsDisplayed()

        BaristaSleepInteractions.sleep(100)
        welcomeRobot.clickConfirmOnboarding()
        welcomeRobot.checkAgeConfirmationDialogIsDisplayed()
        welcomeRobot.clickConfirmAgePositive()

        dataAndPrivacyRobot.checkActivityIsDisplayed()
        dataAndPrivacyRobot.clickConfirmOnboarding()

        postCodeRobot.checkActivityIsDisplayed()
        postCodeRobot.enterPostCode("N12")
        postCodeRobot.checkContinueButtonIsDisplayed()
        postCodeRobot.clickContinue()

        localAuthorityRobot.checkActivityIsDisplayed()
        localAuthorityRobot.clickConfirm()

        permissionRobot.checkActivityIsDisplayed()
        permissionRobot.clickEnablePermissions()
    }
}
