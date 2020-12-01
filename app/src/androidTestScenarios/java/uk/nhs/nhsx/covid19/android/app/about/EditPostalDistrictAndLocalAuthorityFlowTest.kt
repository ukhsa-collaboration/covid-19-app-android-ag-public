package uk.nhs.nhsx.covid19.android.app.about

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EditPostalDistrictRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.UserDataRobot

class EditPostalDistrictAndLocalAuthorityFlowTest : EspressoTest() {

    private val editPostalDistrictRobot = EditPostalDistrictRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()
    private val userDataRobot = UserDataRobot()

    private val postCode = "N12"
    private val localAuthorityName = "Barnet"

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun completePostCodeAndLocalAuthorityWithFeatureFlagEnabled() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        userDataRobot.userClicksEditPostalDistrict()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.enterPostDistrictCode(postCode)

        editPostalDistrictRobot.clickSavePostDistrictCode()

        localAuthorityRobot.checkActivityIsDisplayed()

        waitFor { localAuthorityRobot.checkSingleAuthorityIsDisplayed(postCode, localAuthorityName) }

        localAuthorityRobot.clickConfirm()

        waitFor { userDataRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun completePostCodeWithFeatureFlagDisabled() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        startTestActivity<UserDataActivity>()

        userDataRobot.checkActivityIsDisplayed()

        userDataRobot.userClicksEditPostalDistrict()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.enterPostDistrictCode(postCode)

        editPostalDistrictRobot.clickSavePostDistrictCode()

        waitFor { userDataRobot.checkActivityIsDisplayed() }
    }
}
