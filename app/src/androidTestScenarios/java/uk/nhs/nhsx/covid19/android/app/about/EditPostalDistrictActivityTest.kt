package uk.nhs.nhsx.covid19.android.app.about

import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EditPostalDistrictRobot
import java.util.concurrent.TimeUnit.SECONDS

class EditPostalDistrictActivityTest : EspressoTest() {

    private val editPostalDistrictRobot = EditPostalDistrictRobot()

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

        editPostalDistrictRobot.checkInvalidPostDistrictErrorIsDisplayed()
    }

    @Test
    fun clickSavePostDistrictCodeWhenCodeIsValid_finishesActivity() = notReported {
        val editPostalDistrictActivity = startTestActivity<EditPostalDistrictActivity>()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.enterPostDistrictCode(validPostDistrictCode)

        editPostalDistrictRobot.clickSavePostDistrictCode()

        await.atMost(10, SECONDS) until {
            editPostalDistrictActivity?.isDestroyed ?: false
        }
    }

    @Test
    fun userTriesToChangePostCodeToNotSupported_errorIsShown() = notReported {
        startTestActivity<EditPostalDistrictActivity>()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.enterPostDistrictCode("BT1")

        editPostalDistrictRobot.clickSavePostDistrictCode()

        editPostalDistrictRobot.checkErrorContainerForNotSupportedPostCodeIsDisplayed()
    }
}
