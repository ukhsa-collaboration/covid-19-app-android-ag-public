package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider.Companion.DURATION_DAYS_KEY
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT

class IsolationConfigurationProviderTest : ProviderTest<IsolationConfigurationProvider, DurationDays>() {

    override val getTestSubject = ::IsolationConfigurationProvider
    override val property = IsolationConfigurationProvider::durationDays
    override val key = DURATION_DAYS_KEY
    override val defaultValue = DurationDays()
    override val expectations: List<ProviderTestExpectation<DurationDays>> = listOf(
        ProviderTestExpectation(json = durationDaysJson, objectValue = durationDays),
        ProviderTestExpectation(json = durationDaysJsonWithoutHousekeepingPeriod, objectValue = durationDaysDefaultHousekeepingPeriod, direction = JSON_TO_OBJECT)
    )

    companion object {
        private val durationDays = DurationDays(
            contactCase = 32,
            indexCaseSinceSelfDiagnosisOnset = 7,
            indexCaseSinceSelfDiagnosisUnknownOnset = 5,
            maxIsolation = 21,
            pendingTasksRetentionPeriod = 8,
            indexCaseSinceTestResultEndDate = 11
        )

        private const val durationDaysJson =
            """{"contactCase":32,"indexCaseSinceSelfDiagnosisOnset":7,"indexCaseSinceSelfDiagnosisUnknownOnset":5,"maxIsolation":21,"pendingTasksRetentionPeriod":8,"indexCaseSinceTestResultEndDate":11}"""

        private val durationDaysDefaultHousekeepingPeriod = DurationDays(
            contactCase = 32,
            indexCaseSinceSelfDiagnosisOnset = 7,
            indexCaseSinceSelfDiagnosisUnknownOnset = 5,
            maxIsolation = 21,
            pendingTasksRetentionPeriod = 14
        )

        private const val durationDaysJsonWithoutHousekeepingPeriod =
            """{"contactCase":32,"indexCaseSinceSelfDiagnosisOnset":7,"indexCaseSinceSelfDiagnosisUnknownOnset":5,"maxIsolation":21}"""
    }
}
