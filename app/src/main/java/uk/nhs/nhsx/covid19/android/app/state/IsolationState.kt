package uk.nhs.nhsx.covid19.android.app.state

import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.PositiveTestResult
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexCaseIsolationTrigger.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IsolationPeriod
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IsolationPeriod.Companion.mergeNewestOverlapping
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

private interface IsolationInfo {
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
             * create a new isolation period where the start date is the miminum of those and the expiry date the maximum
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
        }
    }

    sealed class IndexCaseIsolationTrigger {
        abstract val assumedOnsetDateForExposureKeys: LocalDate

        @JsonClass(generateAdapter = true)
        data class SelfAssessment(
            val selfAssessmentDate: LocalDate,
            val onsetDate: LocalDate? = null
        ) : IndexCaseIsolationTrigger() {

            val assumedOnsetDate: LocalDate
                get() = onsetDate ?: selfAssessmentDate.minusDays(assumedDaysFromOnsetToSelfAssessment)

            override val assumedOnsetDateForExposureKeys: LocalDate
                get() = assumedOnsetDate
        }

        @JsonClass(generateAdapter = true)
        data class PositiveTestResult(
            val testEndDate: LocalDate
        ) : IndexCaseIsolationTrigger() {

            override val assumedOnsetDateForExposureKeys: LocalDate
                get() = testEndDate.minusDays(assumedDaysFromOnsetToTestResult)
        }

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
                    else -> PositiveTestResult(receivedTestResult.testEndDay(clock))
                }
        }
    }

    val assumedOnsetDateForExposureKeys: LocalDate?
        get() = (indexInfo as? IndexCase)?.isolationTrigger?.assumedOnsetDateForExposureKeys
}

/**
 * High-level representation of the isolation state, as opposed to the low-level [IsolationState].
 */
sealed class IsolationLogicalState {
    abstract val isolationConfiguration: DurationDays
    abstract fun toIsolationState(): IsolationState
    abstract fun capExpiryDate(isolationPeriod: IsolationPeriod): LocalDate
    abstract fun isActiveIsolation(clock: Clock): Boolean

    data class NeverIsolating(
        override val isolationConfiguration: DurationDays,
        val negativeTest: NegativeTest?
    ) : IsolationLogicalState() {
        override fun toIsolationState(): IsolationState =
            IsolationState(
                isolationConfiguration,
                indexInfo = negativeTest,
                contactCase = null
            )

        override fun capExpiryDate(isolationPeriod: IsolationPeriod): LocalDate =
            isolationPeriod.capExpiryDate(isolationConfiguration)

        override fun isActiveIsolation(clock: Clock): Boolean =
            false
    }

    data class PossiblyIsolating(val isolationState: IsolationState) : IsolationLogicalState(), IsolationInfo by isolationState, IsolationPeriod {
        override val startDate: LocalDate
        override val expiryDate: LocalDate
        val lastDayOfIsolation: LocalDate

        init {
            val isolationPeriods = mutableListOf<IsolationPeriod>().apply {
                if (indexInfo is IndexCase) {
                    add(indexInfo)
                }
                if (contactCase != null) {
                    add(contactCase)
                }
            }

            val isolationPeriod = mergeNewestOverlapping(isolationPeriods)
                ?: throw IllegalArgumentException("Cannot instantiate using an isolation that has neither an index nor a contact case")

            startDate = isolationPeriod.startDate
            expiryDate = isolationPeriod.capExpiryDate(isolationConfiguration)
            lastDayOfIsolation = expiryDate.minusDays(1)
        }

        override fun toIsolationState(): IsolationState = isolationState

        fun isActiveIndexCase(clock: Clock): Boolean =
            indexInfo is IndexCase && !indexInfo.hasExpired(clock)

        fun isActiveIndexCaseOnly(clock: Clock): Boolean =
            isActiveIndexCase(clock) && !isActiveContactCase(clock)

        fun getActiveIndexCase(clock: Clock): IndexCase? =
            if (isActiveIndexCase(clock)) indexInfo as? IndexCase
            else null

        fun isActiveContactCase(clock: Clock): Boolean =
            contactCase != null && !contactCase.hasExpired(clock)

        fun isActiveContactCaseOnly(clock: Clock): Boolean =
            isActiveContactCase(clock) && !isActiveIndexCase(clock)

        fun getActiveContactCase(clock: Clock): ContactCase? =
            if (isActiveContactCase(clock)) contactCase
            else null

        fun remembersIndexCase(): Boolean =
            indexInfo is IndexCase

        fun remembersIndexCaseOnly(): Boolean =
            remembersIndexCase() && !remembersContactCase()

        fun remembersIndexCaseWithSelfAssessment(): Boolean =
            indexInfo is IndexCase && indexInfo.isSelfAssessment()

        fun remembersContactCase(): Boolean =
            contactCase != null

        fun remembersContactCaseOnly(): Boolean =
            remembersContactCase() && !remembersIndexCase()

        fun remembersBothCases(): Boolean =
            remembersIndexCase() && remembersContactCase()

        override fun capExpiryDate(isolationPeriod: IsolationPeriod): LocalDate {
            val mergedIsolation = mergeNewestOverlapping(listOf(this, isolationPeriod)) ?: isolationPeriod
            val maxExpiryDate = mergedIsolation.capExpiryDate(isolationConfiguration)
            return selectEarliest(maxExpiryDate, isolationPeriod.expiryDate)
        }

        override fun isActiveIsolation(clock: Clock): Boolean =
            !hasExpired(clock)

        fun getActiveTestResult(clock: Clock): AcknowledgedTestResult? =
            getActiveIndexCase(clock)?.testResult

        fun hasActiveConfirmedPositiveTestResult(clock: Clock): Boolean =
            getActiveTestResult(clock)?.let { activeTestResult ->
                activeTestResult.isPositive() && activeTestResult.isConfirmed()
            } ?: false

        fun getTestResultIfPositive(): AcknowledgedTestResult? {
            val relevantTestResult = indexInfo?.testResult
            return if (relevantTestResult != null && relevantTestResult.isPositive()) relevantTestResult
            else null
        }
    }

    fun canReportSymptoms(clock: Clock): Boolean =
        if (this is PossiblyIsolating && this.isActiveIsolation(clock)) isActiveContactCaseOnly(clock) else true

    companion object {
        fun from(isolationState: IsolationState): IsolationLogicalState =
            if (isolationState.indexInfo !is IndexCase && isolationState.contactCase == null)
                NeverIsolating(
                    isolationConfiguration = isolationState.isolationConfiguration,
                    negativeTest = isolationState.indexInfo as? NegativeTest
                )
            else
                PossiblyIsolating(isolationState)
    }
}
