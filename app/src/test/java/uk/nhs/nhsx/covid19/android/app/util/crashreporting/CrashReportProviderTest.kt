package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CrashReportProviderTest {

    private val moshi = Moshi.Builder().build()

    private val mockSynchronousCrashReportStorage =
        mockk<SynchronousCrashReportStorage>(relaxed = true)

    private val testSubject = CrashReportProvider(
        mockSynchronousCrashReportStorage,
        moshi
    )

    @Test
    fun `verify deserialization`() {
        every { mockSynchronousCrashReportStorage.value } returns crashReportJson

        val parsedCrashReport = testSubject.crashReport

        assertEquals(crashReport, parsedCrashReport)
    }

    @Test
    fun `verify serialization`() {

        testSubject.crashReport = crashReport

        verify { mockSynchronousCrashReportStorage.value = crashReportJson }
    }

    @Test
    fun `on exception will return null`() {
        every { mockSynchronousCrashReportStorage.value } returns "wrong_format"

        val parsedCrashReport = testSubject.crashReport

        assertNull(parsedCrashReport)
    }

    private val crashReport = CrashReport(
        exception = "android.app.RemoteServiceException",
        threadName = "main",
        stackTrace = "stackTrace"
    )

    private val crashReportJson =
        """{"exception":"android.app.RemoteServiceException","threadName":"main","stackTrace":"stackTrace"}"""
}
