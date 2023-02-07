package uk.nhs.nhsx.covid19.android.app.flow

import com.jeroenmols.featureflag.framework.FeatureFlag.SELF_REPORTING
import com.jeroenmols.featureflag.framework.FeatureFlag.TESTING_FOR_COVID19_HOME_SCREEN_BUTTON
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import kotlinx.coroutines.runBlocking
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.ExposureCircuitBreakerInfo
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.OrderTest
import uk.nhs.nhsx.covid19.android.app.flow.functionalities.SelfDiagnosis
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.MANUAL_CONFIGURATION_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi.Companion.NEGATIVE_PCR_TOKEN
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.IsolationLogicalState.PossiblyIsolating
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.status.StatusActivity
import uk.nhs.nhsx.covid19.android.app.testhelpers.AWAIT_AT_MOST_SECONDS
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ExposureNotificationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestingHubRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeature
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlowTests : EspressoTest(), LocalAuthoritySetupHelper {

    private val statusRobot = StatusRobot()
    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val linkTestResultRobot = LinkTestResultRobot()
    private val exposureNotificationRobot = ExposureNotificationRobot()
    private val orderTest = OrderTest(this)
    private val selfDiagnosis = SelfDiagnosis(this)
    private val testingHubRobot = TestingHubRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)

    @Before
    fun setUp() {
        FeatureFlagTestHelper.clearFeatureFlags()
        FeatureFlagTestHelper.enableFeatureFlag(USE_WEB_VIEW_FOR_INTERNAL_BROWSER)

        givenLocalAuthorityIsInWales()
    }

    @After
    fun tearDown() {
        FeatureFlagTestHelper.clearFeatureFlags()
        testAppContext.clock.reset()
    }

    @Test
    fun whenSelfIsolationForWalesIsEnabled_startDefault_selfDiagnose_hasNegativeLFDTest_notInIsolation_forWales() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            val walesConfiguration = IsolationConfiguration(
                contactCase = 11,
                indexCaseSinceSelfDiagnosisOnset = 5,
                indexCaseSinceSelfDiagnosisUnknownOnset = 5,
                maxIsolation = 16,
                indexCaseSinceTestResultEndDate = 5,
                pendingTasksRetentionPeriod = 14,
                testResultPollingTokenRetentionPeriod = 28
            )
            val isolationHelper = IsolationHelper(testAppContext.clock, walesConfiguration)

            givenLocalAuthorityIsInWales()
            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            assertEquals(isolationHelper.neverInIsolation(), testAppContext.getCurrentState())

            selfDiagnosis.selfDiagnosePositiveAndPressBack()

            waitFor { statusRobot.checkIsolationViewIsDisplayed() }

            statusRobot.checkIsolationViewHasCorrectContentDescriptionForWales(
                (testAppContext.getCurrentLogicalState() as PossiblyIsolating).expiryDate.minusDays(
                    1
                )
            )

            statusRobot.clickLinkTestResult()

            linkTestResultRobot.checkActivityIsDisplayed()

            linkTestResultRobot.enterCtaToken(MANUAL_CONFIGURATION_TOKEN)

            linkTestResultRobot.clickContinue()

            waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

            testResultRobot.clickGoodNewsActionButton()

            await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
                !testAppContext.getCurrentLogicalState().isActiveIsolation(testAppContext.clock)
            }
        }

    @Test
    fun whenSelfIsolationForWalesIsDisabled_startDefault_selfDiagnose_hasNegativeLFDTest_notInIsolation_forWales() =
        runWithFeature(SELF_REPORTING, enabled = false) {
            val walesConfiguration = IsolationConfiguration(
                contactCase = 11,
                indexCaseSinceSelfDiagnosisOnset = 5,
                indexCaseSinceSelfDiagnosisUnknownOnset = 5,
                maxIsolation = 16,
                indexCaseSinceTestResultEndDate = 5,
                pendingTasksRetentionPeriod = 14,
                testResultPollingTokenRetentionPeriod = 28
            )
            val isolationHelper = IsolationHelper(testAppContext.clock, walesConfiguration)

            givenLocalAuthorityIsInWales()
            startTestActivity<StatusActivity>()

            statusRobot.checkActivityIsDisplayed()

            assertEquals(isolationHelper.neverInIsolation(), testAppContext.getCurrentState())

            selfDiagnosis.selfDiagnosePositiveAndPressBackIsolationDisabled()

            waitFor { statusRobot.checkIsolationViewIsNotDisplayed() }

            statusRobot.clickLinkTestResult()

            linkTestResultRobot.checkActivityIsDisplayed()

            linkTestResultRobot.enterCtaToken(MANUAL_CONFIGURATION_TOKEN)

            linkTestResultRobot.clickContinue()

            waitFor { testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation(WALES) }

            testResultRobot.clickGoodNewsActionButton()

            await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
                !testAppContext.getCurrentLogicalState().isActiveIsolation(testAppContext.clock)
            }
        }

    @Test
    fun startIndexCase_receivePositiveTestResult_inIndexIsolation() =
        runWithFeatureEnabled(TESTING_FOR_COVID19_HOME_SCREEN_BUTTON) {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue(isActiveIndexNoContact())

        statusRobot.clickTestingHub()

        testingHubRobot.checkActivityIsDisplayed()
        testingHubRobot.clickBookTest()

        orderTest()

        testAppContext.virologyTestingApi.setDefaultTestResponse(POSITIVE)

        runBlocking {
            testAppContext.getDownloadVirologyTestResultWork().invoke()
        }

        waitFor { testResultRobot.checkActivityDisplaysPositiveContinueIsolation(PostCodeDistrict.WALES) }

        testResultRobot.clickIsolationActionButton()

        assertTrue(isActiveIndexNoContact())
    }

    @Test
    fun startIndexCaseWithSelfAssessment_receiveExposureNotification_inIndexAndContactIsolation() {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue(isActiveIndexNoContact())

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            isActiveIndexAndContact()
        }

        waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startIndexCaseWithPositiveIndicative_receiveExposureNotification_inIndexAndContactIsolation() {
        val dateNow = LocalDate.now()
        testAppContext.setState(
            AcknowledgedTestResult(
                testEndDate = LocalDate.now(),
                testResult = RelevantVirologyTestResult.POSITIVE,
                testKitType = RAPID_RESULT,
                requiresConfirmatoryTest = true,
                acknowledgedDate = LocalDate.now()
            ).asIsolation()
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue(isActiveIndexNoContact())

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        await.atMost(AWAIT_AT_MOST_SECONDS, SECONDS) until {
            isActiveIndexAndContact()
        }

        waitFor { exposureNotificationRobot.checkActivityIsDisplayed() }

        val contactCaseDays =
            testAppContext.getIsolationConfigurationProvider().durationDays.wales.contactCase
        val expectedExpiryDate = dateNow.plus(contactCaseDays.toLong(), DAYS)
        val actualExpiryDate = (testAppContext.getCurrentLogicalState() as PossiblyIsolating).expiryDate

        assertEquals(expectedExpiryDate, actualExpiryDate)
    }

    @Test
    fun startIndexCaseWithPositiveConfirmed_receiveExposureNotification_remainIndexCase() {
        testAppContext.setState(
            AcknowledgedTestResult(
                testEndDate = LocalDate.now(),
                testResult = RelevantVirologyTestResult.POSITIVE,
                testKitType = RAPID_RESULT,
                requiresConfirmatoryTest = false,
                acknowledgedDate = LocalDate.now()
            ).asIsolation()
        )

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        assertTrue(isActiveIndexNoContact())

        testAppContext.getExposureCircuitBreakerInfoProvider().add(exposureCircuitBreakerInfo)
        runBackgroundTasks()

        assertTrue(isActiveIndexNoContact())

        waitFor { statusRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun startIndexCase_linkNegativeTestResult() = runWithFeature(SELF_REPORTING, enabled = false) {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        startTestActivity<StatusActivity>()

        statusRobot.checkActivityIsDisplayed()

        waitFor { statusRobot.checkIsolationViewIsDisplayed() }

        assertTrue(isActiveIndexNoContact())

        statusRobot.clickLinkTestResult()

        linkTestResultRobot.checkActivityIsDisplayed()

        linkTestResultRobot.enterCtaToken(NEGATIVE_PCR_TOKEN)

        linkTestResultRobot.clickContinue()

        waitFor { testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation() }

        testResultRobot.clickGoodNewsActionButton()

        statusRobot.checkActivityIsDisplayed()

        statusRobot.checkIsolationViewIsNotDisplayed()

        assertTrue(isExpiredIndexNoContact())
    }

    private fun isActiveIndexAndContact(): Boolean {
        val state = testAppContext.getCurrentLogicalState()
        return state.isActiveIndexCase(testAppContext.clock) &&
                state.isActiveContactCase(testAppContext.clock)
    }

    private fun isActiveIndexNoContact(): Boolean {
        val state = testAppContext.getCurrentLogicalState()
        return state.isActiveIndexCase(testAppContext.clock) &&
                !state.remembersContactCase()
    }

    private fun isExpiredIndexNoContact(): Boolean {
        val state = testAppContext.getCurrentLogicalState()
        return state.remembersIndexCase() &&
                !state.isActiveIndexCase(testAppContext.clock) &&
                !state.remembersContactCase()
    }

    companion object {
        private val exposureCircuitBreakerInfo = ExposureCircuitBreakerInfo(
            maximumRiskScore = 10.0,
            startOfDayMillis = Instant.now().toEpochMilli(),
            matchedKeyCount = 1,
            riskCalculationVersion = 2,
            exposureNotificationDate = Instant.now().toEpochMilli()
        )
    }
}
