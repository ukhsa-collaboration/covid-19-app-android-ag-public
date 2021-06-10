package uk.nhs.nhsx.covid19.android.app.status.localmessage

import androidx.work.ListenableWorker.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.remote.LocalMessagesApi
import uk.nhs.nhsx.covid19.android.app.status.ShowLocalMessageNotificationIfNeeded
import uk.nhs.nhsx.covid19.android.app.util.toWorkerResult
import javax.inject.Inject

class DownloadLocalMessagesWork @Inject constructor(
    private val localMessagesApi: LocalMessagesApi,
    private val localMessagesProvider: LocalMessagesProvider,
    private val showLocalMessageNotificationIfNeeded: ShowLocalMessageNotificationIfNeeded
) {
    suspend operator fun invoke(): Result = withContext(Dispatchers.IO) {
        runSafely {
            val previousResponse = localMessagesProvider.localMessages
            val receivedResponse = localMessagesApi.fetchLocalMessages()
            localMessagesProvider.localMessages = receivedResponse

            showLocalMessageNotificationIfNeeded(previousResponse, receivedResponse)
        }.toWorkerResult()
    }
}
