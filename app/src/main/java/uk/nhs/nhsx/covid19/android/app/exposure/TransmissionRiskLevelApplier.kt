package uk.nhs.nhsx.covid19.android.app.exposure

import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import java.lang.Integer.max
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

class TransmissionRiskLevelApplier @Inject constructor(
    private val stateMachine: IsolationStateMachine
) {
    fun applyTransmissionRiskLevels(
        keys: List<NHSTemporaryExposureKey>,
        onsetDateBasedOnTestEndDate: LocalDate
    ): List<NHSTemporaryExposureKey> {
        return keys
            .sortedByDescending { it.rollingStartNumber }
            .map { key ->
                val daysFromOnset =
                    ChronoUnit.DAYS.between(getOnsetDate(onsetDateBasedOnTestEndDate), key.date())
                        .toInt()
                key.copy(
                    transmissionRiskLevel = calculateTransmissionRiskLevel(daysFromOnset),
                    daysSinceOnsetOfSymptoms = daysFromOnset
                )
            }
    }

    private fun calculateTransmissionRiskLevel(daysFromOnset: Int): Int {
        if (daysFromOnset < PRIOR_DAYS_THRESHOLD) {
            return 0
        }
        val transmissionRiskLevel = MAX_TRANSMISSION_RISK_LEVEL - abs(daysFromOnset)
        return max(MIN_TRANSMISSION_RISK_LEVEL, transmissionRiskLevel)
    }

    private fun getOnsetDate(onsetDateBasedOnTestEndDate: LocalDate): LocalDate {
        val latestIsolation = when (val state = stateMachine.readState()) {
            is Isolation -> state
            is Default -> state.previousIsolation
        }
        return latestIsolation?.indexCase?.symptomsOnsetDate ?: onsetDateBasedOnTestEndDate
    }

    private fun NHSTemporaryExposureKey.date(): LocalDate {
        val timeSince1970 = rollingStartNumber * Duration.ofMinutes(10).toMillis()
        return Instant.ofEpochMilli(timeSince1970)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
    }
}

const val MAX_TRANSMISSION_RISK_LEVEL = 7
private const val MIN_TRANSMISSION_RISK_LEVEL = 0

// COV-3804: We don't want keys from before 2 days prior to onset of symptoms
private const val PRIOR_DAYS_THRESHOLD = -2
