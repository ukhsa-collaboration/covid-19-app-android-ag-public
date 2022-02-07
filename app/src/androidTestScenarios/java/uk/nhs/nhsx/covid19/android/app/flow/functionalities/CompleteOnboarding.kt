package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.DataAndPrivacyRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.HowAppWorksRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PermissionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PostCodeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.WelcomeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class CompleteOnboarding {

    private val welcomeRobot = WelcomeRobot()
    private val howAppWorksRobot = HowAppWorksRobot()
    private val dataAndPrivacyRobot = DataAndPrivacyRobot()
    private val postCodeRobot = PostCodeRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()
    private val permissionRobot = PermissionRobot()

    fun onboard() {
        waitFor { welcomeRobot.checkActivityIsDisplayed() }

        welcomeRobot.clickConfirmOnboarding()
        welcomeRobot.checkAgeConfirmationDialogIsDisplayed()
        welcomeRobot.clickConfirmAgePositive()

        howAppWorksRobot.checkActivityIsDisplayed()
        howAppWorksRobot.clickContinueOnboarding()

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
