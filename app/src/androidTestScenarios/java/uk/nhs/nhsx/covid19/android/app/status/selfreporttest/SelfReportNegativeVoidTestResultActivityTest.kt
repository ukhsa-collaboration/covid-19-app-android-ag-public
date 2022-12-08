package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.NegativeVoidTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper

class SelfReportNegativeVoidTestResultActivityTest : EspressoTest(), LocalAuthoritySetupHelper {

    private val negativeVoidTestResultRobot = NegativeVoidTestResultRobot()
    private val statusRobot = StatusRobot()
    private val testTypeRobot = TestTypeRobot()

    @Test
    fun showEligibilityTextAndUrlOnEnglandPostcode() {
        givenLocalAuthorityIsInEngland()

        startTestActivity<SelfReportNegativeVoidTestResultActivity>()

        waitFor { negativeVoidTestResultRobot.checkActivityIsDisplayed() }

        negativeVoidTestResultRobot.checkEligibilityParagraphAndUrlIsVisible(true)
    }

    @Test
    fun doNotShowEligibilityTextAndUrlOnWalesPostcode() {
        givenLocalAuthorityIsInWales()

        startTestActivity<SelfReportNegativeVoidTestResultActivity>()

        waitFor { negativeVoidTestResultRobot.checkActivityIsDisplayed() }

        negativeVoidTestResultRobot.checkEligibilityParagraphAndUrlIsVisible(false)
    }

    @Test
    fun showStatusPageWhenClickingBackToHome() {
        startTestActivity<SelfReportNegativeVoidTestResultActivity>()

        waitFor { negativeVoidTestResultRobot.checkActivityIsDisplayed() }

        negativeVoidTestResultRobot.clickBackToHome()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun showTestTypeActivityAfterPressingBack() {
        startTestActivity<TestTypeActivity>()

        waitFor { testTypeRobot.checkActivityIsDisplayed() }

        testTypeRobot.clickNegativeButton()

        waitFor { testTypeRobot.checkNegativeIsSelected() }

        testTypeRobot.clickContinueButton()

        waitFor { negativeVoidTestResultRobot.checkActivityIsDisplayed() }

        testAppContext.device.pressBack()

        waitFor { testTypeRobot.checkActivityIsDisplayed() }
    }
}
