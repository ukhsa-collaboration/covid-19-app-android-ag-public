package uk.nhs.nhsx.covid19.android.app.status

import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.status.localmessage.DownloadLocalMessagesWork
import uk.nhs.nhsx.covid19.android.app.testhelpers.coRunWithFeature
import kotlin.test.assertEquals

class DownloadAreaInfoWorkerTest : FieldInjectionUnitTest() {
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val downloadRiskyPostCodesWorkMock = mockk<DownloadRiskyPostCodesWork>()
    private val downloadLocalMessagesWorkMock = mockk<DownloadLocalMessagesWork>()
    private val appAvailabilityProviderMock = mockk<AppAvailabilityProvider>()

    private val testSubject = DownloadAreaInfoWorker(context, workerParameters).apply {
        downloadRiskyPostCodesWork = downloadRiskyPostCodesWorkMock
        downloadLocalMessagesWork = downloadLocalMessagesWorkMock
        appAvailabilityProvider = appAvailabilityProviderMock
    }

    @Test
    fun `app is in decommissioning state returns failure`() = runBlocking {
        coRunWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {

            val result = testSubject.doWork()

            coVerify(exactly = 0) { downloadRiskyPostCodesWorkMock() }
            coVerify(exactly = 0) { downloadLocalMessagesWorkMock() }
            assertEquals(Result.failure(), result)
        }
    }

    @Test
    fun `app is not available returns retry`() = runBlocking {
        coRunWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { appAvailabilityProviderMock.isAppAvailable() } returns false

            val result = testSubject.doWork()

            coVerify(exactly = 0) { downloadRiskyPostCodesWorkMock() }
            coVerify(exactly = 0) { downloadLocalMessagesWorkMock() }
            assertEquals(Result.retry(), result)
        }
    }

    @Test
    fun `app is available calls downloadRiskyPostCodesWork doWork & tracking`() = runBlocking {
        coRunWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { appAvailabilityProviderMock.isAppAvailable() } returns true
            coEvery { downloadRiskyPostCodesWorkMock() } returns Result.success()
            coEvery { downloadLocalMessagesWorkMock() } returns Result.success()

            testSubject.doWork()

            coVerify(exactly = 1) { downloadRiskyPostCodesWorkMock() }
            coVerify(exactly = 1) { downloadLocalMessagesWorkMock() }
        }
    }
}
