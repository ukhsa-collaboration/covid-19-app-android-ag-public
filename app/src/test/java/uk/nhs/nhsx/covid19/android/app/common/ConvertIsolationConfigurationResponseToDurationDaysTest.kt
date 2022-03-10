package uk.nhs.nhsx.covid19.android.app.common

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationConfigurationResponse
import kotlin.test.assertEquals

class ConvertIsolationConfigurationResponseToDurationDaysTest {

    @Test
    fun `isolation configuration response is converted to duration days`() {
        val englandConfiguration = CountrySpecificConfiguration(
            contactCase = 1,
            indexCaseSinceSelfDiagnosisOnset = 2,
            indexCaseSinceSelfDiagnosisUnknownOnset = 3,
            maxIsolation = 4,
            pendingTasksRetentionPeriod = 5,
            indexCaseSinceTestResultEndDate = 6,
            testResultPollingTokenRetentionPeriod = 7
        )
        val walesConfiguration = CountrySpecificConfiguration(
            contactCase = 8,
            indexCaseSinceSelfDiagnosisOnset = 9,
            indexCaseSinceSelfDiagnosisUnknownOnset = 10,
            maxIsolation = 11,
            pendingTasksRetentionPeriod = 12,
            indexCaseSinceTestResultEndDate = 13,
            testResultPollingTokenRetentionPeriod = 14
        )

        val isolationConfigurationResponse = IsolationConfigurationResponse(
            englandConfiguration,
            walesConfiguration
        )

        val expectedDurationDaysAfterConversion = DurationDays(
            england = englandConfiguration,
            wales = walesConfiguration
        )

        val actualDurationDays =
            ConvertIsolationConfigurationResponseToDurationDays().invoke(isolationConfigurationResponse)

        assertEquals(expectedDurationDaysAfterConversion, actualDurationDays)
    }
}
