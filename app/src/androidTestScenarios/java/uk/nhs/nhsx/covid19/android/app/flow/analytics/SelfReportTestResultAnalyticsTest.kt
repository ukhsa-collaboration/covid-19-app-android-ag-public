package uk.nhs.nhsx.covid19.android.app.flow.analytics

import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_REPORTING
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.exposure.MockExposureNotificationApi.Result.Success
import uk.nhs.nhsx.covid19.android.app.exposure.executeWithTheUserDecliningExposureKeySharing
import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.AppWillNotNotifyOtherUsersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.NegativeVoidTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReportedTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelectTestDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportCheckAnswersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportSymptomsOnsetRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportThankYouRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestKitTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOriginRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import java.time.LocalDate

class SelfReportTestResultAnalyticsTest : AnalyticsTest() {

    private val negativeVoidTestResultRobot = NegativeVoidTestResultRobot()
    private val statusRobot = StatusRobot()
    private val testTypeRobot = TestTypeRobot()
    private val checkAnswersRobot = SelfReportCheckAnswersRobot()
    private val thankYouRobot = SelfReportThankYouRobot()
    private val adviceRobot = SelfReportAdviceRobot()
    private val shareKeysInformationRobot = SelfReportShareKeysInformationRobot()
    private val testKitTypeRobot = TestKitTypeRobot()
    private val testOriginRobot = TestOriginRobot()
    private val appWillNotNotifyOtherUsersRobot = AppWillNotNotifyOtherUsersRobot()
    private val reportedTestRobot = ReportedTestRobot()
    private val selectTestDateRobot = SelectTestDateRobot()
    private val symptomsRobot = SelfReportSymptomsRobot()
    private val symptomsOnsetRobot = SelfReportSymptomsOnsetRobot()

    @Test
    fun enterNegativeTest_pressBackToHome_assertSelfReportedNegativeSelfLFDTestResultEnteredManually() =
    runWithFeature(SELF_REPORTING, enabled = true) {
        startTestActivity<MainActivity>()

        runBackgroundTasks()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.clickLinkTestResult()

        waitFor { this.testTypeRobot.checkActivityIsDisplayed() }

        this.testTypeRobot.clickNegativeButton()
        this.testTypeRobot.clickContinueButton()

        waitFor { negativeVoidTestResultRobot.checkActivityIsDisplayed() }

        negativeVoidTestResultRobot.clickBackToHome()

        assertOnFields {
            assertEquals(1, Metrics::selfReportedNegativeSelfLFDTestResultEnteredManually)
            assertEquals(0, Metrics::selfReportedVoidSelfLFDTestResultEnteredManually)
        }
    }

    @Test
    fun enterVoidTest_pressBackToHome_assertSelfReportedVoidSelfLFDTestResultEnteredManually() =
    runWithFeature(SELF_REPORTING, enabled = true) {
        startTestActivity<MainActivity>()

        runBackgroundTasks()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        statusRobot.clickLinkTestResult()

        waitFor { this.testTypeRobot.checkActivityIsDisplayed() }

        this.testTypeRobot.clickVoidButton()
        this.testTypeRobot.clickContinueButton()

        waitFor { negativeVoidTestResultRobot.checkActivityIsDisplayed() }

        negativeVoidTestResultRobot.clickBackToHome()

        assertOnFields {
            assertEquals(1, Metrics::selfReportedVoidSelfLFDTestResultEnteredManually)
            assertEquals(0, Metrics::selfReportedNegativeSelfLFDTestResultEnteredManually)
        }
    }

