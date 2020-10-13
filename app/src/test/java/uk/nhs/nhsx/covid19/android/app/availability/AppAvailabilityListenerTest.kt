package uk.nhs.nhsx.covid19.android.app.availability

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class AppAvailabilityListenerTest {

    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()
    private val appAvailabilityActivity = mockk<AppAvailabilityActivity>()
    private val appCompatActivity = mockk<AppCompatActivity>()
    private val context = mockk<Context>(relaxed = true)

    private val testSubject =
        AppAvailabilityListener(
            appAvailabilityProvider
        )

    @Test
    fun `trigger show availability screen when the current version is not supported`() {
        every { appAvailabilityProvider.isAppAvailable() } returns false
        val triggerResult = testSubject.shouldShowAvailabilityScreen()

        assertEquals(true, triggerResult)
    }

    @Test
    fun `do not trigger show availability screen when the current version is supported`() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        val triggerResult = testSubject.shouldShowAvailabilityScreen()

        assertEquals(false, triggerResult)
    }

    @Test
    fun `start AppAvailabilityActivity when app is not available and current activity is not AppAvailabilityActivity`() {
        every { appAvailabilityProvider.isAppAvailable() } returns false
        every { appCompatActivity.applicationContext } returns context

        testSubject.onActivityResumed(appCompatActivity)

        verify { context.startActivity(any()) }
    }

    @Test
    fun `do not start AppAvailabilityActivity when app is not available and current activity is AppAvailabilityActivity`() {
        every { appAvailabilityProvider.isAppAvailable() } returns false
        every { appAvailabilityActivity.applicationContext } returns context

        testSubject.onActivityResumed(appAvailabilityActivity)

        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `do not start AppAvailabilityActivity when app is available and current activity is AppAvailabilityActivity`() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { appAvailabilityActivity.applicationContext } returns context

        testSubject.onActivityResumed(appAvailabilityActivity)

        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun `do nothing on activity paused`() {
        testSubject.onActivityPaused(appCompatActivity)

        verifyNoActionsPerformed()
    }

    @Test
    fun `do nothing on activity started`() {
        testSubject.onActivityStarted(appCompatActivity)

        verifyNoActionsPerformed()
    }

    @Test
    fun `do nothing on activity destroyed`() {
        testSubject.onActivityDestroyed(appCompatActivity)

        verifyNoActionsPerformed()
    }

    @Test
    fun `do nothing on activity save instance state`() {
        val outState = mockk<Bundle>()
        testSubject.onActivitySaveInstanceState(appCompatActivity, outState)

        verifyNoActionsPerformed()
    }

    @Test
    fun `do nothing on activity stopped`() {
        testSubject.onActivityStopped(appCompatActivity)

        verifyNoActionsPerformed()
    }

    @Test
    fun `do nothing on activity created`() {
        val savedInstanceState = mockk<Bundle>()
        testSubject.onActivityCreated(appCompatActivity, savedInstanceState)

        verifyNoActionsPerformed()
    }

    private fun verifyNoActionsPerformed() {
        verify(exactly = 0) { appAvailabilityProvider.isAppAvailable() }
        verify(exactly = 0) { context.startActivity(any()) }
    }
}
