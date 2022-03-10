package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import com.google.android.gms.nearby.exposurenotification.ExposureWindow
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.state.GetLatestConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class EvaluateIfConsideredRisky @Inject constructor(
    private val isolationStateMachine: IsolationStateMachine,
    private val getLatestConfiguration: GetLatestConfiguration,
    private val clock: Clock
) {

    operator fun invoke(
        exposureWindow: ExposureWindow,
        calculatedRisk: Double,
        riskThreshold: Double
    ): Boolean {
        Timber.d("ExposureWindowWithRisk: with risk $calculatedRisk isRecentExposure: ${exposureWindow.isRecentExposure()} and isAfterPotentialContactIsolationOptOut: ${exposureWindow.isAfterPotentialContactIsolationOptOut()}")
        return exposureWindow.isConsideredRisky(calculatedRisk, riskThreshold)
    }

    private fun ExposureWindow.isConsideredRisky(calculatedRisk: Double, riskThreshold: Double): Boolean {
        return isRecentExposure() &&
                isAfterPotentialContactIsolationOptOut() &&
                calculatedRisk >= riskThreshold
    }

    private fun ExposureWindow.isRecentExposure(): Boolean {
        val contactCaseIsolationDuration = getLatestConfiguration().contactCase.toLong()
        val oldestPossibleContactCaseIsolationDate =
            LocalDate.now(clock).minusDays(contactCaseIsolationDuration).atStartOfDay()
        val encounterDate: LocalDateTime =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(dateMillisSinceEpoch), ZoneOffset.UTC)
        return encounterDate.isEqualOrAfter(oldestPossibleContactCaseIsolationDate)
    }

    private fun ExposureWindow.isAfterPotentialContactIsolationOptOut(): Boolean =
        isolationStateMachine.readState().contact?.optOutOfContactIsolation?.date?.let { optOutDate ->
            val exposureDate = Instant.ofEpochMilli(dateMillisSinceEpoch).toLocalDate(clock.zone)
            exposureDate > optOutDate
        } ?: true
}
