package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.util.isBeforeOrEqual
import uk.nhs.nhsx.covid19.android.app.util.isEqualOrAfter
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import uk.nhs.nhsx.covid19.android.app.util.selectNewest
import java.time.Clock
import java.time.LocalDate

const val assumedDaysFromOnsetToTestResult: Long = 3
const val assumedDaysFromOnsetToSelfAssessment: Long = 2

interface IsolationInfo {
    val isolationConfiguration: DurationDays
    val indexInfo: IndexInfo?
    val contactCase: ContactCase?
    val hasAcknowledgedEndOfIsolation: Boolean
}

/**
 * Low-level isolation state, containing all information relevant to the isolation. This is intended to be used only
 * by classes that are very close to the isolation state machine and state storage. For a higher-level representation
 * of the isolation state use [IsolationLogicalState] instead.
 */
data class IsolationState(
    override val isolationConfiguration: DurationDays,
    override val indexInfo: IndexInfo? = null,
    override val contactCase: ContactCase? = null,
    override val hasAcknowledgedEndOfIsolation: Boolean = false
) : IsolationInfo {

    interface IsolationPeriod {
        val startDate: LocalDate
        val expiryDate: LocalDate
        fun hasExpired(clock: Clock) = expiryDate.isBeforeOrEqual(LocalDate.now(clock))

        fun overlaps(other: IsolationPeriod): Boolean =
            this.startDate.isBeforeOrEqual(other.expiryDate) &&
                this.expiryDate.isEqualOrAfter(other.startDate)

        fun capExpiryDate(isolationConfiguration: DurationDays): LocalDate {
            val maxExpiryDate = startDate.plusDays(isolationConfiguration.maxIsolation.toLong())
            return selectEarliest(maxExpiryDate, expiryDate)
        }

        companion object {
            /**
             * Go through all [isolationPeriods], find the newest ones that overlap, and merge those. That is, from those,
             * create a new isolation period where the start date is the minimum of those and the expiry date the maximum
             * of those.
             *
             * This function will only return null if [isolationPeriods] is empty.
             */
            fun mergeNewestOverlapping(isolationPeriods: List<IsolationPeriod>): IsolationPeriod? =
                isolationPeriods.reduceOrNull { latestPeriod, period ->
                    MergedIsolationPeriod(
                        startDate =
                            if (latestPeriod.overlaps(period)) selectEarliest(latestPeriod.startDate, period.startDate)
                            else selectNewest(latestPeriod.startDate, period.startDate),
                        expiryDate = selectNewest(latestPeriod.expiryDate, period.expiryDate)
                    )
                }
        }
    }

    data class MergedIsolationPeriod(
        override val startDate: LocalDate,
        override val expiryDate: LocalDate
    ) : IsolationPeriod

    @JsonClass(generateAdapter = true)
    data class ContactCase(
        val exposureDate: LocalDate,
        val notificationDate: LocalDate,
        val dailyContactTestingOptInDate: LocalDate? = null,
        override val expiryDate: LocalDate
    ) : IsolationPeriod {

        override val startDate: LocalDate
            get() = notificationDate
    }

    interface TestResultOwner {
        val testResult: AcknowledgedTestResult?
    }

    sealed class IndexInfo : TestResultOwner {

        data class NegativeTest(override val testResult: AcknowledgedTestResult) : IndexInfo()

        data class IndexCase(
            val isolationTrigger: IndexCaseIsolationTrigger,
            override val testResult: AcknowledgedTestResult? = null,
            override val expiryDate: LocalDate
        ) : IndexInfo(), IsolationPeriod {

            override val startDate: LocalDate by lazy {
                when (isolationTrigger) {
                    is SelfAssessment -> isolationTrigger.selfAssessmentDate
                    is PositiveTestResult -> isolationTrigger.testEndDate
                }
            }

            fun isSelfAssessment(): Boolean =
                isolationTrigger is SelfAssessment

            fun getSelfAssessmentOnsetDate(): LocalDate? =
                (isolationTrigger as? SelfAssessment)?.assumedOnsetDate

            val assumedOnsetDateForExposureKeys: LocalDate
                get() = when (isolationTrigger) {
                    is PositiveTestResult ->
                        isolationTrigger.testEndDate.minusDays(assumedDaysFromOnsetToTestResult)
                    is SelfAssessment ->
                        if (testResult != null && testResult.testEndDate.isBefore(isolationTrigger.assumedOnsetDate))
                            testResult.testEndDate.minusDays(assumedDaysFromOnsetToTestResult)
                        else
                            isolationTrigger.assumedOnsetDate
                }
        }
    }

    sealed class IndexCaseIsolationTrigger {

        @JsonClass(generateAdapter = true)
        data class SelfAssessment(
            val selfAssessmentDate: LocalDate,
            val onsetDate: LocalDate? = null
        ) : IndexCaseIsolationTrigger() {

            val assumedOnsetDate: LocalDate
                get() = onsetDate ?: selfAssessmentDate.minusDays(assumedDaysFromOnsetToSelfAssessment)
        }

        @JsonClass(generateAdapter = true)
        data class PositiveTestResult(
            val testEndDate: LocalDate
        ) : IndexCaseIsolationTrigger()

        companion object {
            fun from(
                receivedTestResult: ReceivedTestResult,
                triggerDate: LocalDate,
                clock: Clock
            ): IndexCaseIsolationTrigger? =
                when {
                    receivedTestResult.testResult != POSITIVE -> null
                    receivedTestResult.symptomsOnsetDate != null ->
                        SelfAssessment(
                            selfAssessmentDate = triggerDate,
                            onsetDate = receivedTestResult.symptomsOnsetDate.explicitDate
                        )
                    else -> PositiveTestResult(receivedTestResult.testEndDate(clock))
                }
        }
    }

    val assumedOnsetDateForExposureKeys: LocalDate?
        get() = (indexInfo as? IndexCase)?.assumedOnsetDateForExposureKeys
}
