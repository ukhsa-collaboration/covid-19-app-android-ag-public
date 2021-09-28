package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PermissionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PostCodeRobot

class PostCodeAndLocalAuthorityFlowTest : EspressoTest() {

    private val postCodeRobot = PostCodeRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()
    private val permissionRobot = PermissionRobot()

    private val postCode = "N12"
    private val localAuthorityName = "Barnet"

    @Test
    fun completePostCode() {
        startTestActivity<PostCodeActivity>()

        postCodeRobot.checkActivityIsDisplayed()

        postCodeRobot.enterPostCode(postCode)

        postCodeRobot.clickContinue()

        localAuthorityRobot.checkActivityIsDisplayed()

        waitFor { localAuthorityRobot.checkSingleAuthorityIsDisplayed(postCode, localAuthorityName) }

        localAuthorityRobot.clickConfirm()

        waitFor { permissionRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun clickContinue_errorIsVisible_completePostcodeAndPressBack_errorIsNotVisible() {
        startTestActivity<PostCodeActivity>()

        postCodeRobot.checkActivityIsDisplayed()

        waitFor { postCodeRobot.checkContinueButtonIsDisplayed() }

        postCodeRobot.clickContinue()

        postCodeRobot.checkErrorTitleIsDisplayed()

        postCodeRobot.enterPostCode(postCode)

        waitFor { postCodeRobot.checkEditTextIs(postCode) }

        waitFor { postCodeRobot.checkContinueButtonIsDisplayed() }

        waitFor { postCodeRobot.clickContinue() }

        waitFor { localAuthorityRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        postCodeRobot.checkActivityIsDisplayed()

        postCodeRobot.checkErrorContainerIsNotDisplayed()
    }
}
