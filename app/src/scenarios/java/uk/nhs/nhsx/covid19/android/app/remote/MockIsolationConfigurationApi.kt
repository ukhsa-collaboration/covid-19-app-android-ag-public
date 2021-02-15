package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.IsolationConfigurationResponse

class MockIsolationConfigurationApi : IsolationConfigurationApi {
    override suspend fun getIsolationConfiguration(): IsolationConfigurationResponse = MockApiModule.behaviour.invoke {
        IsolationConfigurationResponse(
            durationDays = DurationDays()
        )
    }
}
