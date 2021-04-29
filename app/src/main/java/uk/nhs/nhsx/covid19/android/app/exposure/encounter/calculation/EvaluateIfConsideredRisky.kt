package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class EvaluateIfConsideredRisky @Inject constructor(
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock
) {

    operator fun invoke(
        exposureWindow: ExposureWindow,
        calculatedRisk: Double,
        riskThreshold: Double
    ): Boolean {
        Timber.d("ExposureWindowWithRisk: with risk $calculatedRisk isRecentExposure: ${exposureWindow.isRecentExposure()} and isAfterPotentialDailyContactTestingOptIn: ${exposureWindow.isAfterPotentialDailyContactTestingOptIn()}")
        return exposureWindow.isConsideredRisky(calculatedRisk, riskThreshold)
    }

    private fun ExposureWindow.isConsideredRisky(calculatedRisk: Double, riskThreshold: Double): Boolean {
        return isRecentExposure() &&
            isAfterPotentialDailyContactTestingOptIn() &&
            calculatedRisk >= riskThreshold
    }

    private fun ExposureWindow.isRecentExposure(): Boolean {
        val contactCaseIsolationDuration = isolationConfigurationProvider.durationDays.contactCase.toLong()
        val oldestPossibleContactCaseIsolationDate =
            LocalDate.now(clock).minusDays(contactCaseIsolationDuration).atStartOfDay()
        val encounterDate: LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateMillisSinceEpoch), ZoneOffset.UTC)
        return encounterDate.isEqualOrAfter(oldestPossibleContactCaseIsolationDate)
    }

    private fun ExposureWindow.isAfterPotentialDailyContactTestingOptIn(): Boolean {
        return when (val isolationState = isolationStateMachine.readState()) {
            is Default -> isolationState.previousIsolation?.contactCase?.dailyContactTestingOptInDate?.let { optInDate ->
                dateMillisSinceEpoch >= optInDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            } ?: true
            is Isolation -> true
        }
    }
}
