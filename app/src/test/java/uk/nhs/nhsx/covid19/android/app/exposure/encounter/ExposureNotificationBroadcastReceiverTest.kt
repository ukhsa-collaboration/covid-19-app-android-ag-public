package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient.ACTION_EXPOSURE_NOT_FOUND
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import uk.nhs.nhsx.covid19.android.app.FieldInjectionUnitTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature

class ExposureNotificationBroadcastReceiverTest : FieldInjectionUnitTest() {

    private val testSubject = ExposureNotificationBroadcastReceiver().apply {
        exposureNotificationWorkerScheduler = mockk(relaxed = true)
        isolationStateMachine = mockk(relaxed = true)
    }

    private val intent = mockk<Intent>(relaxed = true)

    @Before
    override fun setUp() {
        super.setUp()
        every { testSubject.exposureNotificationWorkerScheduler.scheduleEvaluateRisk(context) } returns Unit
        every { testSubject.exposureNotificationWorkerScheduler.scheduleDoNotEvaluateRisk(context) } returns Unit
    }

    @Test
    fun `schedules evaluate risk if a match is found and interested in exposure notifications`() = runBlocking {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, false) {
            every { testSubject.isolationStateMachine.isInterestedInExposureNotifications() } returns true
            every { intent.action } returns ACTION_EXPOSURE_STATE_UPDATED

            testSubject.onReceive(context, intent)

            verify { testSubject.exposureNotificationWorkerScheduler.scheduleEvaluateRisk(context) }
            verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleDoNotEvaluateRisk(context) }
        }
    }

    @Test
    fun `schedules do not evaluate risk if a match is found but not interested in exposure notifications`() = runBlocking {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, false) {
            every { testSubject.isolationStateMachine.isInterestedInExposureNotifications() } returns false
            every { intent.action } returns ACTION_EXPOSURE_STATE_UPDATED

            testSubject.onReceive(context, intent)

            verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleEvaluateRisk(context) }
            verify { testSubject.exposureNotificationWorkerScheduler.scheduleDoNotEvaluateRisk(context) }
        }
    }

    @Test
    fun `schedules evaluate risk if no match is found but interested in exposure notifications`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, false) {
            every { testSubject.isolationStateMachine.isInterestedInExposureNotifications() } returns true
            every { intent.action } returns ACTION_EXPOSURE_NOT_FOUND

            testSubject.onReceive(context, intent)

            verify { testSubject.exposureNotificationWorkerScheduler.scheduleEvaluateRisk(context) }
            verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleDoNotEvaluateRisk(context) }
        }
    }

    @Test
    fun `schedules do not evaluate risk if no match is found and not interested in exposure notifications`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, false) {
            every { testSubject.isolationStateMachine.isInterestedInExposureNotifications() } returns false
            every { intent.action } returns ACTION_EXPOSURE_NOT_FOUND

            testSubject.onReceive(context, intent)

            verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleEvaluateRisk(context) }
            verify { testSubject.exposureNotificationWorkerScheduler.scheduleDoNotEvaluateRisk(context) }
        }
    }

    @Test
    fun `skips on wrong action`() = runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, false) {
        every { intent.action } returns any()

        testSubject.onReceive(context, intent)

        verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleEvaluateRisk(context) }
        verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleDoNotEvaluateRisk(context) }
    }

    @Test
    fun `skips when in decommissioning mode`() = runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, true) {
        testSubject.onReceive(context, intent)

        verify(exactly = 0) { intent.action }
        verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleEvaluateRisk(context) }
        verify(exactly = 0) { testSubject.exposureNotificationWorkerScheduler.scheduleDoNotEvaluateRisk(context) }
    }
}
