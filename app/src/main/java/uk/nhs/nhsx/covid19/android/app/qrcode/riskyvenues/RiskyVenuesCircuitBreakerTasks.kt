package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import javax.inject.Inject

class RiskyVenuesCircuitBreakerTasks @Inject constructor(private val context: Context) {

    fun scheduleRiskyVenuesCircuitBreakerInitial(venueIds: List<String>) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(CONNECTED)
            .build()

        val inputData =
            Data.Builder()
                .putStringArray(
                    VENUE_IDS,
                    venueIds.toTypedArray()
                )
                .build()

        val initialRiskyVenuesCircuitBreakerWork =
            OneTimeWorkRequestBuilder<RiskyVenuesCircuitBreakerInitialWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

        WorkManager.getInstance(context).enqueue(initialRiskyVenuesCircuitBreakerWork)
    }

    companion object {
        const val VENUE_IDS = "VENUE_IDS"
    }
}
