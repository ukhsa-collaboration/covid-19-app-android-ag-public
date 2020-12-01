package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import com.jeroenmols.featureflag.framework.FeatureFlag.LOCAL_AUTHORITY
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
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

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun completePostCodeAndLocalAuthorityWithFeatureFlagEnabled() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(LOCAL_AUTHORITY)

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
    fun completePostCodeWithFeatureFlagDisabled() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(LOCAL_AUTHORITY)

        startTestActivity<PostCodeActivity>()

        postCodeRobot.checkActivityIsDisplayed()

        postCodeRobot.enterPostCode(postCode)

        postCodeRobot.clickContinue()

        waitFor { permissionRobot.checkActivityIsDisplayed() }
    }
}
