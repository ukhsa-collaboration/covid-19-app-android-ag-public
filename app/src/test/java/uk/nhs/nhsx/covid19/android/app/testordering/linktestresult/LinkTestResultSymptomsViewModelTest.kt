package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAskForSymptomsOnPositiveTestEntry
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidHaveSymptomsBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor

class LinkTestResultSymptomsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxed = true)

    private val confirmSymptomsObserver = mockk<Observer<Unit>>(relaxed = true)

    private val testSubject = LinkTestResultSymptomsViewModel(analyticsEventProcessor)

    @Before
    fun setUp() {
        testSubject.confirmSymptoms().observeForever(confirmSymptomsObserver)
    }

    @Test
    fun `onCreate should trigger analytics event DidAskForSymptomsOnPositiveTestEntry`() = runBlocking {
        testSubject.onCreate()

        verify { analyticsEventProcessor.track(DidAskForSymptomsOnPositiveTestEntry) }
        verify(exactly = 0) { confirmSymptomsObserver.onChanged(any()) }
    }

    @Test
    fun `repeatedly calling onCreate should trigger analytics event DidAskForSymptomsOnPositiveTestEntry only once`() = runBlocking {
        testSubject.onCreate()
        testSubject.onCreate()
        testSubject.onCreate()

        verify(exactly = 1) { analyticsEventProcessor.track(DidAskForSymptomsOnPositiveTestEntry) }
        verify(exactly = 0) { confirmSymptomsObserver.onChanged(any()) }
    }

    @Test
    fun `onConfirmSymptomsClicked should trigger analytics event DidHaveSymptomsBeforeReceivedTestResult and emit confirmSymptoms event`() =
        runBlocking {
            testSubject.onConfirmSymptomsClicked()
            verifyOrder {
                analyticsEventProcessor.track(DidHaveSymptomsBeforeReceivedTestResult)
                confirmSymptomsObserver.onChanged(null)
            }
        }
}
