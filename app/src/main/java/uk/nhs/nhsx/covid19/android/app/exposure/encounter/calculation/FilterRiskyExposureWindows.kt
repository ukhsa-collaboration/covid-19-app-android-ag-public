package uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

class FilterRiskyExposureWindows @Inject constructor(
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val isolationStateMachine: IsolationStateMachine,
    private val clock: Clock,
) {

    operator fun invoke(
        exposureWindowsWithRisk: List<ExposureWindowWithRisk>,
        riskThreshold: Double
    ): List<ExposureWindowWithRisk> {
        return exposureWindowsWithRisk
            .onEach {
                Timber.d("ExposureWindowWithRisk: with risk ${it.calculatedRisk} isRecentExposure: ${it.isRecentExposure()} and isAfterPotentialDailyContactTestingOptIn: ${it.isAfterPotentialDailyContactTestingOptIn()}")
            }
            .filter {
                it.isRecentExposure() &&
                    it.isAfterPotentialDailyContactTestingOptIn() &&
                    it.isAboveThreshold(riskThreshold)
            }
    }

    private fun ExposureWindowWithRisk.isRecentExposure(): Boolean {
        val contactCaseIsolationDuration = isolationConfigurationProvider.durationDays.contactCase.toLong()
        val oldestPossibleContactCaseIsolationDate =
            LocalDate.now(clock).minusDays(contactCaseIsolationDuration).atStartOfDay()
        return encounterDate.isEqualOrAfter(oldestPossibleContactCaseIsolationDate)
    }

    private fun ExposureWindowWithRisk.isAfterPotentialDailyContactTestingOptIn(): Boolean {
        return when (val isolationState = isolationStateMachine.readState()) {
            is Default -> isolationState.previousIsolation?.contactCase?.dailyContactTestingOptInDate?.let { optInDate ->
                this.startOfDayMillis >= optInDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            } ?: true
            is Isolation -> true
        }
    }
}
