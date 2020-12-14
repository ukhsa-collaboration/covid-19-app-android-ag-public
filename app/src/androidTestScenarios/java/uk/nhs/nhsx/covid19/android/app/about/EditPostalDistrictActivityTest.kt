package uk.nhs.nhsx.covid19.android.app.about

import com.jeroenmols.featureflag.framework.FeatureFlag
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EditPostalDistrictRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot

class EditPostalDistrictActivityTest : EspressoTest() {

    private val editPostalDistrictRobot = EditPostalDistrictRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()

    private val invalidPostDistrictCode = "INV"
    private val validPostDistrictCode = "CM2"

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
    }

    @Test
    fun editPostalDistrictScreenShows() = notReported {
        startTestActivity<EditPostalDistrictActivity>()

        editPostalDistrictRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickSavePostDistrictCodeWhenCodeIsEmpty_showsErrorMessage() = notReported {
        startTestActivity<EditPostalDistrictActivity>()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.clickSavePostDistrictCode()

        waitFor { editPostalDistrictRobot.checkInvalidPostDistrictErrorIsDisplayed() }
    }

    @Test
    fun clickSavePostDistrictCodeWhenCodeIsInvalid_showsErrorMessage() = notReported {
        startTestActivity<EditPostalDistrictActivity>()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.enterPostDistrictCode(invalidPostDistrictCode)

        editPostalDistrictRobot.clickSavePostDistrictCode()

        waitFor { editPostalDistrictRobot.checkInvalidPostDistrictErrorIsDisplayed() }
    }

    @Test
    fun clickSavePostDistrictCodeWhenCodeIsValidAndLocalAuthorityFeatureFlagIsEnabled_opensLocalAuthorityActivity() = notReported {
        FeatureFlagTestHelper.enableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        startTestActivity<EditPostalDistrictActivity>()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.enterPostDistrictCode(validPostDistrictCode)

        editPostalDistrictRobot.clickSavePostDistrictCode()

        localAuthorityRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickSavePostDistrictCodeWhenCodeIsValidAndLocalAuthorityFeatureFlagIsDisabled_finishesActivity() = notReported {
        FeatureFlagTestHelper.disableFeatureFlag(FeatureFlag.LOCAL_AUTHORITY)

        val editPostalDistrictActivity = startTestActivity<EditPostalDistrictActivity>()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.enterPostDistrictCode(validPostDistrictCode)

        editPostalDistrictRobot.clickSavePostDistrictCode()

        waitFor { assertTrue(editPostalDistrictActivity?.isDestroyed ?: false) }
    }

    @Test
    fun userTriesToChangePostCodeToNotSupported_errorIsShown() = notReported {
        startTestActivity<EditPostalDistrictActivity>()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.enterPostDistrictCode("BT1")

        editPostalDistrictRobot.clickSavePostDistrictCode()

        editPostalDistrictRobot.checkErrorContainerForNotSupportedPostCodeIsDisplayed()

        editPostalDistrictRobot.checkErrorTitleForNotSupportedPostCodeIsDisplayed()
    }
}
