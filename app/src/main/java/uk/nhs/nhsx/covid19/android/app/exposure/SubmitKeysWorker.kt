package uk.nhs.nhsx.covid19.android.app.exposure

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.remote.KeysSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.data.TemporaryExposureKeysPayload
import java.io.IOException
import javax.inject.Inject

class SubmitKeysWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    @Inject
    lateinit var moshi: Moshi

    @Inject
    lateinit var keysSubmissionApi: KeysSubmissionApi

    override suspend fun doWork(): Result {
        applicationContext.appComponent.inject(this)
        val adapter = moshi.adapter(TemporaryExposureKeysPayload::class.java)

        val payload = inputData.getString(TEMPORARY_EXPOSURE_KEYS_PAYLOAD)!!
        val jsonPayload = try {
            adapter.fromJson(payload) ?: return Result.failure()
        } catch (ioException: IOException) {
            return Result.failure()
        }
        return try {
            keysSubmissionApi.submitGeneratedKeys(jsonPayload)
            Result.success()
        } catch (exception: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val TEMPORARY_EXPOSURE_KEYS_PAYLOAD = "TEMPORARY_EXPOSURE_KEYS_PAYLOAD"
    }
}
