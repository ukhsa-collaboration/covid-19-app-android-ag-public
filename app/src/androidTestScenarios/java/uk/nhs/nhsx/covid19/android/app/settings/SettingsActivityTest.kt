package uk.nhs.nhsx.covid19.android.app.settings

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LanguagesRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SettingsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.UserDataRobot

class SettingsActivityTest : EspressoTest() {

    private val settingsRobot = SettingsRobot()
    private val languagesRobot = LanguagesRobot()
    private val userDataRobot = UserDataRobot()

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
    fun clickManageMyDataSetting_shouldNavigateToManageMyDataActivity() = notReported {
        testAppContext.setPostCode("AL1")

        startTestActivity<SettingsActivity>()

        settingsRobot.checkActivityIsDisplayed()

        settingsRobot.clickManageMyDataSetting()

        userDataRobot.checkActivityIsDisplayed()
    }
}
