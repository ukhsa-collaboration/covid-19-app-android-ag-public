package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfo
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.RiskyContactIsolationOptOutRobot
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry
import uk.nhs.nhsx.covid19.android.app.remote.data.SupportedCountry.ENGLAND
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationAgeLimitRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationReviewRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationVaccinationStatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.RiskyContactIsolationAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class RiskyContact(
    private val espressoTest: EspressoTest
) {
    private val exposureNotificationRobot = ExposureNotificationRobot()
    private val ageLimitRobot = ExposureNotificationAgeLimitRobot()
    private val vaccinationStatusRobot = ExposureNotificationVaccinationStatusRobot()
    private val reviewRobot = ExposureNotificationReviewRobot(espressoTest.testAppContext)
    private val riskyContactIsolationAdviceRobot = RiskyContactIsolationAdviceRobot()
    private val riskyContactIsolationOptOutRobot = RiskyContactIsolationOptOutRobot()

    fun triggerViaCircuitBreaker(runBackgroundTasks: () -> Unit) {
        val exposureCircuitBreakerInfo = ExposureCircuitBreakerInfo(
            maximumRiskScore = 10.0,
            startOfDayMillis = espressoTest.testAppContext.clock.instant().toEpochMilli(),
            matchedKeyCount = 1,
            riskCalculationVersion = 2,
            exposureNotificationDate = espressoTest.testAppContext.clock.instant().toEpochMilli()
        )

        espressoTest.testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)

        runBackgroundTasks()
    }

    fun triggerViaBroadcastReceiver() {
        espressoTest.testAppContext.sendExposureStateUpdatedBroadcast()
    }

    fun acknowledgeIsolatingViaNotMinorNotVaccinatedForContactQuestionnaireJourney(alreadyIsolating: Boolean = false, country: SupportedCountry) {
        waitFor { exposureNotificationRobot.clickContinueButton() }

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        ageLimitRobot.clickYesButton()
        ageLimitRobot.clickContinueButton()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }
        vaccinationStatusRobot.clickDosesNoButton()
        if (country == ENGLAND)
            vaccinationStatusRobot.clickMedicallyExemptNoButton()
        vaccinationStatusRobot.clickClinicalTrialNoButton()
        vaccinationStatusRobot.clickContinueButton()

        clickSubmitButtonOnReviewScreen()

        clickBackToHomeOnIsolationAdviceScreen(alreadyIsolating)
    }

    fun acknowledgeIsolationViaOptOutMinor(alreadyIsolating: Boolean = false) {
        waitFor { exposureNotificationRobot.clickContinueButton() }

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        ageLimitRobot.clickNoButton()
        ageLimitRobot.clickContinueButton()

        clickSubmitButtonOnReviewScreen()

        clickBackToHomeOnIsolationAdviceScreen(alreadyIsolating)
    }

    fun acknowledgeIsolationViaOptOutFullyVaccinatedForContactQuestionnaireJourney(alreadyIsolating: Boolean = false) {
        waitFor { exposureNotificationRobot.clickContinueButton() }

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        ageLimitRobot.clickYesButton()
        ageLimitRobot.clickContinueButton()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }
        vaccinationStatusRobot.clickDosesYesButton()
        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }
        vaccinationStatusRobot.clickDateYesButton()
        vaccinationStatusRobot.clickContinueButton()

        clickSubmitButtonOnReviewScreen()

        clickBackToHomeOnIsolationAdviceScreen(alreadyIsolating)
    }

    fun acknowledgeNoIsolationForNewAdviceJourney() {
        waitFor { exposureNotificationRobot.clickContinueButton() }
        riskyContactIsolationOptOutRobot.checkActivityIsDisplayed()
        waitFor { riskyContactIsolationOptOutRobot.clickSecondaryButton() }
    }

    fun acknowledgeContinueIsolationForNewAdviceJourney() {
        waitFor { exposureNotificationRobot.clickContinueButton() }
        riskyContactIsolationAdviceRobot.checkActivityIsDisplayed()
        waitFor { riskyContactIsolationAdviceRobot.clickPrimaryBackToHome() }
    }

    private fun clickBackToHomeOnIsolationAdviceScreen(alreadyIsolating: Boolean) {
        riskyContactIsolationAdviceRobot.checkActivityIsDisplayed()
        if (alreadyIsolating) {
            riskyContactIsolationAdviceRobot.clickPrimaryBackToHome()
        } else {
            riskyContactIsolationAdviceRobot.clickSecondaryBackToHome()
        }
    }

    private fun clickSubmitButtonOnReviewScreen() {
        waitFor { reviewRobot.checkActivityIsDisplayed() }
        reviewRobot.clickSubmitButton()
    }
}
