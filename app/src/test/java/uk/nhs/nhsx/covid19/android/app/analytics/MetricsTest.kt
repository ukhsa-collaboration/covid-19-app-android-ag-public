package uk.nhs.nhsx.covid19.android.app.analytics

import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.BackgroundTaskCompletion
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.Event
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ExposureWindowMatched
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.ResultReceived
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsLogItem.UpdateNetworkStats
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
import uk.nhs.nhsx.covid19.android.app.analytics.RegularAnalyticsEventType.RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT
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
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.INSIDE_APP
import uk.nhs.nhsx.covid19.android.app.analytics.TestOrderType.OUTSIDE_APP
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import java.time.Instant
import kotlin.test.assertEquals

class MetricsTest {
    private val expectedLogEventCount = 9
    private val totalBackgroundTasksMetric =
        Metrics().copy(
            totalBackgroundTasks = expectedLogEventCount
        )

    //region network stats

    @Test
    fun `add cumulativeDownloadBytes for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            UpdateNetworkStats(downloadedBytes = 25, uploadedBytes = null),
            Metrics().copy(cumulativeDownloadBytes = 225)
        )
    }

    @Test
    fun `add cumulativeUploadBytes for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            UpdateNetworkStats(downloadedBytes = null, uploadedBytes = 15),
            Metrics().copy(cumulativeUploadBytes = 135)
        )
    }

    //endregion

    //region exposure window

    @Test
    fun `add totalExposureWindowsConsideredRisky for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            ExposureWindowMatched(totalRiskyExposures = 4, totalNonRiskyExposures = 0),
            Metrics().copy(totalExposureWindowsConsideredRisky = 36)
        )
    }

    @Test
    fun `add totalExposureWindowsNotConsideredRisky for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            ExposureWindowMatched(totalRiskyExposures = 0, totalNonRiskyExposures = 5),
            Metrics().copy(totalExposureWindowsNotConsideredRisky = 45)
        )
    }

    //endregion

    //region result received

    @Test
    fun `add receivedVoidTestResultViaPolling for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(VOID, LAB_RESULT, INSIDE_APP),
                Metrics().copy(receivedVoidTestResultViaPolling = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedPositiveTestResultViaPolling for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(POSITIVE, LAB_RESULT, INSIDE_APP),
                Metrics().copy(receivedPositiveTestResultViaPolling = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedNegativeTestResultViaPolling for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(NEGATIVE, LAB_RESULT, INSIDE_APP),
                Metrics().copy(receivedNegativeTestResultViaPolling = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedVoidTestResultEnteredManually for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            ResultReceived(VOID, LAB_RESULT, OUTSIDE_APP),
            Metrics().copy(receivedVoidTestResultEnteredManually = expectedLogEventCount)
        )
    }

    @Test
    fun `add receivedPositiveTestResultEnteredManually for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(POSITIVE, LAB_RESULT, OUTSIDE_APP),
                Metrics().copy(receivedPositiveTestResultEnteredManually = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedNegativeTestResultEnteredManually for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(NEGATIVE, LAB_RESULT, OUTSIDE_APP),
                Metrics().copy(receivedNegativeTestResultEnteredManually = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedVoidLFDTestResultViaPolling on assisted test for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(VOID, RAPID_RESULT, INSIDE_APP),
                Metrics().copy(receivedVoidLFDTestResultViaPolling = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedPositiveLFDTestResultViaPolling on assisted test for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(POSITIVE, RAPID_RESULT, INSIDE_APP),
                Metrics().copy(receivedPositiveLFDTestResultViaPolling = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedNegativeLFDTestResultViaPolling on assisted test for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(NEGATIVE, RAPID_RESULT, INSIDE_APP),
                Metrics().copy(receivedNegativeLFDTestResultViaPolling = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedVoidLFDTestResultEnteredManually on assisted test for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(VOID, RAPID_RESULT, OUTSIDE_APP),
                Metrics().copy(receivedVoidLFDTestResultEnteredManually = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedPositiveLFDTestResultEnteredManually on assisted test for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(POSITIVE, RAPID_RESULT, OUTSIDE_APP),
                Metrics().copy(receivedPositiveLFDTestResultEnteredManually = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedPositiveSelfRapidTestResultEnteredManually on unassisted test for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(POSITIVE, RAPID_SELF_REPORTED, OUTSIDE_APP),
                Metrics().copy(receivedPositiveSelfRapidTestResultEnteredManually = expectedLogEventCount)
            )
        }

    @Test
    fun `add receivedNegativeLFDTestResultEnteredManually on assisted test for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                ResultReceived(NEGATIVE, RAPID_RESULT, OUTSIDE_APP),
                Metrics().copy(receivedNegativeLFDTestResultEnteredManually = expectedLogEventCount)
            )
        }

    //endregion

    //region regular events

    @Test
    fun `add canceledCheckIn for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(CANCELED_CHECK_IN),
            Metrics().copy(canceledCheckIn = expectedLogEventCount)
        )
    }

    @Test
    fun `add checkedIn for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(QR_CODE_CHECK_IN),
            Metrics().copy(checkedIn = expectedLogEventCount)
        )
    }

    @Test
    fun `add acknowledgedStartOfIsolationDueToRiskyContact for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(ACKNOWLEDGED_START_OF_ISOLATION_DUE_TO_RISKY_CONTACT),
            Metrics().copy(acknowledgedStartOfIsolationDueToRiskyContact = expectedLogEventCount)
        )
    }

    @Test
    fun `add completedQuestionnaireAndStartedIsolation for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                Event(COMPLETED_QUESTIONNAIRE_AND_STARTED_ISOLATION),
                Metrics().copy(completedQuestionnaireAndStartedIsolation = expectedLogEventCount)
            )
        }

    @Test
    fun `add completedQuestionnaireButDidNotStartIsolation for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                Event(COMPLETED_QUESTIONNAIRE_BUT_DID_NOT_START_ISOLATION),
                Metrics().copy(completedQuestionnaireButDidNotStartIsolation = expectedLogEventCount)
            )
        }

    @Test
    fun `add totalRiskyContactReminderNotifications for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(RISKY_CONTACT_REMINDER_NOTIFICATION),
            Metrics().copy(totalRiskyContactReminderNotifications = expectedLogEventCount)
        )
    }

    @Test
    fun `add receivedNegativeTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(NEGATIVE_RESULT_RECEIVED),
            Metrics().copy(receivedNegativeTestResult = expectedLogEventCount)
        )
    }

    @Test
    fun `add receivedPositiveTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(POSITIVE_RESULT_RECEIVED),
            Metrics().copy(receivedPositiveTestResult = expectedLogEventCount)
        )
    }

    @Test
    fun `add receivedVoidTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(VOID_RESULT_RECEIVED),
            Metrics().copy(receivedVoidTestResult = expectedLogEventCount)
        )
    }

    @Test
    fun `add receivedRiskyContactNotification for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(RECEIVED_RISKY_CONTACT_NOTIFICATION),
            Metrics().copy(receivedRiskyContactNotification = 1)
        )
    }

    @Test
    fun `add startedIsolation for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(STARTED_ISOLATION),
            Metrics().copy(startedIsolation = expectedLogEventCount)
        )
    }

    @Test
    fun `add receivedActiveIpcToken for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(RECEIVED_ACTIVE_IPC_TOKEN),
            Metrics().copy(receivedActiveIpcToken = expectedLogEventCount)
        )
    }

    @Test
    fun `add selectedIsolationPaymentsButton for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(SELECTED_ISOLATION_PAYMENTS_BUTTON),
            Metrics().copy(selectedIsolationPaymentsButton = expectedLogEventCount)
        )
    }

    @Test
    fun `add launchedIsolationPaymentsApplication for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(LAUNCHED_ISOLATION_PAYMENTS_APPLICATION),
            Metrics().copy(launchedIsolationPaymentsApplication = expectedLogEventCount)
        )
    }

    @Test
    fun `add launchedTestOrdering for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(LAUNCHED_TEST_ORDERING),
            Metrics().copy(launchedTestOrdering = expectedLogEventCount)
        )
    }

    @Test
    fun `add receivedUnconfirmedPositiveTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(RECEIVED_UNCONFIRMED_POSITIVE_TEST_RESULT),
            Metrics().copy(receivedUnconfirmedPositiveTestResult = expectedLogEventCount)
        )
    }

    @Test
    fun `add didHaveSymptomsBeforeReceivedTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(DID_HAVE_SYMPTOMS_BEFORE_RECEIVED_TEST_RESULT),
            Metrics().copy(didHaveSymptomsBeforeReceivedTestResult = expectedLogEventCount)
        )
    }

    @Test
    fun `add didRememberOnsetSymptomsDateBeforeReceivedTestResult for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(DID_REMEMBER_ONSET_SYMPTOMS_DATE_BEFORE_RECEIVED_TEST_RESULT),
            Metrics().copy(didRememberOnsetSymptomsDateBeforeReceivedTestResult = expectedLogEventCount)
        )
    }

    @Test
    fun `add didAskForSymptomsOnPositiveTestEntry for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(DID_ASK_FOR_SYMPTOMS_ON_POSITIVE_TEST_ENTRY),
            Metrics().copy(didAskForSymptomsOnPositiveTestEntry = expectedLogEventCount)
        )
    }

    @Test
    fun `add receivedRiskyVenueM1Warning for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(RECEIVED_RISKY_VENUE_M1_WARNING),
            Metrics().copy(receivedRiskyVenueM1Warning = expectedLogEventCount)
        )
    }

    @Test
    fun `add receivedRiskyVenueM2Warning for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(RECEIVED_RISKY_VENUE_M2_WARNING),
            Metrics().copy(receivedRiskyVenueM2Warning = expectedLogEventCount)
        )
    }

    @Test
    fun `add totalAlarmManagerBackgroundTasks for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(TOTAL_ALARM_MANAGER_BACKGROUND_TASKS),
            Metrics().copy(totalAlarmManagerBackgroundTasks = expectedLogEventCount)
        )
    }

    @Test
    fun `add askedToShareExposureKeysInTheInitialFlow for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(ASKED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW),
            Metrics().copy(askedToShareExposureKeysInTheInitialFlow = expectedLogEventCount)
        )
    }

    @Test
    fun `add consentedToShareExposureKeysInTheInitialFlow for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_THE_INITIAL_FLOW),
            Metrics().copy(consentedToShareExposureKeysInTheInitialFlow = expectedLogEventCount)
        )
    }

    @Test
    fun `add consentedToShareExposureKeysInReminderScreen for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(CONSENTED_TO_SHARE_EXPOSURE_KEYS_IN_REMINDER_SCREEN),
            Metrics().copy(consentedToShareExposureKeysInReminderScreen = expectedLogEventCount)
        )
    }

    @Test
    fun `add totalShareExposureKeyReminderNotifications for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(TOTAL_SHARE_EXPOSURE_KEYS_REMINDER_NOTIFICATIONS),
            Metrics().copy(totalShareExposureKeysReminderNotifications = expectedLogEventCount)
        )
    }

    @Test
    fun `add successfullySharedExposureKeys for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(SUCCESSFULLY_SHARED_EXPOSURE_KEYS),
            Metrics().copy(successfullySharedExposureKeys = expectedLogEventCount)
        )
    }

    @Test
    fun `add didSendLocalInfoNotification for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(DID_SEND_LOCAL_INFO_NOTIFICATION),
            Metrics().copy(didSendLocalInfoNotification = expectedLogEventCount)
        )
    }

    @Test
    fun `add didAccessLocalInfoScreenViaNotification for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(DID_ACCESS_LOCAL_INFO_SCREEN_VIA_NOTIFICATION),
            Metrics().copy(didAccessLocalInfoScreenViaNotification = expectedLogEventCount)
        )
    }

    @Test
    fun `add didAccessLocalInfoScreenViaBanner for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(DID_ACCESS_LOCAL_INFO_SCREEN_VIA_BANNER),
            Metrics().copy(didAccessLocalInfoScreenViaBanner = expectedLogEventCount)
        )
    }

    @Test
    fun `add positiveLabResultAfterPositiveLFD for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(POSITIVE_LAB_RESULT_AFTER_POSITIVE_LFD),
            Metrics().copy(positiveLabResultAfterPositiveLFD = expectedLogEventCount)
        )
    }

    @Test
    fun `add negativeLabResultAfterPositiveLFDWithinTimeLimit for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_WITHIN_TIME_LIMIT),
            Metrics().copy(negativeLabResultAfterPositiveLFDWithinTimeLimit = expectedLogEventCount)
        )
    }

    @Test
    fun `add negativeLabResultAfterPositiveLFDOutsideTimeLimit for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(NEGATIVE_LAB_RESULT_AFTER_POSITIVE_LFD_OUTSIDE_TIME_LIMIT),
            Metrics().copy(negativeLabResultAfterPositiveLFDOutsideTimeLimit = expectedLogEventCount)
        )
    }

    @Test
    fun `add positiveLabResultAfterPositiveSelfRapidTest for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(POSITIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST),
            Metrics().copy(positiveLabResultAfterPositiveSelfRapidTest = expectedLogEventCount)
        )
    }

    @Test
    fun `add negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                Event(NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_WITHIN_TIME_LIMIT),
                Metrics().copy(negativeLabResultAfterPositiveSelfRapidTestWithinTimeLimit = expectedLogEventCount)
            )
        }

    @Test
    fun `add negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                Event(NEGATIVE_LAB_RESULT_AFTER_POSITIVE_SELF_RAPID_TEST_OUTSIDE_TIME_LIMIT),
                Metrics().copy(negativeLabResultAfterPositiveSelfRapidTestOutsideTimeLimit = expectedLogEventCount)
            )
        }

    @Test
    fun `add didAccessRiskyVenueM2Notification for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(DID_ACCESS_RISKY_VENUE_M2_NOTIFICATION),
            Metrics().copy(didAccessRiskyVenueM2Notification = expectedLogEventCount)
        )
    }

    @Test
    fun `add selectedTakeTestM2Journey for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(SELECTED_TAKE_TEST_M2_JOURNEY),
            Metrics().copy(selectedTakeTestM2Journey = expectedLogEventCount)
        )
    }

    @Test
    fun `add selectedTakeTestLaterM2Journey for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(SELECTED_TAKE_TEST_LATER_M2_JOURNEY),
            Metrics().copy(selectedTakeTestLaterM2Journey = expectedLogEventCount)
        )
    }

    @Test
    fun `add selectedHasSymptomsM2Journey for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(SELECTED_HAS_SYMPTOMS_M2_JOURNEY),
            Metrics().copy(selectedHasSymptomsM2Journey = expectedLogEventCount)
        )
    }

    @Test
    fun `add selectedHasNoSymptomsM2Journey for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(SELECTED_HAS_NO_SYMPTOMS_M2_JOURNEY),
            Metrics().copy(selectedHasNoSymptomsM2Journey = expectedLogEventCount)
        )
    }

    @Test
    fun `add selectedLFDTestOrderingM2Journey for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(SELECTED_LFD_TEST_ORDERING_M2_JOURNEY),
            Metrics().copy(selectedLFDTestOrderingM2Journey = expectedLogEventCount)
        )
    }

    @Test
    fun `add selectedHasLFDTestM2Journey for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(SELECTED_HAS_LFD_TEST_M2_JOURNEY),
            Metrics().copy(selectedHasLFDTestM2Journey = expectedLogEventCount)
        )
    }

    @Test
    fun `add optedOutForContactIsolation for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(OPTED_OUT_FOR_CONTACT_ISOLATION),
            Metrics().copy(optedOutForContactIsolation = expectedLogEventCount)
        )
    }

    //endregion

    //region background ticks

    @Test
    fun `add encounterDetectionPausedBackgroundTick for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                BackgroundTaskCompletion(
                    BackgroundTaskTicks(encounterDetectionPausedBackgroundTick = true)
                ),
                totalBackgroundTasksMetric.copy(encounterDetectionPausedBackgroundTick = expectedLogEventCount)
            )
        }

    @Test
    fun `add haveActiveIpcTokenBackgroundTick for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                BackgroundTaskCompletion(
                    BackgroundTaskTicks(haveActiveIpcTokenBackgroundTick = true)
                ),
                totalBackgroundTasksMetric.copy(haveActiveIpcTokenBackgroundTick = expectedLogEventCount)
            )
        }

    @Test
    fun `add hasHadRiskyContactBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(hasHadRiskyContactBackgroundTick = true)
            ),
            totalBackgroundTasksMetric.copy(hasHadRiskyContactBackgroundTick = expectedLogEventCount)
        )
    }

    @Test
    fun `add hasSelfDiagnosedPositiveBackgroundTick for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                BackgroundTaskCompletion(
                    BackgroundTaskTicks(hasSelfDiagnosedPositiveBackgroundTick = true)
                ),
                totalBackgroundTasksMetric.copy(hasSelfDiagnosedPositiveBackgroundTick = 9)
            )
        }

    @Test
    fun `add isIsolatingBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(isIsolatingBackgroundTick = true)
            ),
            totalBackgroundTasksMetric.copy(isIsolatingBackgroundTick = expectedLogEventCount)
        )
    }

    @Test
    fun `add runningNormallyBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(runningNormallyBackgroundTick = true)
            ),
            totalBackgroundTasksMetric.copy(runningNormallyBackgroundTick = expectedLogEventCount)
        )
    }

    @Test
    fun `add totalBackgroundTasks for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks()
            ),
            totalBackgroundTasksMetric.copy()
        )
    }

    @Test
    fun `add hasRiskyContactNotificationsEnabledBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(hasRiskyContactNotificationsEnabledBackgroundTick = true)
            ),
            totalBackgroundTasksMetric.copy(hasRiskyContactNotificationsEnabledBackgroundTick = expectedLogEventCount)
        )
    }

    @Test
    fun `add hasSelfDiagnosedBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(hasSelfDiagnosedBackgroundTick = true)
            ),
            totalBackgroundTasksMetric.copy(hasSelfDiagnosedBackgroundTick = expectedLogEventCount)
        )
    }

    @Test
    fun `add hasTestedPositiveBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(hasTestedPositiveBackgroundTick = true)
            ),
            totalBackgroundTasksMetric.copy(hasTestedPositiveBackgroundTick = expectedLogEventCount)
        )
    }

    @Test
    fun `add hasTestedLFDPositiveBackgroundTick for events in same analytics window`() = runBlocking {
        `test aggregation of analytics metrics`(
            BackgroundTaskCompletion(
                BackgroundTaskTicks(hasTestedLFDPositiveBackgroundTick = true)
            ),
            totalBackgroundTasksMetric.copy(hasTestedLFDPositiveBackgroundTick = expectedLogEventCount)
        )
    }

    @Test
    fun `add isIsolatingForSelfDiagnosedBackgroundTick for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                BackgroundTaskCompletion(
                    BackgroundTaskTicks(isIsolatingForSelfDiagnosedBackgroundTick = true)
                ),
                totalBackgroundTasksMetric.copy(isIsolatingForSelfDiagnosedBackgroundTick = expectedLogEventCount)
            )
        }

    @Test
    fun `add isIsolatingForTestedPositiveBackgroundTick for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                BackgroundTaskCompletion(
                    BackgroundTaskTicks(isIsolatingForTestedPositiveBackgroundTick = true)
                ),
                totalBackgroundTasksMetric.copy(isIsolatingForTestedPositiveBackgroundTick = expectedLogEventCount)
            )
        }

    @Test
    fun `add isIsolatingForTestedLFDPositiveBackgroundTick for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                BackgroundTaskCompletion(
                    BackgroundTaskTicks(isIsolatingForTestedLFDPositiveBackgroundTick = true)
                ),
                totalBackgroundTasksMetric.copy(isIsolatingForTestedLFDPositiveBackgroundTick = expectedLogEventCount)
            )
        }

    @Test
    fun `add isIsolatingForHadRiskyContactBackgroundTick for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                BackgroundTaskCompletion(
                    BackgroundTaskTicks(
                        isIsolatingForHadRiskyContactBackgroundTick = true
                    )
                ),
                totalBackgroundTasksMetric.copy(isIsolatingForHadRiskyContactBackgroundTick = expectedLogEventCount)
            )
        }

    @Test
    fun `add isDisplayingLocalInfoBackgroundTick for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                BackgroundTaskCompletion(
                    BackgroundTaskTicks(
                        isDisplayingLocalInfoBackgroundTick = true
                    )
                ),
                totalBackgroundTasksMetric.copy(isDisplayingLocalInfoBackgroundTick = expectedLogEventCount)
            )
        }

    @Test
    fun `add optedOutForContactIsolationBackgroundTick for events in same analytics window`() =
        runBlocking {
            `test aggregation of analytics metrics`(
                BackgroundTaskCompletion(
                    BackgroundTaskTicks(
                        optedOutForContactIsolationBackgroundTick = true
                    )
                ),
                totalBackgroundTasksMetric.copy(optedOutForContactIsolationBackgroundTick = expectedLogEventCount)
            )
        }

    // endregion

    //region missingSubmissionDays

    @Test
    fun `metrics with missingSubmissionDays`() = runBlocking {
        `test aggregation of analytics metrics`(
            Event(QR_CODE_CHECK_IN),
            Metrics().copy(checkedIn = expectedLogEventCount, missingPacketsLast7Days = 2),
            2
        )
    }

    //endregion

    private fun `test aggregation of analytics metrics`(
        analyticsLogItem: AnalyticsLogItem,
        expectedMetrics: Metrics,
        missingSubmissionDays: Int = 0
    ) = runBlocking {

        val logEntry1 = AnalyticsLogEntry(
            instant = Instant.parse("2020-09-28T00:00:00Z"),
            logItem = analyticsLogItem
        )
        val logEntries1 = listOf(logEntry1, logEntry1, logEntry1, logEntry1)

        val logEntry2 = AnalyticsLogEntry(
            instant = Instant.parse("2020-09-28T23:59:59Z"),
            logItem = analyticsLogItem
        )
        val logEntries2 = listOf(logEntry2, logEntry2, logEntry2, logEntry2, logEntry2)

        val analyticsLog = listOf(logEntries1, logEntries2).flatten()

        val analyticsLogAsMetrics = analyticsLog.toMetrics(missingSubmissionDays)

        assertEquals(expectedMetrics, analyticsLogAsMetrics)
    }
}
