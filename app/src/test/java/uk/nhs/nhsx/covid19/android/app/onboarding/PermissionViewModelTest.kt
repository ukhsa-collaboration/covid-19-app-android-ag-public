package uk.nhs.nhsx.covid19.android.app.onboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OnboardingCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor

class PermissionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>(relaxed = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxed = true)

    private val testSubject =
        PermissionViewModel(onboardingCompletedProvider, analyticsEventProcessor)

    private val onboardingCompletedObserver = mockk<Observer<Unit>>(relaxed = true)

    @Test
    fun `onboarding completed`() = runBlocking {
        testSubject.onboardingCompleted().observeForever(onboardingCompletedObserver)

        testSubject.setOnboardingCompleted()

        verify { onboardingCompletedProvider setProperty "value" value eq(true) }
        coVerify { analyticsEventProcessor.track(OnboardingCompletion) }
        verify { onboardingCompletedObserver.onChanged(any()) }
    }
}
