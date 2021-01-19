package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import java.time.Instant
import java.util.concurrent.TimeUnit.SECONDS
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfo
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.AWAIT_AT_MOST_SECONDS
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.EncounterDetectionRobot

class RiskyContact(
    private val espressoTest: EspressoTest
) {
    private val encounterDetectionRobot = EncounterDetectionRobot()

    fun trigger(runBackgroundTasks: () -> Unit) {
        espressoTest.testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)

        runBackgroundTasks()

        espressoTest.waitFor { encounterDetectionRobot.clickIUnderstandButton() }

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            (espressoTest.testAppContext.getCurrentState() as Isolation).isContactCase()
        }
    }

    companion object {
        private val exposureCircuitBreakerInfo = ExposureCircuitBreakerInfo(
            maximumRiskScore = 10.0,
            startOfDayMillis = Instant.now().toEpochMilli(),
            matchedKeyCount = 1,
            riskCalculationVersion = 2,
            exposureNotificationDate = Instant.now().toEpochMilli()
        )
    }
}
