package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.OBJECT_TO_JSON

class CrashReportProviderTest : ProviderTest<CrashReportProvider, CrashReport?>() {

    override val getTestSubject = ::CrashReportProvider
    override val property = CrashReportProvider::crashReport
    override val key = CrashReportProvider.VALUE_KEY
    override val defaultValue: CrashReport? = null
    override val expectations: List<ProviderTestExpectation<CrashReport?>> = listOf(
        ProviderTestExpectation(json = crashReportJson, objectValue = crashReport),
        ProviderTestExpectation(json = null, objectValue = null, direction = OBJECT_TO_JSON)
    )

    @Test
    fun `clear sets crash report to null`() {
        testSubject.clear()

        assertSharedPreferenceSetsValue(null)
    }

    companion object {
        private val crashReport = CrashReport(
            exception = "android.app.RemoteServiceException",
            threadName = "main",
            stackTrace = "stackTrace"
        )

        private const val crashReportJson =
            """{"exception":"android.app.RemoteServiceException","threadName":"main","stackTrace":"stackTrace"}"""
    }
}
