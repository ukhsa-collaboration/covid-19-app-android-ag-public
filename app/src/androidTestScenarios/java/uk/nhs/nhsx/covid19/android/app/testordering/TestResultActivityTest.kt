package uk.nhs.nhsx.covid19.android.app.testordering

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.annotation.StringRes
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.exposure.setTemporaryExposureKeyHistoryResolutionRequired
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.NEGATIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.PLOD
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.VOID
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter.Kind.SCREEN
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.addTestResult
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryFlakyTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysInformationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ShareKeysResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithIntents
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import uk.nhs.nhsx.covid19.android.app.util.IsolationChecker
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class TestResultActivityTest(override val configuration: TestConfiguration) : EspressoTest(),
    LocalAuthoritySetupHelper {

    private val testResultRobot = TestResultRobot(testAppContext.app)
    private val statusRobot = StatusRobot()
    private val shareKeysInformationRobot = ShareKeysInformationRobot()
    private val shareKeysResultRobot = ShareKeysResultRobot()
    private val isolationHelper = IsolationHelper(testAppContext.clock)
    private val isolationChecker = IsolationChecker(testAppContext)

    @Before
    fun setUp() {
        givenLocalAuthorityIsInEngland()
    }

    @Test
    @Reported
    fun showContinueToSelfIsolateScreenOnPositiveConfirmatory() = reporter(
        scenario = "Test result",
        title = "Positive confirmatory in isolation",
        description = "User receives positive confirmatory test result while in isolation",
        kind = SCREEN
    ) {
        givenLocalAuthorityIsInWales()

        testAppContext.setState(isolationHelper.contact().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveContinueIsolation(WALES)
        testResultRobot.checkExposureLinkIsDisplayed()
        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        step(
            stepName = "Positive in isolation",
            stepDescription = "User receives positive test result while in isolation"
        )

        waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    @Reported
    fun showContinueToSelfIsolateScreenOnPositiveConfirmatoryForEngland() = reporter(
        scenario = "Test result",
        title = "Positive confirmatory in isolation",
        description = "User receives positive confirmatory test result while in isolation",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveContinueIsolation(ENGLAND)
        testResultRobot.checkExposureLinkIsDisplayed()
        testResultRobot.checkIsolationActionButtonShowsAnonymouslyNotifyOthers()

        testResultRobot.clickIsolationActionButton()

        step(
            stepName = "Positive in isolation",
            stepDescription = "User receives positive test result while in isolation"
        )

        waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    @Reported
    fun showContinueToSelfIsolateScreenOnNegativeConfirmatoryAndNotIndexCaseOnly() = reporter(
        scenario = "Test result",
        title = "Negative confirmatory in isolation when not at index case only",
        description = "User receives negative confirmatory test result while in isolation when at index case only",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeWillBeInIsolation()
        testResultRobot.checkExposureLinkIsNotDisplayed()
        testResultRobot.checkIsolationActionButtonShowsBackHome()

        testResultRobot.clickIsolationActionButton()

        isolationChecker.assertActiveContactNoIndex()

        step(
            stepName = "Negative confirmatory in isolation when not at index case only",
            stepDescription = "User receives negative confirmatory test result while in isolation"
        )
    }

    @Test
    @Reported
    fun showDoNotHaveToSelfIsolateScreenOnNegativeConfirmatoryAndIndexCaseOnly() = reporter(
        scenario = "Test result",
        title = "Negative confirmatory in isolation at index case only",
        description = "User receives negative confirmatory test result while in isolation when at index case only",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationHelper.selfAssessment().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeWontBeInIsolation()
        testResultRobot.checkGoodNewsActionButtonShowsContinue()

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertExpiredIndexNoContact()

        step(
            stepName = "Negative in isolation",
            stepDescription = "User receives negative test result while in isolation"
        )
    }

    @Test
    fun showIsolationScreenWhenReceivingPositiveConfirmatoryAndThenNegativeConfirmatoryTestResultWales() {
        givenLocalAuthorityIsInWales()
        showIsolationScreenWhenReceivingPositiveConfirmatoryAndThenNegativeConfirmatoryTestResult(WALES)
    }

    @Test
    fun showIsolationScreenWhenReceivingPositiveConfirmatoryAndThenNegativeConfirmatoryTestResultEngland() {
        givenLocalAuthorityIsInEngland()
        showIsolationScreenWhenReceivingPositiveConfirmatoryAndThenNegativeConfirmatoryTestResult(ENGLAND)
    }

    private fun showIsolationScreenWhenReceivingPositiveConfirmatoryAndThenNegativeConfirmatoryTestResult(country: PostCodeDistrict) {
        testAppContext.setState(
            isolationHelper.selfAssessment().asIsolation()
                .addTestResult(
                    testResult = AcknowledgedTestResult(
                        testEndDate = LocalDate.now(),
                        testResult = RelevantVirologyTestResult.POSITIVE,
                        testKitType = LAB_RESULT,
                        requiresConfirmatoryTest = false,
                        acknowledgedDate = LocalDate.now()
                    )
                )
        )

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeAfterPositiveOrSymptomaticWillBeInIsolation(country)

        testResultRobot.checkExposureLinkIsNotDisplayed()

        @StringRes val linkText = if (country == ENGLAND) R.string.nhs_111_online_service else R.string.nhs_111_online_service_wales
        testResultRobot.checkOnlineServiceLinkText(linkText)

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        isolationChecker.assertActiveIndexNoContact()
    }

    @RetryFlakyTest
    @Test
    fun showIsolationScreenWhenReceivingNegativeConfirmatoryAndThenPositiveConfirmatoryTestResultAndSharingKeysForEngland() {
        givenLocalAuthorityIsInEngland()
        testAppContext.setState(
            AcknowledgedTestResult(
                testEndDate = LocalDate.now(),
                testResult = RelevantVirologyTestResult.NEGATIVE,
                testKitType = LAB_RESULT,
                requiresConfirmatoryTest = false,
                acknowledgedDate = LocalDate.now()
            ).asIsolation()
        )

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(country = ENGLAND)

        testResultRobot.checkExposureLinkIsDisplayed()

        testResultRobot.checkIsolationActionButtonShowsAnonymouslyNotifyOthers()

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    @RetryFlakyTest
    @Test
    fun showIsolationScreenWhenReceivingNegativeConfirmatoryAndThenPositiveConfirmatoryTestResultAndRefusingToShareKeys() {
        givenLocalAuthorityIsInWales()

        testAppContext.setTemporaryExposureKeyHistoryResolutionRequired(testAppContext.app, false)

        testAppContext.setState(isolationHelper.neverInIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(country = WALES)

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    fun showIsolationScreenOnPositiveConfirmatoryWhenRecentlyIsolated() {
        testAppContext.setState(isolationHelper.selfAssessment(expired = true).asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(country = ENGLAND)

        testResultRobot.checkExposureLinkIsDisplayed()

        testResultRobot.checkIsolationActionButtonShowsAnonymouslyNotifyOthers()

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    fun showIsolationScreenOnPositiveConfirmatoryWhenRecentlyIsolatedWales() {
        givenLocalAuthorityIsInWales()
        testAppContext.setState(isolationHelper.selfAssessment(expired = true).asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(country = WALES)

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()

        shareKeysInformationRobot.checkActivityIsDisplayed()

        shareKeysInformationRobot.clickContinueButton()

        waitFor { shareKeysResultRobot.checkActivityIsDisplayed() }

        shareKeysResultRobot.clickActionButton()

        waitFor { statusRobot.checkActivityIsDisplayed() }

        isolationChecker.assertActiveIndexNoContact()
    }

    @Test
    fun showDoNotHaveToSelfIsolateScreenOnNegativeConfirmatory() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation(ENGLAND)

        testResultRobot.checkGoodNewsActionButtonShowsContinue()

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertNeverIsolating()
    }

    @Test
    @Reported
    fun showContinueToSelfIsolateScreenOnVoidConfirmatory() = reporter(
        scenario = "Test result",
        title = "Void confirmatory in isolation",
        description = "User receives void confirmatory test result while in isolation",
        kind = SCREEN
    ) {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = VOID,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysVoidWillBeInIsolation(ENGLAND)
        testResultRobot.checkExposureLinkIsNotDisplayed()
        testResultRobot.checkIsolationActionButtonShowsBackHome()

        testResultRobot.clickIsolationActionButton()

        isolationChecker.assertActiveContactNoIndex()

        step(
            stepName = "Void confirmatory in isolation",
            stepDescription = "User receives void confirmatory test result while in isolation"
        )
    }

    @Test
    fun showNegativeAlreadyNotInIsolationOnNegativeConfirmatory() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = NEGATIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysNegativeAlreadyNotInIsolation(ENGLAND)
        testResultRobot.checkGoodNewsActionButtonShowsContinue()

        isolationChecker.assertNeverIsolating()
    }

    @Test
    fun showAreNotIsolatingScreenOnVoidConfirmatory() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = VOID,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysVoidNotInIsolation(ENGLAND)

        testResultRobot.checkGoodNewsActionButtonShowsBackHome()

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertNeverIsolating()
    }

    @Test
    fun showPlodScreenOnPlodResult() {
        testAppContext.setState(isolationHelper.neverInIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = PLOD,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPlodScreen(ENGLAND)

        testResultRobot.checkGoodNewsActionButtonShowsBackHome()

        testResultRobot.clickGoodNewsActionButton()

        isolationChecker.assertNeverIsolating()
    }

    @Test
    fun onVoidTestResultConfirmAction_finish() {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = VOID,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = false,
                requiresConfirmatoryTest = false
            )
        )

        val activity = startTestActivity<TestResultActivity>()
        testResultRobot.clickIsolationActionButton()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun onActivityResultTestOrderingNotOk_finish() {
        runWithIntents {
            testAppContext.setState(isolationHelper.contact().asIsolation())

            testAppContext.getUnacknowledgedTestResultsProvider().add(
                ReceivedTestResult(
                    diagnosisKeySubmissionToken = "a",
                    testEndDate = Instant.now(),
                    testResult = VOID,
                    testKitType = LAB_RESULT,
                    diagnosisKeySubmissionSupported = true,
                    requiresConfirmatoryTest = false
                )
            )

            val result = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, Intent())
            intending(hasComponent(TestOrderingActivity::class.qualifiedName))
                .respondWith(result)

            val activity = startTestActivity<TestResultActivity>()
            testResultRobot.clickIsolationActionButton()

            waitFor { assertTrue(activity!!.isDestroyed) }
        }
    }

    @Test
    fun onBackPressed_navigate() {
        testAppContext.setState(isolationHelper.contact().asIsolation())

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a",
                testEndDate = Instant.now(),
                testResult = VOID,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        val activity = startTestActivity<TestResultActivity>()

        testAppContext.device.pressBack()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @RetryFlakyTest
    @Test
    fun showIsolationAdviceScreenWhenReceivingPositiveConfirmatoryTestResultAndSharingKeysForEngland() {
        givenLocalAuthorityIsInEngland()

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(country = ENGLAND)

        testResultRobot.checkExposureLinkIsDisplayed()

        testResultRobot.checkIsolationActionButtonShowsAnonymouslyNotifyOthers()

        testResultRobot.clickIsolationActionButton()

        waitFor { shareKeysInformationRobot.checkActivityIsDisplayed() }
    }

    @RetryFlakyTest
    @Test
    fun showIsolationAdviceScreenWhenReceivingPositiveUnConfirmedTestResultAndDoNotRequireConfirmatoryTestForEngland() {
        givenLocalAuthorityIsInEngland()

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = RAPID_RESULT,
                diagnosisKeySubmissionSupported = false,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(country = ENGLAND)

        testResultRobot.checkExposureLinkIsDisplayed()

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()
    }

    @Test
    fun showSelfIsolateScreenWhenReceivingPositiveConfirmatoryTestResultWales() {
        givenLocalAuthorityIsInWales()

        testAppContext.getUnacknowledgedTestResultsProvider().add(
            ReceivedTestResult(
                diagnosisKeySubmissionToken = "a2",
                testEndDate = Instant.now(),
                testResult = POSITIVE,
                testKitType = LAB_RESULT,
                diagnosisKeySubmissionSupported = true,
                requiresConfirmatoryTest = false
            )
        )

        startTestActivity<TestResultActivity>()

        testResultRobot.checkActivityDisplaysPositiveWillBeInIsolation(country = WALES)

        testResultRobot.checkExposureLinkIsNotDisplayed()

        testResultRobot.checkIsolationActionButtonShowsContinue()

        testResultRobot.clickIsolationActionButton()
    }
}
