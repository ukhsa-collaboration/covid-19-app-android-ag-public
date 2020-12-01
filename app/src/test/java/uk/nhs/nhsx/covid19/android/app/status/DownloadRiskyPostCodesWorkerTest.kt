package uk.nhs.nhsx.covid19.android.app.status

import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import kotlin.test.assertEquals

class DownloadRiskyPostCodesWorkerTest : FieldInjectionUnitTest() {
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val downloadRiskyPostCodesWorkMock = mockk<DownloadRiskyPostCodesWork>()
    private val appAvailabilityProviderMock = mockk<AppAvailabilityProvider>()

    private val testSubject = DownloadRiskyPostCodesWorker(context, workerParameters).apply {
        downloadRiskyPostCodesWork = downloadRiskyPostCodesWorkMock
        appAvailabilityProvider = appAvailabilityProviderMock
    }

    @Test
    fun `app is not available returns retry`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns false

        val result = testSubject.doWork()

        coVerify(exactly = 0) { downloadRiskyPostCodesWorkMock() }
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `app is available calls downloadRiskyPostCodesWork doWork & tracking`() = runBlocking {
        every { appAvailabilityProviderMock.isAppAvailable() } returns true
        coEvery { downloadRiskyPostCodesWorkMock() } returns Result.success()

        testSubject.doWork()

        coVerify(exactly = 1) { downloadRiskyPostCodesWorkMock() }
    }
}
