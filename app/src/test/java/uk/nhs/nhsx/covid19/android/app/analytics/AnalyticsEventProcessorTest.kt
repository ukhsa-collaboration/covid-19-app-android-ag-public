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
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.State.Default
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.ContactCase
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.testordering.ReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.TestResultsProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class AnalyticsEventProcessorTest {

    private val analyticsMetricsLogStorage = mockk<AnalyticsMetricsLogStorage>(relaxed = true)
    private val stateStorage = mockk<StateStorage>(relaxed = true)
    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()
    private val networkTrafficStats = mockk<NetworkTrafficStats>()
    private val testResultsProvider = mockk<TestResultsProvider>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = AnalyticsEventProcessor(
        analyticsMetricsLogStorage,
        stateStorage,
        exposureNotificationApi,
        appAvailabilityProvider,
        networkTrafficStats,
        testResultsProvider,
        fixedClock
    )

    @Before
    fun setUp() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { stateStorage.state } returns Default()
        every { testResultsProvider.testResults.values } returns emptyList()
        coEvery { exposureNotificationApi.isEnabled() } returns true
    }

    @Test
    fun `on background completed when app is not available`() =
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
    fun `on background completed when app is available`() =
        runBlocking {
            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to contact`() = runBlocking {
        every { stateStorage.state } returns
            Isolation(
                isolationStart = Instant.now(fixedClock),
                isolationConfiguration = DurationDays(),
                contactCase = ContactCase(
                    startDate = Instant.now(fixedClock),
                    notificationDate = Instant.now(fixedClock),
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
                        isIsolatingForHadRiskyContactBackgroundTick = 1
                        hasHadRiskyContactBackgroundTick = 1
                        totalBackgroundTasks = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `on background completed when user was isolating due to contact`() = runBlocking {
        every { stateStorage.state } returns
            Default(
                previousIsolation = Isolation(
                    isolationStart = Instant.now(fixedClock),
                    isolationConfiguration = DurationDays(),
                    contactCase = ContactCase(
                        startDate = Instant.now(fixedClock),
                        notificationDate = Instant.now(fixedClock),
                        expiryDate = LocalDate.of(2018, 10, 10)
                    )
                )
            )

        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsMetricsLogStorage.add(
                MetricsLogEntry(
                    Metrics().apply {
                        runningNormallyBackgroundTick = 1
                        hasHadRiskyContactBackgroundTick = 1
                        totalBackgroundTasks = 1
                    },
                    Instant.now(fixedClock)
                )
            )
        }
    }

    @Test
    fun `on background completed when user is isolating due to self assessment`() =
        runBlocking {
            every { stateStorage.state } returns
                Isolation(
                    isolationStart = Instant.now(fixedClock),
                    isolationConfiguration = DurationDays(),
                    indexCase = IndexCase(
                        symptomsOnsetDate = LocalDate.of(2018, 10, 1),
                        expiryDate = LocalDate.of(2018, 10, 10),
                        selfAssessment = true
                    )
                )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            isIsolatingBackgroundTick = 1
                            hasSelfDiagnosedPositiveBackgroundTick = 1
                            isIsolatingForSelfDiagnosedBackgroundTick = 1
                            hasSelfDiagnosedBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to self assessment and contact`() =
        runBlocking {
            every { stateStorage.state } returns
                Isolation(
                    isolationStart = Instant.now(fixedClock),
                    isolationConfiguration = DurationDays(),
                    contactCase = ContactCase(
                        startDate = Instant.now(fixedClock),
                        notificationDate = Instant.now(fixedClock),
                        expiryDate = LocalDate.of(2018, 10, 10)
                    ),
                    indexCase = IndexCase(
                        symptomsOnsetDate = LocalDate.of(2018, 10, 1),
                        expiryDate = LocalDate.of(2018, 10, 10),
                        selfAssessment = true
                    )
                )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            isIsolatingBackgroundTick = 1
                            isIsolatingForHadRiskyContactBackgroundTick = 1
                            hasHadRiskyContactBackgroundTick = 1
                            isIsolatingForSelfDiagnosedBackgroundTick = 1
                            hasSelfDiagnosedPositiveBackgroundTick = 1
                            hasSelfDiagnosedBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed when user was isolating due to self assessment`() =
        runBlocking {
            every { stateStorage.state } returns
                Default(
                    previousIsolation = Isolation(
                        isolationStart = Instant.now(fixedClock),
                        isolationConfiguration = DurationDays(),
                        indexCase = IndexCase(
                            symptomsOnsetDate = LocalDate.of(2018, 10, 1),
                            expiryDate = LocalDate.of(2018, 10, 10),
                            selfAssessment = true
                        )
                    )
                )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            hasSelfDiagnosedBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to self assessment and last test result is positive and acknowledged during current isolation`() =
        runBlocking {
            every { stateStorage.state } returns
                Isolation(
                    isolationStart = Instant.now(fixedClock).minus(2, ChronoUnit.DAYS),
                    isolationConfiguration = DurationDays(),
                    indexCase = IndexCase(
                        symptomsOnsetDate = LocalDate.of(2018, 10, 1),
                        expiryDate = LocalDate.of(2018, 10, 10),
                        selfAssessment = true
                    )
                )
            every { testResultsProvider.testResults.values } returns listOf(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now(fixedClock),
                    acknowledgedDate = Instant.now(fixedClock).minus(1, ChronoUnit.DAYS),
                    testResult = POSITIVE
                )
            )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            isIsolatingBackgroundTick = 1
                            hasSelfDiagnosedPositiveBackgroundTick = 1
                            isIsolatingForSelfDiagnosedBackgroundTick = 1
                            isIsolatingForTestedPositiveBackgroundTick = 1
                            hasSelfDiagnosedBackgroundTick = 1
                            hasTestedPositiveBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to self assessment and last test result is positive and acknowledged on isolation start date`() =
        runBlocking {
            every { stateStorage.state } returns
                Isolation(
                    isolationStart = Instant.now(fixedClock),
                    isolationConfiguration = DurationDays(),
                    indexCase = IndexCase(
                        symptomsOnsetDate = LocalDate.of(2018, 10, 1),
                        expiryDate = LocalDate.of(2018, 10, 10),
                        selfAssessment = true
                    )
                )
            every { testResultsProvider.testResults.values } returns listOf(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now(fixedClock),
                    acknowledgedDate = Instant.now(fixedClock),
                    testResult = POSITIVE
                )
            )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            isIsolatingBackgroundTick = 1
                            hasSelfDiagnosedPositiveBackgroundTick = 1
                            isIsolatingForSelfDiagnosedBackgroundTick = 1
                            isIsolatingForTestedPositiveBackgroundTick = 1
                            hasSelfDiagnosedBackgroundTick = 1
                            hasTestedPositiveBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to self assessment and last test result is positive and acknowledged before current isolation`() =
        runBlocking {
            every { stateStorage.state } returns
                Isolation(
                    isolationStart = Instant.now(fixedClock).minus(1, ChronoUnit.DAYS),
                    isolationConfiguration = DurationDays(),
                    indexCase = IndexCase(
                        symptomsOnsetDate = LocalDate.of(2018, 10, 1),
                        expiryDate = LocalDate.of(2018, 10, 10),
                        selfAssessment = true
                    )
                )
            every { testResultsProvider.testResults.values } returns listOf(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now(fixedClock),
                    acknowledgedDate = Instant.now(fixedClock).minus(2, ChronoUnit.DAYS),
                    testResult = POSITIVE
                )
            )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            isIsolatingBackgroundTick = 1
                            hasSelfDiagnosedPositiveBackgroundTick = 1
                            isIsolatingForSelfDiagnosedBackgroundTick = 1
                            hasSelfDiagnosedBackgroundTick = 1
                            hasTestedPositiveBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed when user was isolating due to self assessment and last test result is positive`() =
        runBlocking {
            every { stateStorage.state } returns
                Default(
                    previousIsolation = Isolation(
                        isolationStart = Instant.now(fixedClock),
                        isolationConfiguration = DurationDays(),
                        indexCase = IndexCase(
                            symptomsOnsetDate = LocalDate.of(2018, 10, 1),
                            expiryDate = LocalDate.of(2018, 10, 10),
                            selfAssessment = true
                        )
                    )
                )
            every { testResultsProvider.testResults.values } returns listOf(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now(fixedClock),
                    acknowledgedDate = Instant.now(fixedClock),
                    testResult = POSITIVE
                )
            )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            hasSelfDiagnosedBackgroundTick = 1
                            hasTestedPositiveBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating without self assessment and last test result is positive`() =
        runBlocking {
            every { stateStorage.state } returns
                Isolation(
                    isolationStart = Instant.now(fixedClock),
                    isolationConfiguration = DurationDays(),
                    indexCase = IndexCase(
                        symptomsOnsetDate = LocalDate.of(2018, 10, 1),
                        expiryDate = LocalDate.of(2018, 10, 10),
                        selfAssessment = false
                    )
                )
            every { testResultsProvider.testResults.values } returns listOf(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now(fixedClock),
                    acknowledgedDate = Instant.now(fixedClock),
                    testResult = POSITIVE
                ),
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now(fixedClock),
                    acknowledgedDate = Instant.now(fixedClock),
                    testResult = NEGATIVE
                ),
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now(fixedClock),
                    acknowledgedDate = null,
                    testResult = POSITIVE
                ),
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now(fixedClock),
                    acknowledgedDate = Instant.now(fixedClock).minus(30, ChronoUnit.DAYS),
                    testResult = POSITIVE
                )
            )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            isIsolatingBackgroundTick = 1
                            hasSelfDiagnosedPositiveBackgroundTick = 1
                            isIsolatingForTestedPositiveBackgroundTick = 1
                            hasTestedPositiveBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed when user was isolating without self assessment and last test result is positive`() =
        runBlocking {
            every { stateStorage.state } returns
                Default(
                    previousIsolation = Isolation(
                        isolationStart = Instant.now(fixedClock),
                        isolationConfiguration = DurationDays(),
                        indexCase = IndexCase(
                            symptomsOnsetDate = LocalDate.of(2018, 10, 1),
                            expiryDate = LocalDate.of(2018, 10, 10),
                            selfAssessment = false
                        )
                    )
                )
            every { testResultsProvider.testResults.values } returns listOf(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "token1",
                    testEndDate = Instant.now(fixedClock),
                    acknowledgedDate = Instant.now(fixedClock),
                    testResult = POSITIVE
                )
            )
            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            hasTestedPositiveBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed updates only total tasks and running normally ticks when user is not in isolation`() =
        runBlocking {
            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsMetricsLogStorage.add(
                    MetricsLogEntry(
                        Metrics().apply {
                            runningNormallyBackgroundTick = 1
                            totalBackgroundTasks = 1
                        },
                        Instant.now(fixedClock)
                    )
                )
            }
        }

    @Test
    fun `on background completed updates encounter detection pause when exposure notification is disabled`() =
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
