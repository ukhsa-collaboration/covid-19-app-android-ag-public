package uk.nhs.nhsx.covid19.android.app.analytics

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AcknowledgedStartOfIsolationDueToRiskyContact
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.AskedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireAndStartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CompletedQuestionnaireButDidNotStartIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInReminderScreen
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ConsentedToShareExposureKeysInTheInitialFlow
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessLocalInfoScreenViaBanner
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessLocalInfoScreenViaNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessRiskyVenueM2Notification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAccessSelfIsolationNoteLink
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidAskForSymptomsOnPositiveTestEntry
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidHaveSymptomsBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidRememberOnsetSymptomsDateBeforeReceivedTestResult
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.DidSendLocalInfoNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedIsolationPaymentsApplication
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.LaunchedTestOrdering
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveLFDOutsideTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveLFDWithinTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.NegativeResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.OptedOutForContactIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveLabResultAfterPositiveLFD
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveLabResultAfterPositiveSelfRapidTest
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.PositiveResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.QrCodeCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedActiveIpcToken
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyContactNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM1Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.ReceivedRiskyVenueM2Warning
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.RiskyContactReminderNotification
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasLfdTestM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasNoSymptomsM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedHasSymptomsM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedIsolationPaymentsButton
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedLfdTestOrderingM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedTakeTestLaterM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SelectedTakeTestM2Journey
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.StartedIsolation
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.SuccessfullySharedExposureKeys
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.TotalAlarmManagerBackgroundTasks
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.TotalShareExposureKeysReminderNotifications
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.UpdateNetworkStats
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.VoidResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.ASKED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CANCELED_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_REMINDER_SCREEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ACCESS_LOCAL_INFO_SCREEN_VIA_BANNER
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ACCESS_LOCAL_INFO_SCREEN_VIA_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ACCESS_RISKY_VENUE_M2_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ACCESS_SELF_ISOLATION_NOTE_LINK
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.DID_SEND_LOCAL_INFO_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_ISOLATION_PAYMENTS_APPLICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.LAUNCHED_TEST_ORDERING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_OUTSIDE_TIME_LIMIT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_WITHIN_TIME_LIMIT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_OUTSIDE_TIME_LIMIT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_WITHIN_TIME_LIMIT
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.NEGATIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.OPTED_OUT_FOR_CONTACT_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_LAB_RESULT_AFTER_POSITIVE_LFD
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.POSITIVE_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.QR_CODE_CHECK_IN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_ACTIVE_IPC_TOKEN
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_CONTACT_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_VENUE_M1_WARNING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_RISKY_VENUE_M2_WARNING
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RISKY_CONTACT_REMINDER_NOTIFICATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_HAS_LFD_TEST_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_HAS_NO_SYMPTOMS_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_HAS_SYMPTOMS_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_ISOLATION_PAYMENTS_BUTTON
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_LFD_TEST_ORDERING_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_TAKE_TEST_LATER_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SELECTED_TAKE_TEST_M2_JOURNEY
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.STARTED_ISOLATION
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.SUCCESSFULLY_SHARED_EXPOSURE_KEYS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.TOTAL_ALARM_MANAGER_BACKGROUND_TASKS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.TOTAL_SHARE_EXPOSURE_KEYS_REMINDER_NOTIFICATIONS
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.VOID_RESULT_RECEIVED
import uk.nhs.nhsx.covid19.android.app.availability.AppAvailabilityProvider
import uk.nhs.nhsx.covid19.android.app.exposure.ExposureNotificationApi
import uk.nhs.nhsx.covid19.android.app.isolation.createIsolationLogicalState
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider
import uk.nhs.nhsx.covid19.android.app.notifications.NotificationProvider.Companion.ISOLATION_STATE_CHANNEL_ID
import uk.nhs.nhsx.covid19.android.app.onboarding.OnboardingCompletedProvider
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Disabled
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Unresolved
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.LastVisitedBookTestTypeVenueDateProvider
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.DISABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityState.ENABLED
import uk.nhs.nhsx.covid19.android.app.receiver.AvailabilityStateProvider
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationState
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.SelfAssessment
import uk.nhs.nhsx.covid19.android.app.state.StateStorage
import uk.nhs.nhsx.covid19.android.app.status.localmessage.GetLocalMessageFromStorage
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.ConfirmatoryTestCompletionStatus.COMPLETED_AND_CONFIRMED
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AnalyticsEventProcessorTest {

    private val analyticsLogStorage = mockk<AnalyticsLogStorage>(relaxed = true)
    private val stateStorage = mockk<StateStorage>(relaxUnitFun = true)
    private val exposureNotificationApi = mockk<ExposureNotificationApi>()
    private val appAvailabilityProvider = mockk<AppAvailabilityProvider>()
    private val networkTrafficStats = mockk<NetworkTrafficStats>()
    private val isolationPaymentTokenStateProvider = mockk<IsolationPaymentTokenStateProvider>()
    private val notificationProvider = mockk<NotificationProvider>()
    private val lastVisitedBookTestTypeVenueDateProvider =
        mockk<LastVisitedBookTestTypeVenueDateProvider>(relaxed = true)
    private val onboardingCompletedProvider = mockk<OnboardingCompletedProvider>()
    private val getLocalMessageFromStorage = mockk<GetLocalMessageFromStorage>()
    private val testCoroutineScope = TestCoroutineScope()
    private val fixedClock = Clock.fixed(Instant.parse("2020-05-21T10:00:00Z"), ZoneOffset.UTC)
    private val bluetoothAvailabilityStateProvider = mockk<AvailabilityStateProvider>()
    private val locationAvailabilityStateProvider = mockk<AvailabilityStateProvider>()

    private val isolationHelper = IsolationHelper(fixedClock)

    private val testSubject = AnalyticsEventProcessor(
        analyticsLogStorage,
        stateStorage,
        createIsolationLogicalState(fixedClock),
        exposureNotificationApi,
        appAvailabilityProvider,
        networkTrafficStats,
        isolationPaymentTokenStateProvider,
        notificationProvider,
        lastVisitedBookTestTypeVenueDateProvider,
        onboardingCompletedProvider,
        getLocalMessageFromStorage,
        bluetoothAvailabilityStateProvider,
        locationAvailabilityStateProvider,
        testCoroutineScope,
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
        coEvery { getLocalMessageFromStorage.invoke() } returns null
        every { bluetoothAvailabilityStateProvider.getState() } returns ENABLED
        every { locationAvailabilityStateProvider.getState() } returns ENABLED
    }

    //region background ticks

    @Test
    fun `on background completed when bluetooth is off`() = runBlocking {
        every { bluetoothAvailabilityStateProvider.getState() } returns DISABLED

        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks(
                            runningNormallyBackgroundTick = true,
                            appIsUsableBackgroundTick = true,
                            appIsUsableBluetoothOffBackgroundTick = true,
                            appIsContactTraceableBackgroundTick = true
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `on background completed when location is off and device does not support locationless scanning`() = runBlocking {
        every { exposureNotificationApi.deviceSupportsLocationlessScanning() } returns false
        every { locationAvailabilityStateProvider.getState() } returns DISABLED

        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks(
                            runningNormallyBackgroundTick = true,
                            appIsUsableBackgroundTick = false,
                            appIsContactTraceableBackgroundTick = true
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `on background completed when location is off and device supports locationless scanning`() = runBlocking {
        every { exposureNotificationApi.deviceSupportsLocationlessScanning() } returns true
        every { locationAvailabilityStateProvider.getState() } returns DISABLED

        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks(
                            runningNormallyBackgroundTick = true,
                            appIsUsableBackgroundTick = true,
                            appIsContactTraceableBackgroundTick = true
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `on background completed when app is usable and contact traceable`() = runBlocking {
        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks(
                            runningNormallyBackgroundTick = true,
                            appIsUsableBackgroundTick = true,
                            appIsContactTraceableBackgroundTick = true
                        )
                    )
                )
            )
        }
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
                            runningNormallyBackgroundTick = true,
                            appIsUsableBackgroundTick = true,
                            appIsContactTraceableBackgroundTick = true
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
                            appIsUsableBackgroundTick = true,
                            appIsContactTraceableBackgroundTick = true,
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
                            runningNormallyBackgroundTick = true,
                            appIsUsableBackgroundTick = true,
                            appIsContactTraceableBackgroundTick = true
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
                    contact = isolationHelper.contact()
                )

        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks(
                            runningNormallyBackgroundTick = true,
                            appIsUsableBackgroundTick = true,
                            appIsContactTraceableBackgroundTick = true,
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
                    contact = isolationHelper.contact(expired = true)
                )

        testSubject.track(BackgroundTaskCompletion)

        verify {
            analyticsLogStorage.add(
                AnalyticsLogEntry(
                    instant = Instant.now(fixedClock),
                    logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                        backgroundTaskTicks = BackgroundTaskTicks(
                            runningNormallyBackgroundTick = true,
                            appIsUsableBackgroundTick = true,
                            appIsContactTraceableBackgroundTick = true,
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
                        selfAssessment = isolationHelper.selfAssessment()
                    )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        contact = isolationHelper.contact(),
                        selfAssessment = isolationHelper.selfAssessment()
                    )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        selfAssessment = isolationHelper.selfAssessment(expired = true)
                    )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        selfAssessment = isolationHelper.selfAssessment(),
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(1),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        selfAssessment = isolationHelper.selfAssessment(),
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(1),
                            testResult = POSITIVE,
                            testKitType = RAPID_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        selfAssessment = isolationHelper.selfAssessment(),
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(1),
                            testResult = POSITIVE,
                            testKitType = RAPID_SELF_REPORTED
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        selfAssessment = isolationHelper.selfAssessment(),
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock).minusDays(2),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        selfAssessment = isolationHelper.selfAssessment(),
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock).minusDays(2),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
                            testResult = POSITIVE,
                            testKitType = RAPID_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        selfAssessment = isolationHelper.selfAssessment(),
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock).minusDays(2),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(2),
                            testResult = POSITIVE,
                            testKitType = RAPID_SELF_REPORTED
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        selfAssessment = SelfAssessment(selfAssessmentDate = isolationStart),
                        testResult = AcknowledgedTestResult(
                            testEndDate = isolationStart.minusDays(2),
                            acknowledgedDate = isolationStart,
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
    fun `on background completed when user is isolating due to self assessment and last test result is positive`() =
        runBlocking {
            val isolationStart = LocalDate.now(fixedClock).minusDays(1)
            every { stateStorage.state } returns
                    IsolationState(
                        isolationConfiguration = DurationDays(),
                        selfAssessment = SelfAssessment(selfAssessmentDate = isolationStart),
                        testResult = AcknowledgedTestResult(
                            testEndDate = isolationStart.minusDays(2),
                            acknowledgedDate = isolationStart.minusDays(2),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
    fun `on background completed when user was isolating due to self assessment and last test result is positive`() =
        runBlocking {
            every { stateStorage.state } returns
                    IsolationState(
                        isolationConfiguration = DurationDays(),
                        selfAssessment = isolationHelper.selfAssessment(expired = true),
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock),
                            testResult = POSITIVE,
                            testKitType = RAPID_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock),
                            testResult = POSITIVE,
                            testKitType = RAPID_SELF_REPORTED,
                            requiresConfirmatoryTest = true,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock),
                            acknowledgedDate = LocalDate.now(fixedClock),
                            testResult = POSITIVE,
                            testKitType = RAPID_RESULT,
                            requiresConfirmatoryTest = true,
                            confirmedDate = LocalDate.now(fixedClock),
                            confirmatoryTestCompletionStatus = COMPLETED_AND_CONFIRMED
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
    fun `on background completed when user is isolating as contact, was isolating without self assessment and last test result is positive`() =
        runBlocking {
            every { stateStorage.state } returns
                    IsolationState(
                        isolationConfiguration = DurationDays(),
                        contact = isolationHelper.contact(),
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock).minusDays(12),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(12),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
                                isIsolatingBackgroundTick = true,
                                isIsolatingForHadRiskyContactBackgroundTick = true,
                                hasHadRiskyContactBackgroundTick = true,
                                hasTestedPositiveBackgroundTick = true,
                                hasSelfDiagnosedPositiveBackgroundTick = true
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
                        testResult = AcknowledgedTestResult(
                            testEndDate = LocalDate.now(fixedClock).minusDays(12),
                            acknowledgedDate = LocalDate.now(fixedClock).minusDays(12),
                            testResult = POSITIVE,
                            testKitType = LAB_RESULT,
                            requiresConfirmatoryTest = false,
                            confirmedDate = null
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                                runningNormallyBackgroundTick = true,
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true
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
                                appIsContactTraceableBackgroundTick = false,
                                appIsUsableBackgroundTick = true,
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
                                hasReceivedRiskyVenueM2WarningBackgroundTick = false
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed does set isDisplayingLocalInfoBackgroundTick when localInfo is available`() =
        runBlocking {
            coEvery { getLocalMessageFromStorage.invoke() } returns mockk()

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
                                isDisplayingLocalInfoBackgroundTick = true,
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed does set optedOutForContactIsolationBackgroundTick when contact isolation opt-out date is stored`() =
        runBlocking {
            every { stateStorage.state } returns
                    IsolationState(
                        isolationConfiguration = DurationDays(),
                        contact = isolationHelper.contactWithOptOutDate(
                            optOutOfContactIsolation = LocalDate.now(fixedClock)
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
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
                                hasHadRiskyContactBackgroundTick = true,
                                optedOutForContactIsolationBackgroundTick = true
                            )
                        )
                    )
                )
            }
        }

    @Test
    fun `on background completed does not set optedOutForContactIsolationBackgroundTick when no contact isolation opt-out date is stored`() =
        runBlocking {
            every { stateStorage.state } returns
                    IsolationState(
                        isolationConfiguration = DurationDays(),
                        contact = isolationHelper.contact(expired = true)
                    )

            testSubject.track(BackgroundTaskCompletion)

            verify {
                analyticsLogStorage.add(
                    AnalyticsLogEntry(
                        instant = Instant.now(fixedClock),
                        logItem = AnalyticsLogItem.BackgroundTaskCompletion(
                            backgroundTaskTicks = BackgroundTaskTicks(
                                runningNormallyBackgroundTick = true,
                                appIsUsableBackgroundTick = true,
                                appIsContactTraceableBackgroundTick = true,
                                hasHadRiskyContactBackgroundTick = true,
                                optedOutForContactIsolationBackgroundTick = false
                            )
                        )
                    )
                )
            }
        }

    //endregion

    //region network stats

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

    //endregion

    //region regular events

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

    @Test
    fun `track askedToShareExposureKeysInTheInitialFlow`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            AskedToShareExposureKeysInTheInitialFlow,
            ASKED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW
        )
    }

    @Test
    fun `track consentedToShareExposureKeysInTheInitialFlow`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            ConsentedToShareExposureKeysInTheInitialFlow,
            CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW
        )
    }

    @Test
    fun `track consentedToShareExposureKeysInReminderScreen`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            ConsentedToShareExposureKeysInReminderScreen,
            CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_REMINDER_SCREEN
        )
    }

    @Test
    fun `track totalShareExposureKeysReminderNotifications`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            TotalShareExposureKeysReminderNotifications,
            TOTAL_SHARE_EXPOSURE_KEYS_REMINDER_NOTIFICATIONS
        )
    }

    @Test
    fun `track successfullySharedExposureKeys`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            SuccessfullySharedExposureKeys,
            SUCCESSFULLY_SHARED_EXPOSURE_KEYS
        )
    }

    @Test
    fun `track didSendLocalInfoNotification`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            DidSendLocalInfoNotification,
            DID_SEND_LOCAL_INFO_NOTIFICATION
        )
    }

    @Test
    fun `track didAccessLocalInfoScreenViaNotification`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            DidAccessLocalInfoScreenViaNotification,
            DID_ACCESS_LOCAL_INFO_SCREEN_VIA_NOTIFICATION
        )
    }

    @Test
    fun `track didAccessLocalInfoScreenViaBanner`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            DidAccessLocalInfoScreenViaBanner,
            DID_ACCESS_LOCAL_INFO_SCREEN_VIA_BANNER
        )
    }

    @Test
    fun `track positiveLabResultAfterPositiveLFD`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            PositiveLabResultAfterPositiveLFD,
            POSITIVE_LAB_RESULT_AFTER_POSITIVE_LFD
        )
    }

    @Test
    fun `track negativeLabResultAfterPositiveLFDWithinTimeLimit`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            NegativeLabResultAfterPositiveLFDWithinTimeLimit,
            NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_WITHIN_TIME_LIMIT
        )
    }

    @Test
    fun `track negativeLabResultAfterPositiveLFDOutsideTimeLimit`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            NegativeLabResultAfterPositiveLFDOutsideTimeLimit,
            NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_OUTSIDE_TIME_LIMIT
        )
    }

    @Test
    fun `track positiveLabResultAfterPositiveSelfRapidTest`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            PositiveLabResultAfterPositiveSelfRapidTest,
            POSITIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST
        )
    }

    @Test
    fun `track negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            NegativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit,
            NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_WITHIN_TIME_LIMIT
        )
    }

    @Test
    fun `track negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            NegativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit,
            NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_OUTSIDE_TIME_LIMIT
        )
    }

    @Test
    fun `track didAccessRiskyVenueM2Notification`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            DidAccessRiskyVenueM2Notification,
            DID_ACCESS_RISKY_VENUE_M2_NOTIFICATION
        )
    }

    @Test
    fun `track selectedTakeTestM2Journey`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            SelectedTakeTestM2Journey,
            SELECTED_TAKE_TEST_M2_JOURNEY
        )
    }

    @Test
    fun `track selectedTakeTestLaterM2Journey`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            SelectedTakeTestLaterM2Journey,
            SELECTED_TAKE_TEST_LATER_M2_JOURNEY
        )
    }

    @Test
    fun `track selectedHasSymptomsM2Journey`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            SelectedHasSymptomsM2Journey,
            SELECTED_HAS_SYMPTOMS_M2_JOURNEY
        )
    }

    @Test
    fun `track selectedHasNoSymptomsM2Journey`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            SelectedHasNoSymptomsM2Journey,
            SELECTED_HAS_NO_SYMPTOMS_M2_JOURNEY
        )
    }

    @Test
    fun `track selectedLFDTestOrderingM2Journey`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            SelectedLfdTestOrderingM2Journey,
            SELECTED_LFD_TEST_ORDERING_M2_JOURNEY
        )
    }

    @Test
    fun `track selectedHasLFDTestM2Journey`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            SelectedHasLfdTestM2Journey,
            SELECTED_HAS_LFD_TEST_M2_JOURNEY
        )
    }

    @Test
    fun `track optedOutForContactIsolation`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            OptedOutForContactIsolation,
            OPTED_OUT_FOR_CONTACT_ISOLATION
        )
    }

    @Test
    fun `track didAccessSelfIsolationNoteLink`() = runBlocking {
        verifyTrackRegularAnalyticsEvent(
            DidAccessSelfIsolationNoteLink,
            DID_ACCESS_SELF_ISOLATION_NOTE_LINK
        )
    }

    private fun verifyTrackRegularAnalyticsEvent(event: AnalyticsEvent, eventType: RegularAnalyticsEventType) {
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

    //endregion
}
