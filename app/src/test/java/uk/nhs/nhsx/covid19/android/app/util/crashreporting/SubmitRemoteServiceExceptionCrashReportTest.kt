package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.RemoteServiceExceptionCrashReportSubmissionApi

class SubmitRemoteServiceExceptionCrashReportTest {

    private val mockRemoteServiceExceptionCrashReportSubmissionApi = mockk<RemoteServiceExceptionCrashReportSubmissionApi>(relaxUnitFun = true)

    private val submitRemoteServiceExceptionCrashReport = SubmitRemoteServiceExceptionCrashReport(mockRemoteServiceExceptionCrashReportSubmissionApi)

    @Test
    fun `calls crash report submission api with crash report`() = runBlocking {
        val expectedCrashReport = mockk<CrashReport>()

        submitRemoteServiceExceptionCrashReport(expectedCrashReport)

        coVerify { mockRemoteServiceExceptionCrashReportSubmissionApi.submitRemoteServiceExceptionCrashReport(expectedCrashReport) }
    }
}
