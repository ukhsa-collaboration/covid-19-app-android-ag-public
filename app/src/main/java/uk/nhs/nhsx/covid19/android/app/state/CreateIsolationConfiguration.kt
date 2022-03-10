package uk.nhs.nhsx.covid19.android.app.state

import kotlinx.coroutines.runBlocking
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import javax.inject.Inject

class CreateIsolationConfiguration @Inject constructor(
    private val localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider
) {
    operator fun invoke(durationDays: DurationDays): IsolationConfiguration {
        return runBlocking {
            val countrySpecificConfiguration =
                when (localAuthorityPostCodeProvider.getPostCodeDistrict()) {
                    ENGLAND -> durationDays.england
                    WALES -> durationDays.wales
                    else -> dummyConfigurationDuringOnboarding
                }

            IsolationConfiguration(
                countrySpecificConfiguration.contactCase,
                countrySpecificConfiguration.indexCaseSinceSelfDiagnosisOnset,
                countrySpecificConfiguration.indexCaseSinceSelfDiagnosisUnknownOnset,
                countrySpecificConfiguration.maxIsolation,
                countrySpecificConfiguration.pendingTasksRetentionPeriod,
                countrySpecificConfiguration.indexCaseSinceTestResultEndDate,
                countrySpecificConfiguration.testResultPollingTokenRetentionPeriod
            )
        }
    }

    companion object {
        private val dummyConfigurationDuringOnboarding = CountrySpecificConfiguration(
            contactCase = 0,
            indexCaseSinceSelfDiagnosisOnset = 0,
            indexCaseSinceSelfDiagnosisUnknownOnset = 0,
            maxIsolation = 0,
            indexCaseSinceTestResultEndDate = 0,
            pendingTasksRetentionPeriod = 0,
            testResultPollingTokenRetentionPeriod = 0
        )
    }
}
