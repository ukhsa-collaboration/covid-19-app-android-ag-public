package uk.nhs.nhsx.covid19.android.app.testordering

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestOrderResponse
import javax.inject.Inject

class LoadVirologyTestOrder @Inject constructor(private val virologyTestingApi: VirologyTestingApi) {
    suspend operator fun invoke(): Result<VirologyTestOrderResponse> = withContext(Dispatchers.IO) {
        runSafely {
            virologyTestingApi.getHomeKitOrder()
        }
    }
}
