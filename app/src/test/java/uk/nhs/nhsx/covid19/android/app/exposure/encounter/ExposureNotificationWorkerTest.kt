package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.testhelpers.coRunWithFeature
import kotlin.test.assertEquals

class ExposureNotificationWorkerTest : FieldInjectionUnitTest() {
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val exposureNotificationWorkMock = mockk<ExposureNotificationWork>()
    private val notificationProviderMock = mockk<NotificationProvider>()

    private val testSubject = ExposureNotificationWorker(context, workerParameters).apply {
        exposureNotificationWork = exposureNotificationWorkMock
        notificationProvider = notificationProviderMock
    }

    @Test
    fun `app is in decommissioning state returns failure`() = runBlocking {
        coRunWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {

            val result = testSubject.doWork()

            coVerify(exactly = 0) { exposureNotificationWorkMock.evaluateRisk() }
            coVerify(exactly = 0) { exposureNotificationWorkMock.doNotEvaluateRisk() }
            assertEquals(Result.failure(), result)
        }
    }
}
