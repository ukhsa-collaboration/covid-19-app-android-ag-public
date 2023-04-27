package uk.nhs.nhsx.covid19.android.app.analytics

import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.testhelpers.coRunWithFeature
import kotlin.test.assertEquals

class SubmitOnboardingAnalyticsWorkerTest : FieldInjectionUnitTest() {

    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val submitOnboardingAnalyticsMock = mockk<SubmitOnboardingAnalytics>()

    private val testSubject = SubmitOnboardingAnalyticsWorker(context, workerParameters).apply {
        submitOnboardingAnalytics = submitOnboardingAnalyticsMock
    }

    @Test
    fun `app is in decommissioning state returns failure`() = runBlocking {
        coRunWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {

            val result = testSubject.doWork()

            coVerify(exactly = 0) { submitOnboardingAnalyticsMock() }
            assertEquals(Result.failure(), result)
        }
    }

    @Test
    fun `app is not in decommissioning state returns result`() = runBlocking {
        coRunWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            coEvery { submitOnboardingAnalyticsMock() } returns Success(Unit)

            testSubject.doWork()

            coVerify(exactly = 1) { submitOnboardingAnalyticsMock() }
        }
    }
}
