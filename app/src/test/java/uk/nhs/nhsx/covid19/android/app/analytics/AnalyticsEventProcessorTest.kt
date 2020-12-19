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
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_CONTACT_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
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

    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxed = true)
    private val stateStorage = mockk<StateStorage>(relaxed = true)
    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()
    private val networkTrafficStats = mockk<NetworkTrafficStats>()
    private val testResultsProvider = mockk<TestResultsProvider>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)

    private val testSubject = AnalyticsEventProcessor(
        analyticsLogStorage,
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
    fun `on background completed when app is not available`() = runBlocking {
        every { appAvailabilityProvider.isAppAvailable() } returns false

        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks()
                    )
                )
            )
        }
    }

    @Test
    fun `on background completed when app is available`() = runBlocking {
        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks(
                            runningNormallyBackgroundTick = true
                        )
                    )
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
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks(
                            runningNormallyBackgroundTick = true,
                            isIsolatingBackgroundTick = true,
                            isIsolatingForHadRiskyContactBackgroundTick = true,
                            hasHadRiskyContactBackgroundTick = true
                        )
                    )
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
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks(
                            runningNormallyBackgroundTick = true,
                            hasHadRiskyContactBackgroundTick = true
                        )
                    )
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
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForSelfDiagnosedBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true
                            )
                        )
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
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                isIsolatingBackgroundTick = true,
                                isIsolatingForHadRiskyContactBackgroundTick = true,
                                isIsolatingForSelfDiagnosedBackgroundTick = true,
                                hasHadRiskyContactBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true
                            )
                        )
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
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true
                            )
                        )
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
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForSelfDiagnosedBackgroundTick = true,
                                isIsolatingForTestedPositiveBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true,
                                hasTestedPositiveBackgroundTick = true
                            )
                        )
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
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForSelfDiagnosedBackgroundTick = true,
                                isIsolatingForTestedPositiveBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true,
                                hasTestedPositiveBackgroundTick = true
                            )
                        )
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
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForSelfDiagnosedBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true,
                                hasTestedPositiveBackgroundTick = true
                            )
                        )
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
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true,
                                hasTestedPositiveBackgroundTick = true
                            )
                        )
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
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForTestedPositiveBackgroundTick = true,
                                hasTestedPositiveBackgroundTick = true
                            )
                        )
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
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                hasTestedPositiveBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed updates only total tasks and running normally ticks when user is not in isolation`() =
        runBlocking {
            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true
                            )
                        )
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
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                encounterDetectionPausedBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `track qr code check in`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(QrCodeCheckIn, QR_CODE_CHECK_IN)
    }

    @Test
    fun `track cancelled check in`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(CanceledCheckIn, CANCELED_CHECK_IN)
    }

    @Test
    fun `track completed questionnaire and started isolation`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            CompletedQuestionnaireAndStartedIsolation,
            COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
        )
    }

    @Test
    fun `track completed questionnaire but did not start isolation`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            CompletedQuestionnaireButDidNotStartIsolation,
            COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
        )
    }

    @Test
    fun `track positive result received`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(PositiveResultReceived, POSITIVE_RESULT_RECEIVED)
    }

    @Test
    fun `track negative result received`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(NegativeResultReceived, NEGATIVE_RESULT_RECEIVED)
    }

    @Test
    fun `track void result received`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(VoidResultReceived, VOID_RESULT_RECEIVED)
    }

    @Test
    fun `on network stats update`() = runBlocking {
        every { networkTrafficStats.getTotalBytesUploaded() } returns 15
        every { networkTrafficStats.getTotalBytesDownloaded() } returns 25

        testSubject.track(UpdateNetworkStats)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.UpdateNetworkStats(
                        downloadedBytes = 25,
                        uploadedBytes = 15
                    )
                )
            )
        }
    }

    @Test
    fun `track isolation started today`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(StartedIsolation, STARTED_ISOLATION)
    }

    @Test
    fun `track risky contact notification today`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            ReceivedRiskyContactNotification,
            RECEIVED_RISKY_CONTACT_NOTIFICATION
        )
    }

    private suspend fun verifyTrackRegularAnalyticsEvent(event: AnalyticsEvent, eventType: RegularAnalyticsEventType) {
        testSubject.track(event)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = Event(eventType)
                )
            )
        }
    }
}
