package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfo
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testhelpers.AWAIT_AT_MOST_SECONDS
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EncounterDetectionRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor
import java.util.concurrent.TimeUnit.SECONDS

class RiskyContact(
    private val espressoTest: EspressoTest
) {
    private val encounterDetectionRobot = EncounterDetectionRobot()

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

        waitFor { encounterDetectionRobot.clickIUnderstandButton() }
    }

    fun acknowledge() {
        espressoTest.waitFor { encounterDetectionRobot.clickIUnderstandButton() }

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            (espressoTest.testAppContext.getCurrentLogicalState() as PossiblyIsolating)
                .isActiveContactCase(espressoTest.testAppContext.clock)
        }
    }
}
