package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class ExistingUserLocalAuthorityFlowTest : EspressoTest() {

    private val localAuthorityInformationRobot = LocalAuthorityInformationRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()
    private val statusRobot = StatusRobot()

    private val postCode = "N12"
    private val localAuthorityName = "Barnet"

    @Test
    @RetryFlakyTest
    fun setLocalAuthority() {
        testAppContext.setPostCode(postCode)
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)
        testAppContext.setLocalAuthority(null)

        startTestActivity<MainActivity>()

        waitFor { localAuthorityInformationRobot.checkActivityIsDisplayed() }

        localAuthorityInformationRobot.clickContinue()

        localAuthorityRobot.checkActivityIsDisplayed()

        waitFor { localAuthorityRobot.checkSingleAuthorityIsDisplayed(postCode, localAuthorityName) }

        localAuthorityRobot.clickConfirm()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun skipLocalAuthority() {
        testAppContext.setPostCode(postCode)
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)

        testAppContext.setLocalAuthority("E07000240")

        startTestActivity<MainActivity>()

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }
}
