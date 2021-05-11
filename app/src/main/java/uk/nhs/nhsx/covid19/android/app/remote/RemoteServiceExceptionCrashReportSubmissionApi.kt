package uk.nhs.nhsx.covid19.android.app.remote

import retrofit2.http.Body
import retrofit2.http.POST
import uk.nhs.nhsx.covid19.android.app.util.crashreporting.CrashReport

interface RemoteServiceExceptionCrashReportSubmissionApi {
    @POST("submission/crash-reports")
    suspend fun submitRemoteServiceExceptionCrashReport(@Body crashReport: CrashReport)
}
