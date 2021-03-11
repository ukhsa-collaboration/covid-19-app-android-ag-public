package uk.nhs.nhsx.covid19.android.app.about

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
    fun clickSavePostDistrictCodeWhenCodeIsValid_opensLocalAuthorityActivity() = notReported {
        startTestActivity<EditPostalDistrictActivity>()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.enterPostDistrictCode(validPostDistrictCode)

        editPostalDistrictRobot.clickSavePostDistrictCode()

        localAuthorityRobot.checkActivityIsDisplayed()
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