    @Test
    fun enterPositiveTest_withKeySharingAndAllRelevantLFDAnalyticsFieldsSet() {
        runWithFeature(SELF_REPORTING, enabled = true) {
            testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Success()
            testAppContext.getExposureNotificationApi().setEnabled(true)
            MockApiModule.behaviour.responseType = ALWAYS_SUCCEED

            startTestActivity<StatusActivity>()

            runBackgroundTasks()

            statusRobot.clickLinkTestResult()
            waitFor { testTypeRobot.checkActivityIsDisplayed() }
            testTypeRobot.clickPositiveButton()
            testTypeRobot.clickContinueButton()

            waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }
            shareKeysInformationRobot.clickContinueButton()

            waitFor { testKitTypeRobot.checkActivityIsDisplayed() }
            testKitTypeRobot.clickLFDButton()
            testKitTypeRobot.clickContinueButton()

            waitFor { testOriginRobot.checkActivityIsDisplayed() }
            testOriginRobot.clickYesButton()
            testOriginRobot.clickContinueButton()

            waitFor { selectTestDateRobot.checkActivityIsDisplayed() }
            selectTestDateRobot.selectCannotRememberDate()
            selectTestDateRobot.clickContinueButton()

            waitFor { symptomsRobot.checkActivityIsDisplayed() }
            symptomsRobot.clickYesButton()
            symptomsRobot.clickContinueButton()

            waitFor { symptomsOnsetRobot.checkActivityIsDisplayed() }
            symptomsOnsetRobot.clickSelectDate()
            symptomsOnsetRobot.selectDayOfMonth(LocalDate.now(testAppContext.clock).dayOfMonth)
            symptomsOnsetRobot.clickContinueButton()

            waitFor { reportedTestRobot.checkActivityIsDisplayed() }
            reportedTestRobot.clickYesButton()
            reportedTestRobot.clickContinueButton()

            waitFor { checkAnswersRobot.checkActivityIsDisplayed() }
            checkAnswersRobot.clickContinueButton()
            waitFor { thankYouRobot.checkActivityIsDisplayed() }
            thankYouRobot.clickContinue()
            waitFor { adviceRobot.checkActivityIsDisplayed() }
            adviceRobot.clickPrimaryBackToHomeButton()
            waitFor { statusRobot.checkActivityIsDisplayed() }

            assertOnFields {
                assertEquals(1, Metrics::receivedPositiveTestResult)
                assertEquals(1, Metrics::successfullySharedExposureKeys)
                assertEquals(1, Metrics::isPositiveSelfLFDFree)
                assertEquals(1, Metrics::didHaveSymptomsBeforeReceivedTestResult)
                assertEquals(1, Metrics::didRememberOnsetSymptomsDateBeforeReceivedTestResult)
                assertEquals(1, Metrics::selfReportedPositiveSelfLFDOnGov)
                assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::completedSelfReportingTestFlow)
                assertEquals(1, Metrics::startedIsolation)
                assertEquals(1, Metrics::receivedPositiveSelfRapidTestResultEnteredManually)
                assertEquals(1, Metrics::isIsolatingBackgroundTick)
                assertEquals(1, Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick)
            }
        }
    }

    @Test
    fun enterPositiveTest_withoutKeySharingPCRAndNoSymptoms() {
        runWithFeature(SELF_REPORTING, enabled = true) {
            startTestActivity<StatusActivity>()

            runBackgroundTasks()

            statusRobot.clickLinkTestResult()
            waitFor { testTypeRobot.checkActivityIsDisplayed() }
            testTypeRobot.clickPositiveButton()
            testTypeRobot.clickContinueButton()

            waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }
            testAppContext.executeWithTheUserDecliningExposureKeySharing {

                shareKeysInformationRobot.clickContinueButton()

                waitFor { appWillNotNotifyOtherUsersRobot.checkActivityIsDisplayed() }
            }
            appWillNotNotifyOtherUsersRobot.clickContinue()

            waitFor { testKitTypeRobot.checkActivityIsDisplayed() }
            testKitTypeRobot.clickPCRButton()
            testKitTypeRobot.clickContinueButton()

            waitFor { selectTestDateRobot.checkActivityIsDisplayed() }
            selectTestDateRobot.selectCannotRememberDate()
            selectTestDateRobot.clickContinueButton()

            waitFor { symptomsRobot.checkActivityIsDisplayed() }
            symptomsRobot.clickNoButton()
            symptomsRobot.clickContinueButton()

            waitFor { checkAnswersRobot.checkActivityIsDisplayed() }
            checkAnswersRobot.clickContinueButton()
            waitFor { thankYouRobot.checkActivityIsDisplayed() }
            thankYouRobot.clickContinue()
            waitFor { adviceRobot.checkActivityIsDisplayed() }
            adviceRobot.clickPrimaryBackToHomeButton()
            waitFor { statusRobot.checkActivityIsDisplayed() }

            assertOnFields {
                assertEquals(1, Metrics::receivedPositiveTestResult)
                assertEquals(0, Metrics::successfullySharedExposureKeys)
                assertEquals(0, Metrics::isPositiveSelfLFDFree)
                assertEquals(0, Metrics::didHaveSymptomsBeforeReceivedTestResult)
                assertEquals(0, Metrics::didRememberOnsetSymptomsDateBeforeReceivedTestResult)
                assertEquals(0, Metrics::selfReportedPositiveSelfLFDOnGov)
                assertEquals(0, Metrics::consentedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::completedSelfReportingTestFlow)
                assertEquals(1, Metrics::startedIsolation)
                assertEquals(0, Metrics::receivedPositiveSelfRapidTestResultEnteredManually)
                assertEquals(1, Metrics::isIsolatingBackgroundTick)
                assertEquals(0, Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick)
                assertEquals(1, Metrics::isIsolatingForTestedPositiveBackgroundTick)
                assertEquals(1, Metrics::receivedPositiveTestResultEnteredManually)
            }
        }
    }

    @Test
    fun enterPositiveTest_withKeySharingNotNHSLFDTestAndDidNotRememberSymptomsOnset() {
        runWithFeature(SELF_REPORTING, enabled = true) {
            testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Success()
            testAppContext.getExposureNotificationApi().setEnabled(true)
            MockApiModule.behaviour.responseType = ALWAYS_SUCCEED

            startTestActivity<StatusActivity>()

            runBackgroundTasks()

            statusRobot.clickLinkTestResult()
            waitFor { testTypeRobot.checkActivityIsDisplayed() }
            testTypeRobot.clickPositiveButton()
            testTypeRobot.clickContinueButton()

            waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }
            shareKeysInformationRobot.clickContinueButton()

            waitFor { testKitTypeRobot.checkActivityIsDisplayed() }
            testKitTypeRobot.clickLFDButton()
            testKitTypeRobot.clickContinueButton()

            waitFor { testOriginRobot.checkActivityIsDisplayed() }
            testOriginRobot.clickNoButton()
            testOriginRobot.clickContinueButton()

            waitFor { selectTestDateRobot.checkActivityIsDisplayed() }
            selectTestDateRobot.selectCannotRememberDate()
            selectTestDateRobot.clickContinueButton()

            waitFor { symptomsRobot.checkActivityIsDisplayed() }
            symptomsRobot.clickYesButton()
            symptomsRobot.clickContinueButton()

            waitFor { symptomsOnsetRobot.checkActivityIsDisplayed() }
            symptomsOnsetRobot.selectCannotRememberDate()
            symptomsOnsetRobot.clickContinueButton()

            waitFor { checkAnswersRobot.checkActivityIsDisplayed() }
            checkAnswersRobot.clickContinueButton()
            waitFor { thankYouRobot.checkActivityIsDisplayed() }
            thankYouRobot.clickContinue()
            waitFor { adviceRobot.checkActivityIsDisplayed() }
            adviceRobot.clickPrimaryBackToHomeButton()
            waitFor { statusRobot.checkActivityIsDisplayed() }

            assertOnFields {
                assertEquals(1, Metrics::receivedPositiveTestResult)
                assertEquals(1, Metrics::successfullySharedExposureKeys)
                assertEquals(0, Metrics::isPositiveSelfLFDFree)
                assertEquals(1, Metrics::didHaveSymptomsBeforeReceivedTestResult)
                assertEquals(0, Metrics::didRememberOnsetSymptomsDateBeforeReceivedTestResult)
                assertEquals(0, Metrics::selfReportedPositiveSelfLFDOnGov)
                assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::completedSelfReportingTestFlow)
                assertEquals(1, Metrics::startedIsolation)
                assertEquals(1, Metrics::receivedPositiveSelfRapidTestResultEnteredManually)
                assertEquals(1, Metrics::isIsolatingBackgroundTick)
                assertEquals(1, Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick)
                assertEquals(0, Metrics::isIsolatingForTestedPositiveBackgroundTick)
                assertEquals(0, Metrics::receivedPositiveTestResultEnteredManually)
            }
        }
    }

    @Test
    fun enterPositiveTest_withKeySharingLFDNHSTestNoSymptomsAndDidNotReport() {
        runWithFeature(SELF_REPORTING, enabled = true) {
            testAppContext.getExposureNotificationApi().temporaryExposureKeyHistoryResult = Success()
            testAppContext.getExposureNotificationApi().setEnabled(true)
            MockApiModule.behaviour.responseType = ALWAYS_SUCCEED

            startTestActivity<StatusActivity>()

            runBackgroundTasks()

            statusRobot.clickLinkTestResult()
            waitFor { testTypeRobot.checkActivityIsDisplayed() }
            testTypeRobot.clickPositiveButton()
            testTypeRobot.clickContinueButton()

            waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }
            shareKeysInformationRobot.clickContinueButton()

            waitFor { testKitTypeRobot.checkActivityIsDisplayed() }
            testKitTypeRobot.clickLFDButton()
            testKitTypeRobot.clickContinueButton()

            waitFor { testOriginRobot.checkActivityIsDisplayed() }
            testOriginRobot.clickYesButton()
            testOriginRobot.clickContinueButton()

            waitFor { selectTestDateRobot.checkActivityIsDisplayed() }
            selectTestDateRobot.selectCannotRememberDate()
            selectTestDateRobot.clickContinueButton()

            waitFor { symptomsRobot.checkActivityIsDisplayed() }
            symptomsRobot.clickNoButton()
            symptomsRobot.clickContinueButton()

            waitFor { reportedTestRobot.checkActivityIsDisplayed() }
            reportedTestRobot.clickNoButton()
            reportedTestRobot.clickContinueButton()

            waitFor { checkAnswersRobot.checkActivityIsDisplayed() }
            checkAnswersRobot.clickContinueButton()
            waitFor { thankYouRobot.checkActivityIsDisplayed() }
            thankYouRobot.clickContinue()
            waitFor { adviceRobot.checkActivityIsDisplayed() }
            adviceRobot.clickSecondaryBackToHomeButton()
            waitFor { statusRobot.checkActivityIsDisplayed() }

            assertOnFields {
                assertEquals(1, Metrics::receivedPositiveTestResult)
                assertEquals(1, Metrics::successfullySharedExposureKeys)
                assertEquals(1, Metrics::isPositiveSelfLFDFree)
                assertEquals(0, Metrics::didHaveSymptomsBeforeReceivedTestResult)
                assertEquals(0, Metrics::didRememberOnsetSymptomsDateBeforeReceivedTestResult)
                assertEquals(0, Metrics::selfReportedPositiveSelfLFDOnGov)
                assertEquals(1, Metrics::consentedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::askedToShareExposureKeysInTheInitialFlow)
                assertEquals(1, Metrics::completedSelfReportingTestFlow)
                assertEquals(1, Metrics::startedIsolation)
                assertEquals(1, Metrics::receivedPositiveSelfRapidTestResultEnteredManually)
                assertEquals(1, Metrics::isIsolatingBackgroundTick)
                assertEquals(1, Metrics::isIsolatingForTestedSelfRapidPositiveBackgroundTick)
                assertEquals(0, Metrics::isIsolatingForTestedPositiveBackgroundTick)
                assertEquals(0, Metrics::receivedPositiveTestResultEnteredManually)
            }
        }
    }
}
