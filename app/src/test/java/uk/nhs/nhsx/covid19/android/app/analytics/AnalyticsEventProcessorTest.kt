package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import java.time.Instant
import java.time.LocalDate

class AnalyticsEventProcessorTest {

    private val analyticsMetricsStorage = mockk<AnalyticsMetricsStorage>(relaxed = true)
    private val stateStorage = mockk<StateStorage>(relaxed = true)
    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()
    private val testSubject = AnalyticsEventProcessor(
        analyticsMetricsStorage,
        stateStorage,
        exposureNotificationApi,
        appAvailabilityProvider
    )

    @Before
    fun setUp() {
        every { analyticsMetricsStorage.metrics } returns Metrics()
    }

    @Test
    fun `on background completed updates total background tasks count when app is not available`() =
        runBlocking {
            every { appAvailabilityProvider.isAppAvailable() } returns false
            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsStorage.metrics = Metrics().apply {
                    totalBackgroundTasks = 1
                }
            }
        }

    @Test
    fun `on background completed updates running normally background tick when app is available`() =
        runBlocking {
            every { appAvailabilityProvider.isAppAvailable() } returns true
            coEvery { exposureNotificationApi.isEnabled() } returns false
            every { stateStorage.state } returns Default()

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsStorage.metrics = Metrics().apply {
                    runningNormallyBackgroundTick = 1
                    encounterDetectionPausedBackgroundTick = 1
                    totalBackgroundTasks = 1
                }
            }
        }

    @Test
    fun `on background completed updates risky contact tick when app is available`() = runBlocking {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        coEvery { exposureNotificationApi.isEnabled() } returns false
        every { stateStorage.state } returns Default()
        every { stateStorage.getHistory() } returns listOf(
            Isolation(
                isolationStart = Instant.now(),
                expiryDate = LocalDate.of(2020, 10, 10),
                indexCase = IndexCase(
                    symptomsOnsetDate = LocalDate.of(2020, 10, 1)
                )
            )
        )

        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsMetricsStorage.metrics = Metrics().apply {
                runningNormallyBackgroundTick = 1
                encounterDetectionPausedBackgroundTick = 1
                totalBackgroundTasks = 1
                hasSelfDiagnosedPositiveBackgroundTick = 1
            }
        }
    }
}
