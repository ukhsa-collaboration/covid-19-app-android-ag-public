package uk.nhs.nhsx.covid19.android.app.exposure

import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CalculateKeySubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.KeySharingInfo
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.SubmissionDateRange
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import java.lang.Integer.max
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.abs

class TransmissionRiskLevelApplier @Inject constructor(
    private val stateMachine: IsolationStateMachine,
    private val calculateKeySubmissionDateRange: CalculateKeySubmissionDateRange,
) {
    fun applyTransmissionRiskLevels(
        keys: List<NHSTemporaryExposureKey>,
        keySharingInfo: KeySharingInfo
    ): List<NHSTemporaryExposureKey> {
        val assumedOnsetDate = stateMachine.readState().assumedOnsetDateForExposureKeys

        return if (assumedOnsetDate == null) {
            keys.sortedByDescending { it.rollingStartNumber }
                .map { it.copy(transmissionRiskLevel = MIN_TRANSMISSION_RISK_LEVEL) }
        } else {
            val submissionDateRange = calculateKeySubmissionDateRange(keySharingInfo.acknowledgedDate, assumedOnsetDate)
            keys.sortedByDescending { it.rollingStartNumber }
                .map { key ->
                    val keyDate = key.date()
                    val daysFromOnset = ChronoUnit.DAYS.between(assumedOnsetDate, keyDate).toInt()
                    key.copy(
                        transmissionRiskLevel = calculateTransmissionRiskLevel(keyDate, submissionDateRange, daysFromOnset),
                        daysSinceOnsetOfSymptoms = daysFromOnset
                    )
                }
        }
    }

    private fun calculateTransmissionRiskLevel(
        keyDate: LocalDate,
        submissionDateRange: SubmissionDateRange,
        daysFromOnset: Int
    ): Int {
        return if (submissionDateRange.includes(keyDate)) {
            val transmissionRiskLevel = MAX_TRANSMISSION_RISK_LEVEL - abs(daysFromOnset)
            max(MIN_TRANSMISSION_RISK_LEVEL, transmissionRiskLevel)
        } else {
            MIN_TRANSMISSION_RISK_LEVEL
        }
    }

    private fun NHSTemporaryExposureKey.date(): LocalDate {
        val timeSince1970 = rollingStartNumber * Duration.ofMinutes(10).toMillis()
        return Instant.ofEpochMilli(timeSince1970).toLocalDate(ZoneOffset.UTC)
    }
}

const val MAX_TRANSMISSION_RISK_LEVEL = 7
const val MIN_TRANSMISSION_RISK_LEVEL = 0
