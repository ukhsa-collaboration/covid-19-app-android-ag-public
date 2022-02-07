package uk.nhs.nhsx.covid19.android.app.localstats

import uk.nhs.nhsx.covid19.android.app.remote.LocalStatsApi
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalStatsResponse
import javax.inject.Inject

class FetchLocalStats @Inject constructor(private val localStatsApi: LocalStatsApi) {
    suspend operator fun invoke(): LocalStatsResponse {
        return localStatsApi.fetchLocalStats()
    }
}
