package uk.nhs.nhsx.covid19.android.app.settings

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.DataAndPrivacyRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LanguagesRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MyAreaRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MyDataRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PermissionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PostCodeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SettingsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueHistoryRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.WelcomeRobot

class SettingsActivityTest : EspressoTest() {

    private val settingsRobot = SettingsRobot()
    private val languagesRobot = LanguagesRobot()
    private val myDataRobot = MyDataRobot()
    private val venueHistoryRobot = VenueHistoryRobot(context = testAppContext.app)
    private val myAreaRobot = MyAreaRobot()

    @Test
    fun startActivityWithoutLocale_shouldDisplayEnglishLanguage() = notReported {
        testAppContext.setLocale(null)

        startTestActivity<SettingsActivity>()

        settingsRobot.checkActivityIsDisplayed()

        settingsRobot.checkLanguageSubtitleMatches(R.string.english)
    }

    @Test
    fun startActivityWithWelshLocale_shouldDisplayWelshLanguage() = notReported {
        testAppContext.setLocale("cy")

        startTestActivity<SettingsActivity>()

        settingsRobot.checkActivityIsDisplayed()

        settingsRobot.checkLanguageSubtitleMatches(R.string.welsh)
    }

    @Test
    fun clickLanguagesSetting_shouldNavigateToLanguagesActivity() = notReported {
        startTestActivity<SettingsActivity>()

        settingsRobot.checkActivityIsDisplayed()

        settingsRobot.clickLanguageSetting()

        languagesRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickManageMyDataSetting_HasSettingsOptions() = notReported {
        testAppContext.setPostCode("AL1")

        startTestActivity<SettingsActivity>()

        settingsRobot.apply {
            checkActivityIsDisplayed()

            hasLanguageSetting()
            hasMyAreaSetting()
            hasMyDataSetting()
            hasVenueHistorySetting()
            hasDeleteDataOption()
        }
    }

    @Test
    fun clickMyDataSetting_shouldNavigateToMyDataActivity() = notReported {
        testAppContext.setPostCode("AL1")

        startTestActivity<SettingsActivity>()

        settingsRobot.checkActivityIsDisplayed()

        settingsRobot.clickMyDataSetting()

        myDataRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickMyDataSetting_shouldDeleteData() = notReported {
        val welcomeRobot = WelcomeRobot()
        val dataAndPrivacyRobot = DataAndPrivacyRobot()
        val postCodeRobot = PostCodeRobot()
        val localAuthorityRobot = LocalAuthorityRobot()
        val statusRobot = StatusRobot()
        val permissionRobot = PermissionRobot()

        testAppContext.setPostCode("AL1")

        startTestActivity<SettingsActivity>()

        settingsRobot.checkActivityIsDisplayed()

        settingsRobot.clickDeleteSetting()

        settingsRobot.userClicksDeleteDataOnDialog()

        waitFor { welcomeRobot.isActivityDisplayed() }

        welcomeRobot.checkActivityIsDisplayed()

        welcomeRobot.clickConfirmOnboarding()

        welcomeRobot.checkAgeConfirmationDialogIsDisplayed()

        welcomeRobot.clickConfirmAgePositive()

        dataAndPrivacyRobot.checkActivityIsDisplayed()

        dataAndPrivacyRobot.clickConfirmOnboarding()

        postCodeRobot.checkActivityIsDisplayed()

        postCodeRobot.enterPostCode("N12")

        postCodeRobot.clickContinue()

        waitFor { localAuthorityRobot.checkActivityIsDisplayed() }

        localAuthorityRobot.clickConfirm()

        waitFor { permissionRobot.checkActivityIsDisplayed() }

        permissionRobot.clickEnablePermissions()

        statusRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickMyAreaSetting_shouldNavigateToMyAreaActivity() = notReported {
        testAppContext.setPostCode("AL1")

        startTestActivity<SettingsActivity>()

        settingsRobot.checkActivityIsDisplayed()

        settingsRobot.clickMyAreaSetting()

        myAreaRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickVenueHistorySetting_shouldNavigateToVenusHistoryActivity() = notReported {
        testAppContext.setPostCode("AL1")

        startTestActivity<SettingsActivity>()

        settingsRobot.checkActivityIsDisplayed()

        settingsRobot.clickVenueHistorySetting()

        venueHistoryRobot.checkActivityIsDisplayed()
    }
}
