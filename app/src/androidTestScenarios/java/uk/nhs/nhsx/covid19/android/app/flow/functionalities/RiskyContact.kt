package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfo
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationAgeLimitRobot
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
    private val riskyContactIsolationAdviceRobot = RiskyContactIsolationAdviceRobot()

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

    fun acknowledgeIsolatingViaNotMinorNotVaccinated(alreadyIsolating: Boolean = false) {
        waitFor { exposureNotificationRobot.clickContinueButton() }

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        ageLimitRobot.clickYesButton()
        ageLimitRobot.clickContinueButton()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }
        vaccinationStatusRobot.clickDosesNoButton()
        vaccinationStatusRobot.clickContinueButton()

        waitFor { riskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }
        if (alreadyIsolating) {
            riskyContactIsolationAdviceRobot.clickPrimaryBackToHome()
        } else {
            riskyContactIsolationAdviceRobot.clickSecondaryBackToHome()
        }
    }

    fun acknowledgeIsolationViaOptOutMinor(alreadyIsolating: Boolean = false) {
        waitFor { exposureNotificationRobot.clickContinueButton() }

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        ageLimitRobot.clickNoButton()
        ageLimitRobot.clickContinueButton()

        waitFor { riskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }
        if (alreadyIsolating) {
            riskyContactIsolationAdviceRobot.clickPrimaryBackToHome()
        } else {
            riskyContactIsolationAdviceRobot.clickSecondaryBackToHome()
        }
    }

    fun acknowledgeIsolationViaOptOutFullyVaccinated(alreadyIsolating: Boolean = false) {
        waitFor { exposureNotificationRobot.clickContinueButton() }

        waitFor { ageLimitRobot.checkActivityIsDisplayed() }
        ageLimitRobot.clickYesButton()
        ageLimitRobot.clickContinueButton()

        waitFor { vaccinationStatusRobot.checkActivityIsDisplayed() }
        vaccinationStatusRobot.clickDosesYesButton()
        waitFor { vaccinationStatusRobot.checkDosesDateQuestionContainerDisplayed(true) }
        vaccinationStatusRobot.clickDateYesButton()
        vaccinationStatusRobot.clickContinueButton()

        waitFor { riskyContactIsolationAdviceRobot.checkActivityIsDisplayed() }
        if (alreadyIsolating) {
            riskyContactIsolationAdviceRobot.clickPrimaryBackToHome()
        } else {
            riskyContactIsolationAdviceRobot.clickSecondaryBackToHome()
        }
    }
}
