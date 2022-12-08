package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration
import uk.nhs.nhsx.covid19.android.app.state.IsolationHelper
import uk.nhs.nhsx.covid19.android.app.state.addTestResult
import uk.nhs.nhsx.covid19.android.app.state.asIsolation
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportAdviceRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import uk.nhs.nhsx.covid19.android.app.testordering.AcknowledgedTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.RelevantVirologyTestResult.POSITIVE
import java.time.LocalDate

class SelfReportAdviceActivityTest : EspressoTest(), LocalAuthoritySetupHelper {

    private val selfReportAdviceRobot = SelfReportAdviceRobot()

    @Test
    fun hasReportedNotInIsolationEngland() {
        givenLocalAuthorityIsInEngland()
        startActivityWithExtras(true)

        selfReportAdviceRobot.checkActivityIsDisplayed()

        selfReportAdviceRobot.checkDisplaysHasReportedNoNeedToIsolate()

        selfReportAdviceRobot.checkReadMoreLinkEngland()
    }

    @Test
    fun hasReportedNotInIsolationWales() {
        givenLocalAuthorityIsInWales()
        startActivityWithExtras(true)

        selfReportAdviceRobot.checkActivityIsDisplayed()

        selfReportAdviceRobot.checkDisplaysHasReportedNoNeedToIsolate()

        selfReportAdviceRobot.checkReadMoreLinkWales()
    }

    @Test
    fun hasNotReportedNotInIsolationEngland() {
        givenLocalAuthorityIsInEngland()
        startActivityWithExtras(false)

        selfReportAdviceRobot.checkActivityIsDisplayed()

        selfReportAdviceRobot.checkDisplaysHasNotReportedNoNeedToIsolate()

        selfReportAdviceRobot.checkReadMoreLinkEngland()
    }

    @Test
    fun hasNotReportedNotInIsolationWales() {
        givenLocalAuthorityIsInWales()
        startActivityWithExtras(false)

        selfReportAdviceRobot.checkActivityIsDisplayed()

        selfReportAdviceRobot.checkDisplaysHasNotReportedNoNeedToIsolate()

        selfReportAdviceRobot.checkReadMoreLinkWales()
    }

    @Test
    fun hasReportedInIsolationEngland() {
        givenLocalAuthorityIsInEngland()
        val isolationHelper = IsolationHelper(testAppContext.clock, englandConfiguration)
        testAppContext.setState(
            isolationHelper.selfAssessment(onsetDate = LocalDate.now()).asIsolation(isolationConfiguration = englandConfiguration)
                .addTestResult(
                    testResult = AcknowledgedTestResult(
                        testEndDate = LocalDate.now(),
                        testResult = POSITIVE,
                        testKitType = LAB_RESULT,
                        acknowledgedDate = LocalDate.now()
                    )
                )
        )
        startActivityWithExtras(true)

        selfReportAdviceRobot.checkActivityIsDisplayed()

        selfReportAdviceRobot.checkDisplaysHasReportedIsolate(englandConfiguration.indexCaseSinceSelfDiagnosisOnset)

        selfReportAdviceRobot.checkReadMoreLinkEngland()
    }

    @Test
    fun hasReportedInIsolationWales() {
        givenLocalAuthorityIsInWales()
        val isolationHelper = IsolationHelper(testAppContext.clock, walesConfiguration)
        testAppContext.setState(
            isolationHelper.selfAssessment(onsetDate = LocalDate.now()).asIsolation(isolationConfiguration = walesConfiguration)
                .addTestResult(
                    testResult = AcknowledgedTestResult(
                        testEndDate = LocalDate.now(),
                        testResult = POSITIVE,
                        testKitType = LAB_RESULT,
                        acknowledgedDate = LocalDate.now()
                    )
                )
        )
        startActivityWithExtras(true)

        selfReportAdviceRobot.checkActivityIsDisplayed()

        selfReportAdviceRobot.checkDisplaysHasReportedIsolate(walesConfiguration.indexCaseSinceSelfDiagnosisOnset)

        selfReportAdviceRobot.checkReadMoreLinkWales()
    }

    @Test
    fun hasNotReportedInIsolationEngland() {
        givenLocalAuthorityIsInEngland()
        val isolationHelper = IsolationHelper(testAppContext.clock, englandConfiguration)
        testAppContext.setState(
            isolationHelper.selfAssessment(onsetDate = LocalDate.now()).asIsolation(isolationConfiguration = englandConfiguration)
                .addTestResult(
                    testResult = AcknowledgedTestResult(
                        testEndDate = LocalDate.now(),
                        testResult = POSITIVE,
                        testKitType = RAPID_SELF_REPORTED,
                        acknowledgedDate = LocalDate.now()
                    )
                )
        )
        startActivityWithExtras(false)

        selfReportAdviceRobot.checkActivityIsDisplayed()

        selfReportAdviceRobot.checkDisplaysHasNotReportedIsolate(
            expectedDaysLeft = englandConfiguration.indexCaseSinceSelfDiagnosisOnset,
            expectedEndDate = LocalDate.now().plusDays(englandConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong()))

        selfReportAdviceRobot.checkReadMoreLinkEngland()
    }

    @Test
    fun hasNotReportedInIsolationWales() {
        givenLocalAuthorityIsInWales()
        val isolationHelper = IsolationHelper(testAppContext.clock, walesConfiguration)
        testAppContext.setState(
            isolationHelper.selfAssessment(onsetDate = LocalDate.now()).asIsolation(isolationConfiguration = walesConfiguration)
                .addTestResult(
                    testResult = AcknowledgedTestResult(
                        testEndDate = LocalDate.now(),
                        testResult = POSITIVE,
                        testKitType = RAPID_SELF_REPORTED,
                        acknowledgedDate = LocalDate.now()
                    )
                )
        )
        startActivityWithExtras(false)

        selfReportAdviceRobot.checkActivityIsDisplayed()

        selfReportAdviceRobot.checkDisplaysHasNotReportedIsolate(
            expectedDaysLeft = walesConfiguration.indexCaseSinceSelfDiagnosisOnset,
            expectedEndDate = LocalDate.now().plusDays(walesConfiguration.indexCaseSinceSelfDiagnosisOnset.toLong()))

        selfReportAdviceRobot.checkReadMoreLinkWales()
    }

    private fun startActivityWithExtras(hasReportedTest: Boolean) {
        startTestActivity<SelfReportAdviceActivity> {
            putExtra(
                SelfReportAdviceActivity.REPORTED_TEST_DATA_KEY, hasReportedTest
            )
        }
    }

    private val walesConfiguration = IsolationConfiguration(
        contactCase = 11,
        indexCaseSinceSelfDiagnosisOnset = 5,
        indexCaseSinceSelfDiagnosisUnknownOnset = 5,
        maxIsolation = 16,
        indexCaseSinceTestResultEndDate = 5,
        pendingTasksRetentionPeriod = 14,
        testResultPollingTokenRetentionPeriod = 28
    )

    private val englandConfiguration = IsolationConfiguration(
        contactCase = 11,
        indexCaseSinceSelfDiagnosisOnset = 6,
        indexCaseSinceSelfDiagnosisUnknownOnset = 6,
        maxIsolation = 16,
        indexCaseSinceTestResultEndDate = 6,
        pendingTasksRetentionPeriod = 14,
        testResultPollingTokenRetentionPeriod = 28
    )
}
