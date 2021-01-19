package uk.nhs.nhsx.covid19.android.app.settings

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LanguagesRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SettingsRobot

class SettingsActivityTest : EspressoTest() {

    private val settingsRobot = SettingsRobot()
    private val languagesRobot = LanguagesRobot()

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
}
