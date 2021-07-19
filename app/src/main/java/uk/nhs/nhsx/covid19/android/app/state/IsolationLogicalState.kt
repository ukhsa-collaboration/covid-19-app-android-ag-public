package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.IsolationPeriod
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import java.time.Clock
import java.time.LocalDate

/**
 * High-level representation of the isolation state, as opposed to the low-level [IsolationState].
 */
sealed class IsolationLogicalState {
    abstract val isolationConfiguration: DurationDays

    abstract fun toIsolationState(): IsolationState
    abstract fun remembersIndexCase(): Boolean
    abstract fun remembersIndexCaseWithSelfAssessment(): Boolean
    abstract fun remembersContactCase(): Boolean
    abstract fun isActiveIsolation(clock: Clock): Boolean
    abstract fun isActiveIndexCase(clock: Clock): Boolean
    abstract fun isActiveContactCase(clock: Clock): Boolean
    abstract fun getIndexCase(): IndexCase?
    abstract fun getActiveIndexCase(clock: Clock): IndexCase?
    abstract fun getActiveContactCase(clock: Clock): ContactCase?
    abstract fun getTestResult(): AcknowledgedTestResult?
    abstract fun capExpiryDate(isolationPeriod: IsolationPeriod): LocalDate

    fun isActiveContactCaseOnly(clock: Clock): Boolean =
        isActiveContactCase(clock) && !isActiveIndexCase(clock)

    fun isActiveIndexCaseOnly(clock: Clock): Boolean =
        isActiveIndexCase(clock) && !isActiveContactCase(clock)

    fun remembersIndexCaseOnly(): Boolean =
        remembersIndexCase() && !remembersContactCase()

    fun remembersContactCaseOnly(): Boolean =
        remembersContactCase() && !remembersIndexCase()

    fun remembersBothCases(): Boolean =
        remembersIndexCase() && remembersContactCase()

    fun getTestResultIfPositive(): AcknowledgedTestResult? {
        val relevantTestResult = getTestResult()
        return if (relevantTestResult != null && relevantTestResult.isPositive()) relevantTestResult
        else null
    }

    fun getActiveTestResult(clock: Clock): AcknowledgedTestResult? =
        getActiveIndexCase(clock)?.testResult

    fun getActiveTestResultIfPositive(clock: Clock): AcknowledgedTestResult? {
        val activeTestResult = getActiveTestResult(clock)
        return if (activeTestResult != null && activeTestResult.isPositive()) activeTestResult
        else null
    }

    fun hasActiveConfirmedPositiveTestResult(clock: Clock): Boolean =
        getActiveTestResult(clock)?.let { activeTestResult ->
            activeTestResult.isPositive() && activeTestResult.isConfirmed()
        } ?: false

    fun hasCompletedPositiveTestResult(): Boolean =
        getTestResultIfPositive()?.isCompleted() ?: false

    fun hasActivePositiveTestResult(clock: Clock): Boolean =
        getActiveTestResultIfPositive(clock) != null

    fun canReportSymptoms(clock: Clock): Boolean {
        val notInIsolation = !this.isActiveIsolation(clock)
        val isActiveSelfAssessmentIndexCase =
            this.isActiveIndexCase(clock) && remembersIndexCaseWithSelfAssessment()
        return notInIsolation || !isActiveSelfAssessmentIndexCase
    }

    /**
     * The app does not know about any previous isolation
     */
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

        override fun remembersContactCase(): Boolean = false
        override fun remembersIndexCase(): Boolean = false
        override fun remembersIndexCaseWithSelfAssessment(): Boolean = false
        override fun isActiveIsolation(clock: Clock): Boolean = false
        override fun isActiveIndexCase(clock: Clock): Boolean = false
        override fun isActiveContactCase(clock: Clock): Boolean = false
        override fun getIndexCase(): IndexCase? = null
        override fun getActiveIndexCase(clock: Clock): IndexCase? = null
        override fun getActiveContactCase(clock: Clock): ContactCase? = null

        override fun getTestResult(): AcknowledgedTestResult? =
            negativeTest?.testResult

        override fun capExpiryDate(isolationPeriod: IsolationPeriod): LocalDate =
            isolationPeriod.capExpiryDate(isolationConfiguration)
    }

    /**
     * The app knows about a previous isolation, which might be expired
     */
    data class PossiblyIsolating(
        val isolationState: IsolationState
    ) : IsolationLogicalState(),
        IsolationInfo by isolationState,
        IsolationPeriod {

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

            val isolationPeriod = IsolationPeriod.mergeNewestOverlapping(isolationPeriods)
                ?: throw IllegalArgumentException("Cannot instantiate using an isolation that has neither an index nor a contact case")

            startDate = isolationPeriod.startDate
            expiryDate = isolationPeriod.capExpiryDate(isolationConfiguration)
            lastDayOfIsolation = expiryDate.minusDays(1)
        }

        override fun toIsolationState(): IsolationState = isolationState

        override fun isActiveIsolation(clock: Clock): Boolean =
            !hasExpired(clock)

        override fun remembersIndexCase(): Boolean =
            indexInfo is IndexCase

        override fun remembersIndexCaseWithSelfAssessment(): Boolean =
            indexInfo is IndexCase && indexInfo.isSelfAssessment()

        override fun remembersContactCase(): Boolean =
            contactCase != null

        override fun isActiveIndexCase(clock: Clock): Boolean =
            indexInfo is IndexCase && !indexInfo.hasExpired(clock)

        override fun isActiveContactCase(clock: Clock): Boolean =
            contactCase != null && !contactCase.hasExpired(clock)

        override fun getActiveContactCase(clock: Clock): ContactCase? =
            if (isActiveContactCase(clock)) contactCase
            else null

        override fun getIndexCase(): IndexCase? = indexInfo as? IndexCase

        override fun getActiveIndexCase(clock: Clock): IndexCase? =
            if (isActiveIndexCase(clock)) indexInfo as? IndexCase
            else null

        override fun getTestResult(): AcknowledgedTestResult? =
            indexInfo?.testResult

        override fun capExpiryDate(isolationPeriod: IsolationPeriod): LocalDate {
            val mergedIsolation =
                IsolationPeriod.mergeNewestOverlapping(listOf(this, isolationPeriod)) ?: isolationPeriod
            val maxExpiryDate = mergedIsolation.capExpiryDate(isolationConfiguration)
            return selectEarliest(maxExpiryDate, isolationPeriod.expiryDate)
        }
    }

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
