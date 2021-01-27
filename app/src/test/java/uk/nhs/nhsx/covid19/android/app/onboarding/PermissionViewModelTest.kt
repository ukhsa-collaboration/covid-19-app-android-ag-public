package uk.nhs.nhsx.covid19.android.app.onboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.SubmitOnboardingAnalyticsWorker
import uk.nhs.nhsx.covid19.android.app.analytics.SubmittedOnboardingAnalyticsProvider
import uk.nhs.nhsx.covid19.android.app.battery.BatteryOptimizationRequired
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTasks
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.BATTERY_OPTIMIZATION
import uk.nhs.nhsx.covid19.android.app.onboarding.PermissionViewModel.NavigationTarget.STATUS_ACTIVITY

class PermissionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>(relaxed = true)
    private val submitAnalyticsWorkerScheduler =
        mockk<SubmitOnboardingAnalyticsWorker.Scheduler>(relaxed = true)
    private val periodicTasks = mockk<PeriodicTasks>(relaxed = true)
    private val batteryOptimizationRequired = mockk<BatteryOptimizationRequired>()
    private val submittedOnboardingAnalyticsProvider = mockk<SubmittedOnboardingAnalyticsProvider>(relaxed = true)

    private val testSubject =
        PermissionViewModel(
            onboardingCompletedProvider,
            submitAnalyticsWorkerScheduler,
            periodicTasks,
            batteryOptimizationRequired,
            submittedOnboardingAnalyticsProvider
        )

    private val onActivityNavigationObserver = mockk<Observer<NavigationTarget>>(relaxed = true)

    @Test
    fun `on activity navigation and battery optimization required should fire battery optimization and send onboarding analytics`() {
        testSubject.onActivityNavigation().observeForever(onActivityNavigationObserver)

        every { batteryOptimizationRequired() } returns true
        every { submittedOnboardingAnalyticsProvider.value } returns null

        testSubject.onExposureNotificationsActive()

        verify { onboardingCompletedProvider setProperty "value" value eq(true) }
        verify { submitAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent() }
        verify { submittedOnboardingAnalyticsProvider setProperty "value" value eq(true) }
        verify { periodicTasks.schedule() }
        verify { onActivityNavigationObserver.onChanged(BATTERY_OPTIMIZATION) }
    }

    @Test
    fun `on activity navigation and battery optimization required should fire battery optimization without sending onboarding analytics`() {
        testSubject.onActivityNavigation().observeForever(onActivityNavigationObserver)

        every { batteryOptimizationRequired() } returns true
        every { submittedOnboardingAnalyticsProvider.value } returns true

        testSubject.onExposureNotificationsActive()

        verify { onboardingCompletedProvider setProperty "value" value eq(true) }
        verify(exactly = 0) { submitAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent() }
        verify { periodicTasks.schedule() }
        verify { onActivityNavigationObserver.onChanged(BATTERY_OPTIMIZATION) }
    }

    @Test
    fun `on activity navigation and battery optimization not required should fire status activity without sending onboarding analytics`() {
        testSubject.onActivityNavigation().observeForever(onActivityNavigationObserver)

        every { batteryOptimizationRequired() } returns false
        every { submittedOnboardingAnalyticsProvider.value } returns true

        testSubject.onExposureNotificationsActive()

        verify { onboardingCompletedProvider setProperty "value" value eq(true) }
        verify(exactly = 0) { submitAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent() }
        verify { periodicTasks.schedule() }
        verify { onActivityNavigationObserver.onChanged(STATUS_ACTIVITY) }
    }

    @Test
    fun `on activity navigation and battery optimization not required should fire status activity and send onboarding analytics`() {
        testSubject.onActivityNavigation().observeForever(onActivityNavigationObserver)

        every { batteryOptimizationRequired() } returns false
        every { submittedOnboardingAnalyticsProvider.value } returns false

        testSubject.onExposureNotificationsActive()

        verify { onboardingCompletedProvider setProperty "value" value eq(true) }
        verify { submitAnalyticsWorkerScheduler.scheduleOnboardingAnalyticsEvent() }
        verify { submittedOnboardingAnalyticsProvider setProperty "value" value eq(true) }
        verify { periodicTasks.schedule() }
        verify { onActivityNavigationObserver.onChanged(STATUS_ACTIVITY) }
    }
}
