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
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AnalyticsEventProcessorTest {

    private val analyticsMetricsLogStorage = mockk<AnalyticsMetricsLogStorage>(relaxed = true)
    private val stateStorage = mockk<StateStorage>(relaxed = true)
    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()
    private val networkTrafficStats = mockk<NetworkTrafficStats>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = AnalyticsEventProcessor(
        analyticsMetricsLogStorage,
        stateStorage,
        exposureNotificationApi,
        appAvailabilityProvider,
        networkTrafficStats,
        fixedClock
    )

    @Before
    fun setUp() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        coEvery { exposureNotificationApi.isEnabled() } returns true
        every { stateStorage.state } returns Default()
    }

    @Test
    fun `on background completed updates total background tasks count when app is not available`() =
        runBlocking {
            every { appAvailabilityProvider.isAppAvailable() } returns false
            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed updates running normally background tick when app is available`() =
        runBlocking {
            coEvery { exposureNotificationApi.isEnabled() } returns false

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            encounterDetectionPausedBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
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
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            isIsolatingBackgroundTick = 1
                            hasHadRiskyContactBackgroundTick = 0
                            hasSelfDiagnosedPositiveBackgroundTick = 1
                            encounterDetectionPausedBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
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
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        runningNormallyBackgroundTick = 1
                        isIsolatingBackgroundTick = 1
                        hasHadRiskyContactBackgroundTick = 1
                        hasSelfDiagnosedPositiveBackgroundTick = 0
                        encounterDetectionPausedBackgroundTick = 1
                        totalBackgroundTasks = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `on background completed updates only total tasks and running normally ticks when app is available`() =
        runBlocking {
            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            isIsolatingBackgroundTick = 0
                            hasHadRiskyContactBackgroundTick = 0
                            hasSelfDiagnosedPositiveBackgroundTick = 0
                            encounterDetectionPausedBackgroundTick = 0
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
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
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            isIsolatingBackgroundTick = 1
                            hasHadRiskyContactBackgroundTick = 1
                            hasSelfDiagnosedPositiveBackgroundTick = 0
                            encounterDetectionPausedBackgroundTick = 0
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `track onboarding completion`() = runBlocking {
        testSubject.track(OnboardingCompletion)

        verify {
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        completedOnboarding = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `track qr code check in`() = runBlocking {
        testSubject.track(QrCodeCheckIn)

        verify {
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        checkedIn = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `track cancelled check in`() = runBlocking {
        testSubject.track(CanceledCheckIn)

        verify {
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        canceledCheckIn = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `track completed questionnaire and started isolation`() = runBlocking {
        testSubject.track(CompletedQuestionnaireAndStartedIsolation)

        verify {
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        completedQuestionnaireAndStartedIsolation = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `track completed questionnaire but did not start isolation`() = runBlocking {
        testSubject.track(CompletedQuestionnaireButDidNotStartIsolation)

        verify {
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        completedQuestionnaireButDidNotStartIsolation = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `track positive result received`() = runBlocking {
        testSubject.track(PositiveResultReceived)

        verify {
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        receivedPositiveTestResult = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `track negative result received`() = runBlocking {
        testSubject.track(NegativeResultReceived)

        verify {
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        receivedNegativeTestResult = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `track void result received`() = runBlocking {
        testSubject.track(VoidResultReceived)

        verify {
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        receivedVoidTestResult = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `on network stats update`() = runBlocking {
        every { networkTrafficStats.getTotalBytesUploaded() } returns 15
        every { networkTrafficStats.getTotalBytesDownloaded() } returns 25

        testSubject.track(UpdateNetworkStats)

        verify {
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        cumulativeDownloadBytes = 25
                        cumulativeUploadBytes = 15
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }
}
