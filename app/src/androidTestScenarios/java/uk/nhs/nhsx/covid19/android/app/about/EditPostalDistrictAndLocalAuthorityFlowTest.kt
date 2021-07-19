package uk.nhs.nhsx.covid19.android.app.about

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.settings.SettingsActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EditPostalDistrictRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MyAreaRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SettingsRobot

class EditPostalDistrictAndLocalAuthorityFlowTest : EspressoTest() {

    private val editPostalDistrictRobot = EditPostalDistrictRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()
    private val settingsRobot = SettingsRobot()
    private val myAreaRobot = MyAreaRobot()

    private val postCode = "N12"
    private val localAuthorityName = "Barnet"

    @Test
    fun completePostCode() {
        startTestActivity<SettingsActivity>()

        settingsRobot.checkActivityIsDisplayed()

        settingsRobot.clickMyAreaSetting()

        myAreaRobot.checkActivityIsDisplayed()

        myAreaRobot.clickEditButton()

        editPostalDistrictRobot.checkActivityIsDisplayed()

        editPostalDistrictRobot.enterPostDistrictCode(postCode)

        editPostalDistrictRobot.clickSavePostDistrictCode()

        waitFor { localAuthorityRobot.checkActivityIsDisplayed() }

        localAuthorityRobot.checkSingleAuthorityIsDisplayed(postCode, localAuthorityName)

        localAuthorityRobot.clickConfirm()

        waitFor { myAreaRobot.checkActivityIsDisplayed() }
    }
}
