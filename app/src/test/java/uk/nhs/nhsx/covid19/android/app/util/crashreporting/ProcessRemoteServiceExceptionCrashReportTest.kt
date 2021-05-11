package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import com.jeroenmols.featureflag.framework.FeatureFlag.REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.coRunWithFeature

class ProcessRemoteServiceExceptionCrashReportTest {

    private val mockCrashReportProvider = mockk<CrashReportProvider>(relaxUnitFun = true)
    private val mockSubmitRemoteServiceExceptionCrashReport = mockk<SubmitRemoteServiceExceptionCrashReport>(relaxUnitFun = true)

    private val testSubject = ProcessRemoteServiceExceptionCrashReport(
        mockCrashReportProvider,
        mockSubmitRemoteServiceExceptionCrashReport
    )

    @Test
    fun `when flag enabled but there is no crash report in storage then do not submit crash report and clear storage`() = runBlocking {
        coRunWithFeature(REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS, enabled = true) {
            every { mockCrashReportProvider.crashReport } returns null

            testSubject()

            coVerify(exactly = 0) { mockSubmitRemoteServiceExceptionCrashReport.invoke(any()) }
            verify { mockCrashReportProvider.clear() }
        }
    }

    @Test
    fun `when flag enabled and there is a stored crash report then submit crash report and clear storage`() = runBlocking {
        coRunWithFeature(REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS, enabled = true) {
            val expectedCrashReport = mockk<CrashReport>()
            every { mockCrashReportProvider.crashReport } returns expectedCrashReport

            testSubject()

            coVerify { mockSubmitRemoteServiceExceptionCrashReport.invoke(expectedCrashReport) }
            verify { mockCrashReportProvider.clear() }
        }
    }

    @Test
    fun `when feature flag not enabled and there is crash report in storage then do not submit crash report but clear storage`() = runBlocking {
        coRunWithFeature(REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS, enabled = false) {
            val expectedCrashReport = mockk<CrashReport>()
            every { mockCrashReportProvider.crashReport } returns expectedCrashReport

            testSubject()

            coVerify(exactly = 0) { mockSubmitRemoteServiceExceptionCrashReport.invoke(expectedCrashReport) }
            verify { mockCrashReportProvider.clear() }
        }
    }

    @Test
    fun `when feature flag not enabled and no crash report stored then do not submit crash report but clear storage`() = runBlocking {
        coRunWithFeature(REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS, enabled = false) {
            every { mockCrashReportProvider.crashReport } returns null

            testSubject()

            coVerify(exactly = 0) { mockSubmitRemoteServiceExceptionCrashReport.invoke(any()) }
            verify { mockCrashReportProvider.clear() }
        }
    }
}
