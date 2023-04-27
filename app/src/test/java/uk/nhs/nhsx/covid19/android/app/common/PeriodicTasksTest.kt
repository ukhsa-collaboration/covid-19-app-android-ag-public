package uk.nhs.nhsx.covid19.android.app.common

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.WorkManager
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.DecommissioningNotificationSentProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature

class PeriodicTasksTest {

    private val context = mockk<Context>()
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val decommissioningNotificationSentProvider = mockk<DecommissioningNotificationSentProvider>(relaxUnitFun = true)
    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val testCoroutineScope = TestCoroutineScope()

    private val testSubject = PeriodicTasks(context, decommissioningNotificationSentProvider, exposureNotificationApi, testCoroutineScope)

    @Before
    fun setUp() {
        mockkStatic(WorkManager::class)
    }

    @Test
    fun `schedule periodic tasks cancels legacy work in decommissioning mode when notification has not been sent`() = runBlocking {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {
            every { WorkManager.getInstance(context) } returns workManager
            every { decommissioningNotificationSentProvider.value } returns null
            coEvery { exposureNotificationApi.stop() } returns Unit

            testSubject.schedule()

            verify(exactly = 9) { workManager.cancelUniqueWork(any()) }
            verify(exactly = 0) { workManager.enqueueUniquePeriodicWork(PeriodicTask.PERIODIC_TASKS.workName, REPLACE, any()) }
            coVerify(exactly = 1) { exposureNotificationApi.stop() }
        }
    }

    @Test
    fun `schedule periodic tasks cancels legacy work and periodic task in decommissioning mode when notification has been sent`() = runBlocking {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {
            every { WorkManager.getInstance(context) } returns workManager
            every { decommissioningNotificationSentProvider.value } returns true
            coEvery { exposureNotificationApi.stop() } returns Unit

            testSubject.schedule()

            verify(exactly = 11) { workManager.cancelUniqueWork(any()) }
            verify(exactly = 0) { workManager.enqueueUniquePeriodicWork(PeriodicTask.PERIODIC_TASKS.workName, REPLACE, any()) }
            coVerify(exactly = 1) { exposureNotificationApi.stop() }
        }
    }

    @Test
    fun `schedule periodic tasks cancels legacy work and schedules periodic tasks in non decommissioning mode`() = runBlocking {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { WorkManager.getInstance(context) } returns workManager

            testSubject.schedule()

            verify(exactly = 9) { workManager.cancelUniqueWork(any()) }
            verify { workManager.enqueueUniquePeriodicWork(PeriodicTask.PERIODIC_TASKS.workName, REPLACE, any()) }
            coVerify(exactly = 0) { exposureNotificationApi.stop() }
        }
    }
}
