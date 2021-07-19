package uk.nhs.nhsx.covid19.android.app.settings.languages

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LanguagesRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SettingsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguagesActivityTest : EspressoTest() {

    private val languagesRobot = LanguagesRobot()
    private val settingsRobot = SettingsRobot()
    private val statusRobot = StatusRobot()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun startActivityWithoutLocale_shouldDisplayDefaultEnglishLanguageSelected() {
        testAppContext.setLocale(null)

        startTestActivity<LanguagesActivity>()

        languagesRobot.checkActivityIsDisplayed()
        languagesRobot.checkSystemLanguageNativeNameMatches(SupportedLanguage.ENGLISH.nativeLanguageName)
        languagesRobot.checkSystemLanguageTranslatedNameMatches(SupportedLanguage.ENGLISH.languageName)
        languagesRobot.checkSystemLanguageIsChecked()
        languagesRobot.checkNoOtherLanguageIsChecked()
    }

    @Test
    fun startActivityWithWelshLocale_shouldDisplayWelshLanguageSelected() {
        testAppContext.setLocale("cy")

        startTestActivity<LanguagesActivity>()

        languagesRobot.checkActivityIsDisplayed()
        languagesRobot.checkSystemLanguageIsNotChecked()
        languagesRobot.checkOtherLanguageIsChecked(SupportedLanguage.WELSH.languageName)
    }

    @Test
    fun selectUserLanguage_clickConfirmOnConfirmationDialog() {
        testAppContext.setLocale("en")

        startTestActivity<LanguagesActivity>()

        languagesRobot.checkActivityIsDisplayed()

        languagesRobot.selectLanguage(SupportedLanguage.ARABIC.languageName)

        languagesRobot.checkConfirmationDialogIsDisplayed(context.getString(SupportedLanguage.ARABIC.languageName))

        languagesRobot.clickConfirmPositive()
    }

    @Test
    fun selectSystemLanguage_clickConfirmOnConfirmationDialog() {
        testAppContext.setLocale("cy")

        startTestActivity<LanguagesActivity>()

        languagesRobot.checkActivityIsDisplayed()

        languagesRobot.selectSystemLanguage()

        languagesRobot.checkConfirmationDialogIsDisplayed(context.getString(SupportedLanguage.ENGLISH.languageName))

        languagesRobot.clickConfirmPositive()
    }

    @Test
    fun selectLanguage_clickCancelOnConfirmationDialog_shouldNotChangeLanguage() {
        testAppContext.setLocale("en")

        startTestActivity<LanguagesActivity>()

        languagesRobot.checkActivityIsDisplayed()

        languagesRobot.selectLanguage(SupportedLanguage.ARABIC.languageName)

        languagesRobot.checkConfirmationDialogIsDisplayed(context.getString(SupportedLanguage.ARABIC.languageName))

        languagesRobot.clickConfirmNegative()

        languagesRobot.checkOtherLanguageIsChecked(SupportedLanguage.ENGLISH.languageName)
        languagesRobot.checkSystemLanguageIsNotChecked()
    }

    @Test
    fun changeLanguage_canReloadCurrentActivityWithNewLanguageSet() {
        testAppContext.setLocale("en")

        startTestActivity<LanguagesActivity>()

        languagesRobot.checkActivityIsDisplayed()
        languagesRobot.selectLanguage(SupportedLanguage.WELSH.languageName)
        languagesRobot.clickConfirmPositive()

        waitFor { languagesRobot.checkOtherLanguageIsChecked(SupportedLanguage.WELSH.languageName) }
    }

    @Test
    fun changeLanguage_canReloadAllActivityInBackstackWithNewLanguageSet() {
        testAppContext.setLocale("en")

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.clickSettings()

        settingsRobot.checkActivityIsDisplayed()

        settingsRobot.clickLanguageSetting()

        languagesRobot.checkActivityIsDisplayed()

        languagesRobot.selectLanguage(SupportedLanguage.WELSH.languageName)
        languagesRobot.clickConfirmPositive()

        waitFor { languagesRobot.checkOtherLanguageIsChecked(SupportedLanguage.WELSH.languageName) }

        testAppContext.device.pressBack()

        settingsRobot.checkLanguageSubtitleMatches(SupportedLanguage.WELSH.languageName)

        testAppContext.device.pressBack()

        statusRobot.checkVenueOptionIsTranslatedTo("Mewngofnodi i leoliad")
    }

    @Test
    fun startWithSystemLanguage_changeLanguage_switchBackToSystemLanguage_shouldDisplayDefaultEnglishLanguageSelected() {
        testAppContext.setLocale(null)

        startTestActivity<LanguagesActivity>()

        languagesRobot.checkActivityIsDisplayed()
        languagesRobot.checkSystemLanguageNativeNameMatches(SupportedLanguage.ENGLISH.nativeLanguageName)
        languagesRobot.checkSystemLanguageTranslatedNameMatches(SupportedLanguage.ENGLISH.languageName)
        languagesRobot.checkSystemLanguageIsChecked()
        languagesRobot.checkNoOtherLanguageIsChecked()

        assertNull(testAppContext.getApplicationLocaleProvider().languageCode)
        assertEquals(expected = Locale("en"), testAppContext.getApplicationLocaleProvider().getLocale())

        languagesRobot.selectLanguage(SupportedLanguage.POLISH.languageName)
        languagesRobot.clickConfirmPositive()
        waitFor { languagesRobot.checkOtherLanguageIsChecked(SupportedLanguage.POLISH.languageName) }
        languagesRobot.checkSystemLanguageIsNotChecked()

        assertEquals(expected = Locale("pl"), testAppContext.getApplicationLocaleProvider().getLocale())

        languagesRobot.selectSystemLanguage()
        languagesRobot.clickConfirmPositive()
        waitFor { languagesRobot.checkNoOtherLanguageIsChecked() }
        languagesRobot.checkSystemLanguageIsChecked()

        assertNull(testAppContext.getApplicationLocaleProvider().languageCode)
        assertEquals(expected = Locale("en"), testAppContext.getApplicationLocaleProvider().getLocale())
    }
}
