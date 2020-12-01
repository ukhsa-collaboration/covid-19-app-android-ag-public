package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlag.LOCAL_AUTHORITY
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot

class ExistingUserLocalAuthorityFlowTest : EspressoTest() {

    private val localAuthorityInformationRobot = LocalAuthorityInformationRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()
    private val statusRobot = StatusRobot()

    private val postCode = "N12"
    private val localAuthorityName = "Barnet"

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun setLocalAuthorityWithFeatureFlagEnabled() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(LOCAL_AUTHORITY)

        testAppContext.setPostCode(postCode)
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)
        testAppContext.setLocalAuthority(null)

        startTestActivity<MainActivity>()

        localAuthorityInformationRobot.checkActivityIsDisplayed()

        localAuthorityInformationRobot.clickContinue()

        localAuthorityRobot.checkActivityIsDisplayed()

        waitFor { localAuthorityRobot.checkSingleAuthorityIsDisplayed(postCode, localAuthorityName) }

        localAuthorityRobot.clickConfirm()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun skipLocalAuthorityWithFeatureFlagEnabled() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(LOCAL_AUTHORITY)

        testAppContext.setPostCode(postCode)
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)
        testAppContext.setLocalAuthority("1")

        startTestActivity<MainActivity>()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun skipLocalAuthorityWithFeatureFlagDisabled() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        testAppContext.setPostCode(postCode)
        testAppContext.setOnboardingCompleted(true)
        testAppContext.setPolicyUpdateAccepted(true)

        startTestActivity<MainActivity>()

        statusRobot.checkActivityIsDisplayed()
    }
}
