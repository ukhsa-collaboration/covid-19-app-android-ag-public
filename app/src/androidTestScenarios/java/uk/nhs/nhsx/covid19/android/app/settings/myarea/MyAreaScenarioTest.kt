package uk.nhs.nhsx.covid19.android.app.settings.myarea

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.settings.SettingsActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EditPostalDistrictRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MyAreaRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SettingsRobot

class MyAreaScenarioTest : EspressoTest() {

    private val settingsRobot = SettingsRobot()
    private val myAreaRobot = MyAreaRobot()
    private val editPostalDistrictRobot = EditPostalDistrictRobot()
    private val localAuthorityRobot = LocalAuthorityRobot()

    @Test
    fun whenCompletingEnterPostCodeAndLocalAuthorityJourney_returnToSettings_thenConfirmUpdatedMyAreaDetails() {
        testAppContext.setPostCode(null)
        testAppContext.setLocalAuthority(null)

        startTestActivity<SettingsActivity>()

        settingsRobot.checkActivityIsDisplayed()
        settingsRobot.clickMyAreaSetting()

        myAreaRobot.checkActivityIsDisplayed()
        myAreaRobot.checkPostCodeIsEmpty()
        myAreaRobot.checkLocalAuthorityIsEmpty()
        myAreaRobot.clickEditButton()

        editPostalDistrictRobot.checkActivityIsDisplayed()
        editPostalDistrictRobot.enterPostDistrictCode("SE1")
        editPostalDistrictRobot.clickSavePostDistrictCode()

        localAuthorityRobot.checkActivityIsDisplayed()
        localAuthorityRobot.checkMultipleAuthoritiesAreDisplayed("SE1")
        localAuthorityRobot.selectLocalAuthority("Southwark")
        localAuthorityRobot.clickConfirm()

        myAreaRobot.checkActivityIsDisplayed()
        myAreaRobot.checkPostCodeEquals("SE1")
        myAreaRobot.checkLocalAuthorityEquals("Southwark")
    }
}
