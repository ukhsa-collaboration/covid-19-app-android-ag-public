package uk.nhs.nhsx.covid19.android.app.availability

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals

class AppAvailabilityListenerTest {

    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>(relaxed = true)
    private val appAvailabilityActivity = mockk<AppAvailabilityActivity>()
    private val appCompatActivity = mockk<AppCompatActivity>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val fixedInstant = Instant.parse("2020-05-21T10:00:00Z")

    private val clock = mockk<Clock>()

    private val testSubject =
        AppAvailabilityListener(
            appAvailabilityProvider,
            clock
        )

    @Before
    fun setUp() {
        every { clock.instant() } returns fixedInstant
    }

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
    fun `start UpdateRecommendedActivity when recommended app is available`() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { appAvailabilityProvider.isUpdateRecommended() } returns true

        testSubject.onActivityResumed(appCompatActivity)

        verify { appCompatActivity.startActivity(any()) }
    }

    @Test
    fun `do not start UpdateRecommendedActivity when no update available`() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { appAvailabilityProvider.isUpdateRecommended() } returns false

        testSubject.onActivityResumed(appCompatActivity)

        verify(exactly = 0) { appCompatActivity.startActivity(any()) }
    }

    @Test
    fun `should start UpdateRecommendedActivity when over 5 minutes outside app`() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { appAvailabilityProvider.isUpdateRecommended() } returns true
        testSubject.onActivityPaused(appCompatActivity)

        every { clock.instant() } returns fixedInstant.plus(8, ChronoUnit.MINUTES)
        testSubject.onActivityResumed(appCompatActivity)

        verify { appCompatActivity.startActivity(any()) }
    }

    @Test
    fun `should not start UpdateRecommendedActivity when less than 5 minutes outside app`() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { appAvailabilityProvider.isUpdateRecommended() } returns true
        testSubject.onActivityPaused(appCompatActivity)

        every { clock.instant() } returns fixedInstant.plus(2, ChronoUnit.MINUTES)
        testSubject.onActivityResumed(appCompatActivity)

        verify(exactly = 0) { appCompatActivity.startActivity(any()) }
    }

    @Test
    fun `should not start UpdateRecommendedActivity when app is in foreground`() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { appAvailabilityProvider.isUpdateRecommended() } returns false
        testSubject.onActivityResumed(appCompatActivity)

        every { appAvailabilityProvider.isUpdateRecommended() } returns true
        testSubject.onActivityResumed(appCompatActivity)

        verify(exactly = 0) { appCompatActivity.startActivity(any()) }
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
