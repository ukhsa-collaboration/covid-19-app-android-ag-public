package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import com.jeroenmols.featureflag.framework.FeatureFlag.REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import java.lang.Exception
import kotlin.test.assertFails

class RemoteServiceExceptionHandlerTest {

    private val mockRemoteServiceExceptionReportRateLimiter = mockk<RemoteServiceExceptionReportRateLimiter>()
    private val mockCrashReportProvider = mockk<CrashReportProvider>()
    private val mockSetDefaultUncaughtExceptionHandler = mockk<SetDefaultUncaughtExceptionHandler>(relaxUnitFun = true)
    private val mockCheckThrowableIsRemoteServiceException = mockk<CheckThrowableIsRemoteServiceException>()

    private val thread = mockk<Thread>()
    private val exception = Exception()

    private val testSubject = RemoteServiceExceptionHandler(
        mockRemoteServiceExceptionReportRateLimiter,
        mockCrashReportProvider,
        mockSetDefaultUncaughtExceptionHandler,
        mockCheckThrowableIsRemoteServiceException
    )

    @Test
    fun `when feature flag enabled then set as uncaught exception handler on initialize`() {
        runWithFeature(REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS, enabled = true) {
            testSubject.initialize()

            verify { mockSetDefaultUncaughtExceptionHandler(testSubject) }
        }
    }

    @Test
    fun `when feature flag not enabled then do not set as uncaught exception handler on initialize`() {
        runWithFeature(REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS, enabled = false) {
            testSubject.initialize()

            verify(exactly = 0) { mockSetDefaultUncaughtExceptionHandler(testSubject) }
        }
    }

    @Test
    fun `when exception is not RemoteServiceException then rethrow and crash report is not stored`() {
        every { mockCheckThrowableIsRemoteServiceException(exception) } returns false

        assertFails {
            testSubject.uncaughtException(thread, exception)
        }

        verify {
            mockCrashReportProvider wasNot called
        }
    }

    @Test
    fun `when exception is RemoteServiceException and Rate limit not allowed then rethrow and crash report is not stored`() {

        every { mockCheckThrowableIsRemoteServiceException(exception) } returns true
        every { mockRemoteServiceExceptionReportRateLimiter.isAllowed() } returns false

        assertFails {
            testSubject.uncaughtException(thread, exception)
        }

        verify {
            mockCrashReportProvider wasNot called
        }
    }

    @Test
    fun `when exception is RemoteServiceException and rate limiter is allowed then rethrow and crash report is stored`() {
        val threadName = "Thread name"

        every { thread.name } returns threadName
        every { mockCheckThrowableIsRemoteServiceException(exception) } returns true
        every { mockRemoteServiceExceptionReportRateLimiter.isAllowed() } returns true

        assertFails {
            testSubject.uncaughtException(thread, exception)
        }

        verify {
            mockCrashReportProvider setProperty "crashReport" value CrashReport(
                "android.app.RemoteServiceException",
                threadName,
                exception.stackTraceToString()
            )
        }
    }
}
