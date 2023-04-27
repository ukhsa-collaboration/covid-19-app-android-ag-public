package uk.nhs.nhsx.covid19.android.app.onboarding

import android.app.Activity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitOnboardingAnalyticsWorker
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationRequired
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationPermissionHelper
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.BatteryOptimization
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.EnableExposureNotifications
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.Status
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Error
import uk.nhs.nhsx.covid19.android.app.status.StatusViewModel.PermissionRequestResult.Request
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature

class PermissionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>(relaxUnitFun = true)
    private val submitAnalyticsWorkerScheduler = mockk<SubmitOnboardingAnalyticsWorker.Scheduler>(relaxUnitFun = true)
    private val periodicTasks = mockk<PeriodicTasks>(relaxUnitFun = true)
    private val batteryOptimizationRequired = mockk<BatteryOptimizationRequired>()
    private val submittedOnboardingAnalyticsProvider = mockk<SubmittedOnboardingAnalyticsProvider>(relaxUnitFun = true)
    private val exposureNotificationPermissionHelperFactory = mockk<ExposureNotificationPermissionHelper.Factory>()
    private val exposureNotificationPermissionHelper = mockk<ExposureNotificationPermissionHelper>(relaxUnitFun = true)
    private val permissionRequestObserver = mockk<Observer<PermissionRequestResult>>(relaxUnitFun = true)
    private val navigationTargetObserver = mockk<Observer<NavigationTarget>>(relaxUnitFun = true)

    private lateinit var testSubject: PermissionViewModel

    @Before
    fun setUp() {
        every { exposureNotificationPermissionHelperFactory.create(any(), any()) } returns
            exposureNotificationPermissionHelper
        testSubject = createTestSubject()
        testSubject.permissionRequest().observeForever(permissionRequestObserver)
        testSubject.navigationTarget().observeForever(navigationTargetObserver)
    }

    @Test
    fun `when exposure notifications successfully enabled and battery optimization required should fire battery optimization and send onboarding analytics`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { batteryOptimizationRequired() } returns true
            every { submittedOnboardingAnalyticsProvider.value } returns null

            testSubject.onExposureNotificationsEnabled()

            verify { onboardingCompletedProvider setProperty "value" value eq(true) }
            verify { submitAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent() }
            verify { submittedOnboardingAnalyticsProvider setProperty "value" value eq(true) }
            verify { periodicTasks.schedule() }
            verify { navigationTargetObserver.onChanged(BatteryOptimization) }
        }
    }

    @Test
    fun `when exposure notifications successfully enabled and battery optimization required should fire battery optimization without sending onboarding analytics`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { batteryOptimizationRequired() } returns true
            every { submittedOnboardingAnalyticsProvider.value } returns true

            testSubject.onExposureNotificationsEnabled()

            verify { onboardingCompletedProvider setProperty "value" value eq(true) }
            verify(exactly = 0) { submitAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent() }
            verify { periodicTasks.schedule() }
            verify { navigationTargetObserver.onChanged(BatteryOptimization) }
        }
    }

    @Test
    fun `when exposure notifications successfully enabled and battery optimization not required should fire status activity without sending onboarding analytics`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { batteryOptimizationRequired() } returns false
            every { submittedOnboardingAnalyticsProvider.value } returns true

            testSubject.onExposureNotificationsEnabled()

            verify { onboardingCompletedProvider setProperty "value" value eq(true) }
            verify(exactly = 0) { submitAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent() }
            verify { periodicTasks.schedule() }
            verify { navigationTargetObserver.onChanged(Status) }
        }
    }

    @Test
    fun `when exposure notifications successfully enabled and battery optimization not required should fire status activity and send onboarding analytics`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { batteryOptimizationRequired() } returns false
            every { submittedOnboardingAnalyticsProvider.value } returns false

            testSubject.onExposureNotificationsEnabled()

            verify { onboardingCompletedProvider setProperty "value" value eq(true) }
            verify { submitAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent() }
            verify { submittedOnboardingAnalyticsProvider setProperty "value" value eq(true) }
            verify { periodicTasks.schedule() }
            verify { navigationTargetObserver.onChanged(Status) }
        }
    }

    @Test
    fun `when exposure notifications successfully enabled in decommission state and onboarding analytics has not been sent should not send onboarding analytics`() {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {
            every { batteryOptimizationRequired() } returns false
            every { submittedOnboardingAnalyticsProvider.value } returns false

            testSubject.onExposureNotificationsEnabled()

            verify { onboardingCompletedProvider setProperty "value" value eq(true) }
            verify(exactly = 0) { submitAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent() }
            verify { periodicTasks.schedule() }
            verify { navigationTargetObserver.onChanged(Status) }
        }
    }

    @Test
    fun `onPermissionRequired passes permission request to activity`() {
        val expectedPermissionRequest = mockk<(Activity) -> Unit>()
        testSubject.onPermissionRequired(expectedPermissionRequest)

        verify { permissionRequestObserver.onChanged(Request(expectedPermissionRequest)) }
    }

    @Test
    fun `onPermissionDenied emits navigation target EnableExposureNotifications`() {
        testSubject.onPermissionDenied()

        verify { navigationTargetObserver.onChanged(EnableExposureNotifications) }
    }

    @Test
    fun `when exposure notifications activation results in an error then emit contents of error message`() {
        val throwable = mockk<Throwable>()
        val expectedMessage = "Test"
        every { throwable.message } returns expectedMessage

        testSubject.onError(throwable)

        verify { permissionRequestObserver.onChanged(Error(expectedMessage)) }
    }

    @Test
    fun `when continue button is clicked then exposure notification activation is started`() {
        testSubject.onContinueButtonClicked()

        verify { exposureNotificationPermissionHelper.startExposureNotifications() }
    }

    @Test
    fun `onActivityResult delegates call to ExposureNotificationPermissionHelper`() {
        val expectedRequestCode = 123
        val expectedResultCode = Activity.RESULT_OK

        testSubject.onActivityResult(expectedRequestCode, expectedResultCode)

        verify { exposureNotificationPermissionHelper.onActivityResult(expectedRequestCode, expectedResultCode) }
    }

    private fun createTestSubject() = PermissionViewModel(
        onboardingCompletedProvider,
        submitAnalyticsWorkerScheduler,
        periodicTasks,
        batteryOptimizationRequired,
        submittedOnboardingAnalyticsProvider,
        exposureNotificationPermissionHelperFactory
    )
}
