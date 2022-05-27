package uk.nhs.nhsx.covid19.android.app.state

import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
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
        ProviderTestExpectation(
            json = legacyDurationDaysJson,
            objectValue = durationDaysNewDefault,
            direction = JSON_TO_OBJECT
        )
    )

    companion object {
        private val durationDays = DurationDays(
            england = CountrySpecificConfiguration(
                contactCase = 1,
                indexCaseSinceSelfDiagnosisOnset = 2,
                indexCaseSinceSelfDiagnosisUnknownOnset = 3,
                maxIsolation = 4,
                pendingTasksRetentionPeriod = 5,
                indexCaseSinceTestResultEndDate = 6,
                testResultPollingTokenRetentionPeriod = 7
            ),
            wales = CountrySpecificConfiguration(
                contactCase = 8,
                indexCaseSinceSelfDiagnosisOnset = 9,
                indexCaseSinceSelfDiagnosisUnknownOnset = 10,
                maxIsolation = 11,
                pendingTasksRetentionPeriod = 12,
                indexCaseSinceTestResultEndDate = 13,
                testResultPollingTokenRetentionPeriod = 14
            )
        )

        private const val durationDaysJson =
            """{"england":{"contactCase":1,"indexCaseSinceSelfDiagnosisOnset":2,"indexCaseSinceSelfDiagnosisUnknownOnset":3,"maxIsolation":4,"indexCaseSinceTestResultEndDate":6,"pendingTasksRetentionPeriod":5,"testResultPollingTokenRetentionPeriod":7},"wales":{"contactCase":8,"indexCaseSinceSelfDiagnosisOnset":9,"indexCaseSinceSelfDiagnosisUnknownOnset":10,"maxIsolation":11,"indexCaseSinceTestResultEndDate":13,"pendingTasksRetentionPeriod":12,"testResultPollingTokenRetentionPeriod":14}}"""

        private val durationDaysNewDefault = DurationDays(
            england = CountrySpecificConfiguration(
                contactCase = 11,
                indexCaseSinceSelfDiagnosisOnset = 11,
                indexCaseSinceSelfDiagnosisUnknownOnset = 9,
                maxIsolation = 21,
                indexCaseSinceTestResultEndDate = 11,
                pendingTasksRetentionPeriod = 14,
                testResultPollingTokenRetentionPeriod = 28
            ),
            wales = CountrySpecificConfiguration(
                contactCase = 11,
                indexCaseSinceSelfDiagnosisOnset = 6,
                indexCaseSinceSelfDiagnosisUnknownOnset = 6,
                maxIsolation = 16,
                indexCaseSinceTestResultEndDate = 6,
                pendingTasksRetentionPeriod = 14,
                testResultPollingTokenRetentionPeriod = 28
            )
        )

        private const val legacyDurationDaysJson =
            """{"contactCase":32,"indexCaseSinceSelfDiagnosisOnset":7,"indexCaseSinceSelfDiagnosisUnknownOnset":5,"maxIsolation":21,"indexCaseSinceTestResultEndDate": 9,
                "pendingTasksRetentionPeriod": 5,"testResultPollingTokenRetentionPeriod": 10}"""
    }
}
