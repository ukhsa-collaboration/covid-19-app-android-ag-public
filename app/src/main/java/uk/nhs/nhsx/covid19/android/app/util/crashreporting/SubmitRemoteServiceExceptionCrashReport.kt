package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.RemoteServiceExceptionCrashReportSubmissionApi
import javax.inject.Inject

class SubmitRemoteServiceExceptionCrashReport @Inject constructor(
    private val remoteServiceExceptionCrashReportSubmissionApi: RemoteServiceExceptionCrashReportSubmissionApi
) {
    suspend operator fun invoke(crashReport: CrashReport) {
        withContext(Dispatchers.IO) {
            try {
                remoteServiceExceptionCrashReportSubmissionApi.submitRemoteServiceExceptionCrashReport(crashReport)
            } catch (e: Exception) {
                Timber.d(e, "Failed to submit crash report. Ignoring.")
            }
        }
    }
}
