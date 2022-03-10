package uk.nhs.nhsx.covid19.android.app.state

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.IndexInfo.NegativeTest
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.NeverIsolating
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.util.selectEarliest
import javax.inject.Inject

class CreateIsolationLogicalState @Inject constructor(
    private val calculateContactExpiryDate: CalculateContactExpiryDate,
    private val calculateIndexExpiryDate: CalculateIndexExpiryDate,
    private val calculateIndexStartDate: CalculateIndexStartDate
) {

    operator fun invoke(isolationState: IsolationState): IsolationLogicalState {
        val contactIsolation = isolationState.toContactIsolation()
        val indexInfo = isolationState.toIndexInfo()

        return createIsolationLogicalState(
            contactIsolation,
            indexInfo,
            isolationState.isolationConfiguration,
            isolationState.hasAcknowledgedEndOfIsolation
        )
    }

    private fun createIsolationLogicalState(
        contactCase: ContactCase?,
        indexInfo: IndexInfo?,
        isolationConfiguration: IsolationConfiguration,
        hasAcknowledgedEndOfIsolation: Boolean
    ): IsolationLogicalState {
        val isolationPeriods = mutableListOf<IsolationPeriod>().apply {
            if (indexInfo is IndexCase) {
                add(indexInfo)
            }
            if (contactCase != null) {
                add(contactCase)
            }
        }

        val isolationPeriod = IsolationPeriod.mergeNewestOverlapping(isolationPeriods)
            ?: return createNeverIsolating(indexInfo?.testResult, isolationConfiguration)

        val mergedStartDate = isolationPeriod.startDate
        val mergedExpiryDate = isolationPeriod.capExpiryDate(isolationConfiguration)

        val cappedIndexInfo =
            if (indexInfo is IndexCase) indexInfo.copy(expiryDate = selectEarliest(indexInfo.expiryDate, mergedExpiryDate))
            else indexInfo

        val cappedContactCase = contactCase?.copy(expiryDate = selectEarliest(contactCase.expiryDate, mergedExpiryDate))

        return PossiblyIsolating(
            isolationConfiguration = isolationConfiguration,
            indexInfo = cappedIndexInfo,
            contactCase = cappedContactCase,
            hasAcknowledgedEndOfIsolation = hasAcknowledgedEndOfIsolation,
            startDate = mergedStartDate,
            expiryDate = mergedExpiryDate
        )
    }

    private fun createNeverIsolating(
        testResult: AcknowledgedTestResult?,
        isolationConfiguration: IsolationConfiguration
    ): NeverIsolating =
        NeverIsolating(
            isolationConfiguration = isolationConfiguration,
            negativeTest =
                when {
                    testResult == null -> null
                    testResult.isNegative() -> NegativeTest(testResult)
                    else -> {
                        Timber.e("Trying to create ${NeverIsolating::class.simpleName} with non-negative test result $testResult. This is not allowed => discarding test result")
                        null
                    }
                }
        )

    private fun IsolationState.toContactIsolation(): ContactCase? =
        if (contact == null) null
        else ContactCase(
            exposureDate = contact.exposureDate,
            notificationDate = contact.notificationDate,
            optOutOfContactIsolation = contact.optOutOfContactIsolation,
            expiryDate = calculateContactExpiryDate(contact, isolationConfiguration)
        )

    private fun IsolationState.toIndexInfo(): IndexInfo? =
        when {
            selfAssessment != null || testResult?.isPositive() == true -> {
                val startDate = calculateIndexStartDate(selfAssessment, testResult)
                val expiryDate = calculateIndexExpiryDate(selfAssessment, testResult, isolationConfiguration)
                when {
                    startDate == null -> {
                        Timber.e("Unexpectedly failed to calculate index case start date with self-assessment $selfAssessment and test result $testResult")
                        null
                    }
                    expiryDate == null -> {
                        Timber.e("Unexpectedly failed to calculate index case expiry date with self-assessment $selfAssessment and test result $testResult")
                        null
                    }
                    else ->
                        IndexCase(
                            selfAssessment = selfAssessment,
                            testResult = testResult,
                            startDate = startDate,
                            expiryDate = expiryDate
                        )
                }
            }

            testResult?.isNegative() == true -> NegativeTest(testResult)

            else -> null
        }
}
