package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import uk.nhs.nhsx.covid19.android.app.status.DownloadRiskyPostCodesWorker
import javax.inject.Inject

class UpdateAreaRisk @Inject constructor(private val context: Context) {

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(CONNECTED)
        .build()

    fun schedule() {
        val updateAreaRiskWork = OneTimeWorkRequestBuilder<DownloadRiskyPostCodesWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(updateAreaRiskWork)
    }
}
