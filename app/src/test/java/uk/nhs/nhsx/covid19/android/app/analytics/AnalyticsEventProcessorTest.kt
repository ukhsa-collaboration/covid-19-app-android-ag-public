package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AcknowledgedStartOfIsolationDueToRiskyContact
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DeclaredNegativeResultFromDct
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAskForSymptomsOnPositiveTestEntry
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidHaveSymptomsBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidRememberOnsetSymptomsDateBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedIsolationPaymentsApplication
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedTestOrdering
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedActiveIpcToken
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM1Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM2Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.RiskyContactReminderNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.TotalAlarmManagerBackgroundTasks
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DECLARED_NEGATIVE_RESULT_FROM_DCT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_ISOLATION_PAYMENTS_APPLICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_TEST_ORDERING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_ACTIVE_IPC_TOKEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_CONTACT_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_VENUE_M1_WARNING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_VENUE_M2_WARNING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RISKY_CONTACT_REMINDER_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_ISOLATION_PAYMENTS_BUTTON
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.TOTAL_ALARM_MANAGER_BACKGROUND_TASKS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.Companion.ISOLATION_STATE_CHANNEL_ID
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Disabled
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Unresolved
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class AnalyticsEventProcessorTest {

    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxed = true)
    private val stateStorage = mockk<StateStorage>(relaxed = true)
    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()
    private val networkTrafficStats = mockk<NetworkTrafficStats>()
    private val isolationPaymentTokenStateProvider = mockk<IsolationPaymentTokenStateProvider>()
    private val notificationProvider = mockk<NotificationProvider>()
    private val lastVisitedBookTestTypeVenueDateProvider = mockk<LastVisitedBookTestTypeVenueDateProvider>(relaxed = true)
    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
    private val isolationHelper = IsolationHelper(fixedClock)

    private val testSubject = AnalyticsEventProcessor(
        analyticsLogStorage,
        stateStorage,
        exposureNotificationApi,
        appAvailabilityProvider,
        networkTrafficStats,
        isolationPaymentTokenStateProvider,
        notificationProvider,
        lastVisitedBookTestTypeVenueDateProvider,
        onboardingCompletedProvider,
        fixedClock
    )

    @Before
    fun setUp() {
        every { appAvailabilityProvider.isAppAvailable() } returns true
        every { stateStorage.state } returns IsolationState(DurationDays())
        coEvery { exposureNotificationApi.isEnabled() } returns true
        coEvery { exposureNotificationApi.isRunningNormally() } returns true
        every { isolationPaymentTokenStateProvider.tokenState } returns Unresolved
        every { notificationProvider.isChannelEnabled(ISOLATION_STATE_CHANNEL_ID) } returns false
        every { lastVisitedBookTestTypeVenueDateProvider.lastVisitedVenue } returns null
        every { onboardingCompletedProvider.value } returns true
    }

    @Test
    fun `do not process any items when onboarding is not completed`() = runBlocking {
        every { onboardingCompletedProvider.value } returns false

        testSubject.track(BackgroundTaskCompletion)

        verify(exactly = 0) { analyticsLogStorage.add(any()) }
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
    fun `on background completed when notifications enabled`() = runBlocking {
        every { notificationProvider.isChannelEnabled(ISOLATION_STATE_CHANNEL_ID) } returns true

        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks(
                            runningNormallyBackgroundTick = true,
                            hasRiskyContactNotificationsEnabledBackgroundTick = true
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `on background completed when notifications disabled`() = runBlocking {
        every { notificationProvider.isChannelEnabled(ISOLATION_STATE_CHANNEL_ID) } returns false

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
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase()
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
            IsolationState(
                isolationConfiguration = DurationDays(),
                contactCase = isolationHelper.contactCase(expired = true)
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
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment()
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
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    contactCase = isolationHelper.contactCase(),
                    indexInfo = isolationHelper.selfAssessment()
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
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment(expired = true)
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
                                hasSelfDiagnosedPositiveBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to self assessment and last test result is positive, PCR, and acknowledged during current isolation`() =
        runBlocking {
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment(
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(1),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
    fun `on background completed when user is isolating due to self assessment and last test result is positive, assisted LFD, and acknowledged during current isolation`() =
        runBlocking {
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment(
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(1),
                            testResult = POSITIVE,
                            testKitType = RAPID_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForSelfDiagnosedBackgroundTick = true,
                                isIsolatingForTestedLFDPositiveBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true,
                                hasTestedLFDPositiveBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to self assessment and last test result is positive, unassisted LFD, and acknowledged during current isolation`() =
        runBlocking {
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment(
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(1),
                            testResult = POSITIVE,
                            testKitType = RAPID_SELF_REPORTED
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
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForSelfDiagnosedBackgroundTick = true,
                                isIsolatingForTestedSelfRapidPositiveBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true,
                                hasTestedSelfRapidPositiveBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to self assessment and last test result is positive, PCR, and acknowledged on isolation start date`() =
        runBlocking {
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment(
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock).minusDays(2),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
    fun `on background completed when user is isolating due to self assessment and last test result is positive, assisted LFD, and acknowledged on isolation start date`() =
        runBlocking {
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment(
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock).minusDays(2),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
                            testResult = POSITIVE,
                            testKitType = RAPID_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForSelfDiagnosedBackgroundTick = true,
                                isIsolatingForTestedLFDPositiveBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true,
                                hasTestedLFDPositiveBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to self assessment and last test result is positive, unassisted LFD, and acknowledged on isolation start date`() =
        runBlocking {
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment(
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock).minusDays(2),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
                            testResult = POSITIVE,
                            testKitType = RAPID_SELF_REPORTED
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
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForSelfDiagnosedBackgroundTick = true,
                                isIsolatingForTestedSelfRapidPositiveBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true,
                                hasTestedSelfRapidPositiveBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to self assessment and last test result is positive, with test result date before current isolation, and acknowledged on isolation start date`() =
        runBlocking {
            val isolationStart = LocalDate.now(fixedClock).minusDays(1)
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment(
                        selfAssessmentDate = isolationStart,
                        testResult = AcknowledgedTestResult(
                            testEndDate = isolationStart.minusDays(2),
                            acknowledgedDate = isolationStart,
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForSelfDiagnosedBackgroundTick = true,
                                hasSelfDiagnosedBackgroundTick = true,
                                hasTestedPositiveBackgroundTick = true,
                                isIsolatingForTestedPositiveBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating due to self assessment and last test result is positive, with test result date before current isolation, and acknowledged before current isolation`() =
        runBlocking {
            val isolationStart = LocalDate.now(fixedClock).minusDays(1)
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment(
                        selfAssessmentDate = isolationStart,
                        testResult = AcknowledgedTestResult(
                            testEndDate = isolationStart.minusDays(2),
                            acknowledgedDate = isolationStart.minusDays(2),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.selfAssessment(
                        expired = true,
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                hasSelfDiagnosedBackgroundTick = true,
                                hasTestedPositiveBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating without self assessment and last test result is positive and PCR`() =
        runBlocking {
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.positiveTest(
                        AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
    fun `on background completed when user is isolating without self assessment and last test result is positive and assisted LFD`() =
        runBlocking {
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.positiveTest(
                        AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock),
                            testResult = POSITIVE,
                            testKitType = RAPID_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForTestedLFDPositiveBackgroundTick = true,
                                hasTestedLFDPositiveBackgroundTick = true,
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating for positive unconfirmed LFD`() =
        runBlocking {
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.positiveTest(
                        AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock),
                            testResult = POSITIVE,
                            testKitType = RAPID_SELF_REPORTED,
                            requiresConfirmatoryTest = true,
                            confirmedDate = null
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
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForTestedSelfRapidPositiveBackgroundTick = true,
                                hasTestedSelfRapidPositiveBackgroundTick = true,
                                isIsolatingForUnconfirmedTestBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed when user is isolating for positive confirmed LFD`() =
        runBlocking {
            every { stateStorage.state } returns
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.positiveTest(
                        AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock),
                            testResult = POSITIVE,
                            testKitType = RAPID_RESULT,
                            requiresConfirmatoryTest = true,
                            confirmedDate = LocalDate.now(fixedClock)
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
                                isIsolatingBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true,
                                isIsolatingForTestedLFDPositiveBackgroundTick = true,
                                hasTestedLFDPositiveBackgroundTick = true
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
                IsolationState(
                    isolationConfiguration = DurationDays(),
                    indexInfo = isolationHelper.positiveTest(
                        AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock).minusDays(12),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(12),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                hasTestedPositiveBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true
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
            coEvery { exposureNotificationApi.isRunningNormally() } returns false

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = false,
                                encounterDetectionPausedBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed updates haveActiveIpcTokenBackgroundTick with active token`() =
        runBlocking {
            every { isolationPaymentTokenStateProvider.tokenState } returns Token("validToken")
            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                haveActiveIpcTokenBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed updates haveActiveIpcTokenBackgroundTick when token is disabled`() =
        runBlocking {
            every { isolationPaymentTokenStateProvider.tokenState } returns Disabled
            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                haveActiveIpcTokenBackgroundTick = false
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed updates haveActiveIpcTokenBackgroundTick when token is unresolved`() =
        runBlocking {

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                haveActiveIpcTokenBackgroundTick = false
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed updates hasReceivedRiskyVenueM2WarningBackgroundTick when LastVisitedBookTestTypeVenueDateProvider timestamp is set`() =
        runBlocking {
            every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns true

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                hasReceivedRiskyVenueM2WarningBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed does not update hasReceivedRiskyVenueM2WarningBackgroundTick when LastVisitedBookTestTypeVenueDateProvider timestamp is not set`() =
        runBlocking {
            every { lastVisitedBookTestTypeVenueDateProvider.containsBookTestTypeVenueAtRisk() } returns false

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                hasReceivedRiskyVenueM2WarningBackgroundTick = false
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
    fun `track received active Ipc token today`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(ReceivedActiveIpcToken, RECEIVED_ACTIVE_IPC_TOKEN)
    }

    @Test
    fun `track risky contact notification today`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            ReceivedRiskyContactNotification,
            RECEIVED_RISKY_CONTACT_NOTIFICATION
        )
    }

    @Test
    fun `track selected isolation payments button`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(SelectedIsolationPaymentsButton, SELECTED_ISOLATION_PAYMENTS_BUTTON)
    }

    @Test
    fun `track launched isolation payments application`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(LaunchedIsolationPaymentsApplication, LAUNCHED_ISOLATION_PAYMENTS_APPLICATION)
    }

    @Test
    fun `track launched test ordering`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(LaunchedTestOrdering, LAUNCHED_TEST_ORDERING)
    }

    @Test
    fun `track acknowledgedStartOfIsolationDueToRiskyContact`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            AcknowledgedStartOfIsolationDueToRiskyContact,
            ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT
        )
    }

    @Test
    fun `track totalRiskyContactReminderNotifications`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(RiskyContactReminderNotification, RISKY_CONTACT_REMINDER_NOTIFICATION)
    }

    @Test
    fun `track declaredNegativeResultFromDct`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(DeclaredNegativeResultFromDct, DECLARED_NEGATIVE_RESULT_FROM_DCT)
    }

    @Test
    fun `track didHaveSymptomsBeforeReceivedTestResult`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            DidHaveSymptomsBeforeReceivedTestResult,
            DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT
        )
    }

    @Test
    fun `track didRememberOnsetSymptomsDateBeforeReceivedTestResult`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            DidRememberOnsetSymptomsDateBeforeReceivedTestResult,
            DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT
        )
    }

    @Test
    fun `track didAskForSymptomsOnPositiveTestEntry`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            DidAskForSymptomsOnPositiveTestEntry,
            DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY
        )
    }

    @Test
    fun `track receivedRiskyVenueM1Warning`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            ReceivedRiskyVenueM1Warning,
            RECEIVED_RISKY_VENUE_M1_WARNING
        )
    }

    @Test
    fun `track receivedRiskyVenueM2Warning`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            ReceivedRiskyVenueM2Warning,
            RECEIVED_RISKY_VENUE_M2_WARNING
        )
    }

    @Test
    fun `track totalAlarmManagerBackgroundTasks`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            TotalAlarmManagerBackgroundTasks,
            TOTAL_ALARM_MANAGER_BACKGROUND_TASKS
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

    private fun Instant.toLocalDate(): LocalDate =
        LocalDateTime.ofInstant(this, fixedClock.zone).toLocalDate()
}
