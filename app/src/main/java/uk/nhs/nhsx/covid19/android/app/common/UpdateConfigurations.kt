package uk.nhs.nhsx.covid19.android.app.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.RiskyVenueConfigurationProvider
import uk.nhs.nhsx.covid19.android.app.remote.IsolationConfigurationApi
import uk.nhs.nhsx.covid19.android.app.remote.RiskyVenueConfigurationApi
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfigurationProvider
import javax.inject.Inject

class UpdateConfigurations @Inject constructor(
    private val isolationConfigurationProvider: IsolationConfigurationProvider,
    private val isolationConfigurationApi: IsolationConfigurationApi,
    private val riskyVenueConfigurationProvider: RiskyVenueConfigurationProvider,
    private val riskyVenueConfigurationApi: RiskyVenueConfigurationApi,
    private val convertIsolationConfigurationResponseToDurationDays: ConvertIsolationConfigurationResponseToDurationDays
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        Timber.d("Updating configurations for isolation and risky venue")
        updateIsolationConfiguration()
        updateRiskyVenueConfiguration()
    }

    private suspend fun updateIsolationConfiguration() {
        runCatching {
            val response = isolationConfigurationApi.getIsolationConfiguration()
            isolationConfigurationProvider.durationDays = convertIsolationConfigurationResponseToDurationDays(response)
        }
    }

    private suspend fun updateRiskyVenueConfiguration() {
        runCatching {
            val response = riskyVenueConfigurationApi.getRiskyVenueConfiguration()
            riskyVenueConfigurationProvider.durationDays = response.durationDays
        }
    }
}
