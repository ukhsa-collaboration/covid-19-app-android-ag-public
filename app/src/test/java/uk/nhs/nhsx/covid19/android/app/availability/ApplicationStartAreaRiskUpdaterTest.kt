package uk.nhs.nhsx.covid19.android.app.availability

import androidx.work.ListenableWorker.Result.Failure
import androidx.work.ListenableWorker.Result.Success
import com.jeroenmols.featureflag.framework.FeatureFlag.DECOMMISSIONING_CLOSURE_SCREEN
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.status.DownloadRiskyPostCodesWork
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApplicationStartAreaRiskUpdaterTest {

    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>()
    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()
    private val downloadRiskyPostCodesWork = mockk<DownloadRiskyPostCodesWork>()
    private val testCoroutineScope = TestCoroutineScope()
    private val testCoroutineDispatcher = TestCoroutineDispatcher()
    private val fixedClock = mockk<Clock>()
    private val now = Instant.parse("2021-01-01T00:00:00Z")

    private val testSubject = ApplicationStartAreaRiskUpdater(
        onboardingCompletedProvider,
        appAvailabilityProvider,
        downloadRiskyPostCodesWork,
        testCoroutineScope,
        testCoroutineDispatcher,
        fixedClock
    )

    @Before
    fun setUp() {
        every { fixedClock.instant() } returns now
    }

    @Test
    fun `when app is in decommissioning state then app should not update area risk information`() = testCoroutineScope.runBlockingTest {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = true) {
            every { onboardingCompletedProvider.value } returns true
            every { appAvailabilityProvider.isAppAvailable() } returns true

            testSubject.updateIfNecessary()
            every { fixedClock.instant() } returns now.plus(10, ChronoUnit.MINUTES)
            testSubject.updateIfNecessary()

            coVerify(exactly = 0) { downloadRiskyPostCodesWork.invoke() }
        }
    }

    @Test
    fun `when app is not available then app should not update area risk information`() = testCoroutineScope.runBlockingTest {
        runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
            every { onboardingCompletedProvider.value } returns true
            every { appAvailabilityProvider.isAppAvailable() } returns false

            testSubject.updateIfNecessary()
            every { fixedClock.instant() } returns now.plus(10, ChronoUnit.MINUTES)
            testSubject.updateIfNecessary()

            coVerify(exactly = 0) { downloadRiskyPostCodesWork.invoke() }
        }
    }

    @Test
    fun `when onboarding is not completed then app should not update area risk information`() =
        testCoroutineScope.runBlockingTest {
            runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
                every { onboardingCompletedProvider.value } returns false
                every { appAvailabilityProvider.isAppAvailable() } returns true

                testSubject.updateIfNecessary()
                every { fixedClock.instant() } returns now.plus(10, ChronoUnit.MINUTES)
                testSubject.updateIfNecessary()

                coVerify(exactly = 0) { downloadRiskyPostCodesWork.invoke() }
            }
        }

    @Test
    fun `when last updated timestamp is not set then app should update area risk information`() =
        testCoroutineScope.runBlockingTest {
            runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
                every { onboardingCompletedProvider.value } returns true
                every { appAvailabilityProvider.isAppAvailable() } returns true
                coEvery { downloadRiskyPostCodesWork.invoke() } returns Success()

                testSubject.updateIfNecessary()

                coVerify { downloadRiskyPostCodesWork.invoke() }
            }
        }

    @Test
    fun `when area risk information is outdated then app should update area risk information`() =
        testCoroutineScope.runBlockingTest {
            runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
                every { onboardingCompletedProvider.value } returns true
                every { appAvailabilityProvider.isAppAvailable() } returns true
                coEvery { downloadRiskyPostCodesWork.invoke() } returns Success()

                testSubject.updateIfNecessary()
                every { fixedClock.instant() } returns now.plus(10, ChronoUnit.MINUTES)
                testSubject.updateIfNecessary()

                coVerify(exactly = 2) { downloadRiskyPostCodesWork.invoke() }
            }
        }

    @Test
    fun `when area risk information is not outdated then app should not update area risk information`() =
        testCoroutineScope.runBlockingTest {
            runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
                every { onboardingCompletedProvider.value } returns true
                every { appAvailabilityProvider.isAppAvailable() } returns true
                coEvery { downloadRiskyPostCodesWork.invoke() } returns Success()

                testSubject.updateIfNecessary()
                every { fixedClock.instant() } returns now.plus(9, ChronoUnit.MINUTES)
                testSubject.updateIfNecessary()

                coVerify(exactly = 1) { downloadRiskyPostCodesWork.invoke() }
            }
        }

    @Test
    fun `when updating area risk information fails last updated timestamp is not updated`() =
        testCoroutineScope.runBlockingTest {
            runWithFeature(DECOMMISSIONING_CLOSURE_SCREEN, enabled = false) {
                every { onboardingCompletedProvider.value } returns true
                every { appAvailabilityProvider.isAppAvailable() } returns true
                coEvery { downloadRiskyPostCodesWork.invoke() } returns Failure()

                testSubject.updateIfNecessary()

                assertNull(testSubject.lastUpdated)

                coEvery { downloadRiskyPostCodesWork.invoke() } returns Success()

                testSubject.updateIfNecessary()

                assertEquals(expected = now, actual = testSubject.lastUpdated)

                coVerify(exactly = 2) { downloadRiskyPostCodesWork.invoke() }
            }
        }
}
