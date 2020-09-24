package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OnboardingCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
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
        every { appAvailabilityProvider.isAppAvailable() } returns true
        coEvery { exposureNotificationApi.isEnabled() } returns true
        every { stateStorage.state } returns Default()
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
            coEvery { exposureNotificationApi.isEnabled() } returns false

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
    fun `on background completed updates self-diagnosed contact tick when app is available`() =
        runBlocking {
            coEvery { exposureNotificationApi.isEnabled() } returns false
            every { stateStorage.state } returns
                Isolation(
                    isolationStart = Instant.now(),
                    isolationConfiguration = DurationDays(),
                    indexCase = IndexCase(
                        symptomsOnsetDate = LocalDate.of(2018, 10, 1),
                        expiryDate = LocalDate.of(2018, 10, 10)
                    )
                )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsStorage.metrics = Metrics().apply {
                    runningNormallyBackgroundTick = 1
                    isIsolatingBackgroundTick = 1
                    hasHadRiskyContactBackgroundTick = 0
                    hasSelfDiagnosedPositiveBackgroundTick = 1
                    encounterDetectionPausedBackgroundTick = 1
                    totalBackgroundTasks = 1
                }
            }
        }

    @Test
    fun `on background completed updates risky contact tick when app is available`() = runBlocking {
        coEvery { exposureNotificationApi.isEnabled() } returns false

        every { stateStorage.state } returns
            Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(Instant.now(), expiryDate = LocalDate.of(2018, 10, 10))
            )

        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsMetricsStorage.metrics = Metrics().apply {
                runningNormallyBackgroundTick = 1
                isIsolatingBackgroundTick = 1
                hasHadRiskyContactBackgroundTick = 1
                hasSelfDiagnosedPositiveBackgroundTick = 0
                encounterDetectionPausedBackgroundTick = 1
                totalBackgroundTasks = 1
            }
        }
    }

    @Test
    fun `on background completed updates only total tasks and running normally ticks when app is available`() =
        runBlocking {
            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsStorage.metrics = Metrics().apply {
                    runningNormallyBackgroundTick = 1
                    isIsolatingBackgroundTick = 0
                    hasHadRiskyContactBackgroundTick = 0
                    hasSelfDiagnosedPositiveBackgroundTick = 0
                    encounterDetectionPausedBackgroundTick = 0
                    totalBackgroundTasks = 1
                }
            }
        }

    @Test
    fun `on background completed updates isolating background tick when app is available`() =
        runBlocking {
            every { stateStorage.state } returns Isolation(
                isolationStart = Instant.now(),
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    startDate = Instant.now(),
                    expiryDate = LocalDate.of(2018, 10, 10)
                )
            )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsStorage.metrics = Metrics().apply {
                    runningNormallyBackgroundTick = 1
                    isIsolatingBackgroundTick = 1
                    hasHadRiskyContactBackgroundTick = 1
                    hasSelfDiagnosedPositiveBackgroundTick = 0
                    encounterDetectionPausedBackgroundTick = 0
                    totalBackgroundTasks = 1
                }
            }
        }

    @Test
    fun `track onboarding completion`() = runBlocking {
        testSubject.track(OnboardingCompletion)

        verify {
            analyticsMetricsStorage.metrics = Metrics().apply {
                completedOnboarding = 1
            }
        }
    }

    @Test
    fun `track qr code check in`() = runBlocking {
        testSubject.track(QrCodeCheckIn)

        verify {
            analyticsMetricsStorage.metrics = Metrics().apply {
                checkedIn = 1
            }
        }
    }

    @Test
    fun `track cancelled check in`() = runBlocking {
        testSubject.track(CanceledCheckIn)

        verify {
            analyticsMetricsStorage.metrics = Metrics().apply {
                canceledCheckIn = 1
            }
        }
    }

    @Test
    fun `track completed questionnaire and started isolation`() = runBlocking {
        testSubject.track(CompletedQuestionnaireAndStartedIsolation)

        verify {
            analyticsMetricsStorage.metrics = Metrics().apply {
                completedQuestionnaireAndStartedIsolation = 1
            }
        }
    }

    @Test
    fun `track completed questionnaire but did not start isolation`() = runBlocking {
        testSubject.track(CompletedQuestionnaireButDidNotStartIsolation)

        verify {
            analyticsMetricsStorage.metrics = Metrics().apply {
                completedQuestionnaireButDidNotStartIsolation = 1
            }
        }
    }

    @Test
    fun `track positive result received`() = runBlocking {
        testSubject.track(PositiveResultReceived)

        verify {
            analyticsMetricsStorage.metrics = Metrics().apply {
                receivedPositiveTestResult = 1
            }
        }
    }

    @Test
    fun `track negative result received`() = runBlocking {
        testSubject.track(NegativeResultReceived)

        verify {
            analyticsMetricsStorage.metrics = Metrics().apply {
                receivedNegativeTestResult = 1
            }
        }
    }

    @Test
    fun `track void result received`() = runBlocking {
        testSubject.track(VoidResultReceived)

        verify {
            analyticsMetricsStorage.metrics = Metrics().apply {
                receivedVoidTestResult = 1
            }
        }
    }
}
