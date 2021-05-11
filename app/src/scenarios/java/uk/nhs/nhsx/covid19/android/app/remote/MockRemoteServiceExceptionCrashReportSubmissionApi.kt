package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.util.crashreporting.CrashReport

class MockRemoteServiceExceptionCrashReportSubmissionApi : RemoteServiceExceptionCrashReportSubmissionApi {
    override suspend fun submitRemoteServiceExceptionCrashReport(crashReport: CrashReport) =
        MockApiModule.behaviour.invoke { }
}
