package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.LAB_RESULT
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType.RAPID_SELF_REPORTED
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult.POSITIVE
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ProgressRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.ReportedTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelectTestDateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportCheckAnswersRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportSymptomsOnsetRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SelfReportSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestKitTypeRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOriginRobot
import java.time.LocalDate

class SelfReportCheckAnswersActivityTest : EspressoTest() {

    private val testKitTypeRobot = TestKitTypeRobot()
    private val testOriginRobot = TestOriginRobot()
    private val selectTestDateRobot = SelectTestDateRobot()
    private val selfReportSymptomsRobot = SelfReportSymptomsRobot()
    private val selfReportSymptomsOnsetRobot = SelfReportSymptomsOnsetRobot()
    private val reportedTestRobot = ReportedTestRobot()
    private val selfReportCheckAnswersRobot = SelfReportCheckAnswersRobot()
    private val selfReportSubmitTestResultAndKeysProgressRobot = ProgressRobot()

    @Test
    fun testKitTypeIsPCR_PCRIsDisplayed() {
        startActivityWithExtras(selfReportTestQuestions.copy(testKitType = LAB_RESULT))

        waitFor { selfReportCheckAnswersRobot.checkTestKitTypeHasPCRAnswer() }
    }

    @Test
    fun testKitTypeIsLFD_LFDIsDisplayed_clickingChangeShouldSendToTestKitTypeActivity() {
        startActivityWithExtras()

        waitFor { selfReportCheckAnswersRobot.checkTestKitTypeHasLFDAnswer() }
        waitFor { selfReportCheckAnswersRobot.clickTestKitTypeChangeButton() }
        waitFor { testKitTypeRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun testOriginIsNull_testOriginAnswerShouldBeHidden() {
        startActivityWithExtras(selfReportTestQuestions.copy(isNHSTest = null))

        selfReportCheckAnswersRobot.checkActivityIsDisplayed()

        waitFor { selfReportCheckAnswersRobot.checkTestOriginIsHidden() }
    }

    @Test
    fun testOriginIsNHS_testOriginAnswerShouldBeYes() {
        startActivityWithExtras(selfReportTestQuestions.copy(isNHSTest = true))

        waitFor { selfReportCheckAnswersRobot.checkTestOriginHasYesAnswer() }
    }

    @Test
    fun testOriginIsNotNHS_testOriginAnswerShouldBeNo_clickingChangeShouldSendToTestOriginActivity() {
        startActivityWithExtras(selfReportTestQuestions.copy(isNHSTest = false))

        waitFor { selfReportCheckAnswersRobot.checkTestOriginHasNoAnswer() }
        waitFor { selfReportCheckAnswersRobot.clickTestOriginChangeButton() }
        waitFor { testOriginRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun testDateIsCannotRemember_cannotRememberDisplayed() {
        startActivityWithExtras(selfReportTestQuestions.copy(testEndDate = ChosenDate(false, LocalDate.now())))

        waitFor { selfReportCheckAnswersRobot.checkTestDateHasCannotRememberAnswer() }
    }

    @Test
    fun testDateIsSpecificDate_specificDateDisplayed_clickingChangeShouldSendToTestDateActivity() {
        startActivityWithExtras(selfReportTestQuestions.copy(testEndDate = ChosenDate(true, LocalDate.now())))

        waitFor { selfReportCheckAnswersRobot.checkTestDateHasSpecificDateAnswer() }
        waitFor { selfReportCheckAnswersRobot.clickTestDateChangeButton() }
        waitFor { selectTestDateRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun symptomsIsNull_symptomsAnswerShouldBeHidden() {
        startActivityWithOnlyNecessaryAnswers()

        selfReportCheckAnswersRobot.checkActivityIsDisplayed()

        waitFor { selfReportCheckAnswersRobot.checkSymptomsIsHidden() }
    }

    @Test
    fun symptomsHasNoAnswer_symptomsShouldBeNo() {
        startActivityWithExtras(selfReportTestQuestions.copy(hadSymptoms = false))

        waitFor { selfReportCheckAnswersRobot.checkSymptomsHasNoAnswer() }
    }

    @Test
    fun symptomsHasYesAnswer_symptomsShouldBeYes_clickingChangeShouldSendToSymptomsActivity() {
        startActivityWithExtras(selfReportTestQuestions.copy(hadSymptoms = true))

        waitFor { selfReportCheckAnswersRobot.checkSymptomsHasYesAnswer() }
        waitFor { selfReportCheckAnswersRobot.clickSymptomsChangeButton() }
        waitFor { selfReportSymptomsRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun symptomsOnsetIsNull_symptomsOnsetAnswerShouldBeHidden() {
        startActivityWithExtras(selfReportTestQuestions.copy(symptomsOnsetDate = null))

        selfReportCheckAnswersRobot.checkActivityIsDisplayed()

        waitFor { selfReportCheckAnswersRobot.checkSymptomsOnsetIsHidden() }
    }

    @Test
    fun symptomsOnsetIsCannotRemember_cannotRememberDisplayed() {
        startActivityWithExtras(selfReportTestQuestions.copy(symptomsOnsetDate = ChosenDate(false, LocalDate.now())))

        waitFor { selfReportCheckAnswersRobot.checkSymptomsOnsetHasCannotRememberAnswer() }
    }

    @Test
    fun symptomsOnsetIsSpecificDate_specificDateDisplayed_clickingChangeShouldSendToSymptomsOnsetActivity() {
        startActivityWithExtras(selfReportTestQuestions.copy(symptomsOnsetDate = ChosenDate(true, LocalDate.now())))

        waitFor { selfReportCheckAnswersRobot.checkSymptomsOnsetHasSpecificDateAnswer() }
        waitFor { selfReportCheckAnswersRobot.clickSymptomsOnsetChangeButton() }
        waitFor { selfReportSymptomsOnsetRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun reportedTestIsNull_reportedTestAnswerShouldBeHidden() {
        startActivityWithExtras(selfReportTestQuestions.copy(hasReportedResult = null))

        selfReportCheckAnswersRobot.checkActivityIsDisplayed()

        waitFor { selfReportCheckAnswersRobot.checkReportedTestIsHidden() }
    }

    @Test
    fun reportedTestIsYes_reportedTestAnswerShouldBeYes() {
        startActivityWithExtras(selfReportTestQuestions.copy(hasReportedResult = true))

        waitFor { selfReportCheckAnswersRobot.checkReportedTestHasYesAnswer() }
    }

    @Test
    fun reportedTestIsNo_reportedTestAnswerShouldBeNo_clickingChangeShouldSendToReportedTestActivity() {
        startActivityWithExtras(selfReportTestQuestions.copy(hasReportedResult = false))

        waitFor { selfReportCheckAnswersRobot.checkReportedTestHasNoAnswer() }
        waitFor { selfReportCheckAnswersRobot.clickReportedTestChangeButton() }
        waitFor { reportedTestRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressBackWithLFDTestFromNHS_goToReportedTestActivity() {
        startActivityWithExtras()

        selfReportCheckAnswersRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { reportedTestRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressBackWithSymptomsAndTestNotFromNHS_goToSymptomsOnsetActivity() {
        startActivityWithSymptomsAndNonNHSTest()

        selfReportCheckAnswersRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { selfReportSymptomsOnsetRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressBackWithoutSymptomsAndTestNotFromNHS_goToSymptomsActivity() {
        startActivityWithoutSymptomsAndNonNHSTest()

        selfReportCheckAnswersRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { selfReportSymptomsRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressBackWithoutSymptomsAnswerAndTestNotFromNHS_goToTestDateActivity() {
        startActivityWithoutSymptomsAnswerAndNonNHSTest()

        selfReportCheckAnswersRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { selectTestDateRobot.checkActivityIsDisplayed() }
    }

    @Test
    fun pressSubmitAndContinue_shouldSendToSubmitTestResultAndKeysProgressActivity() {
        startActivityWithExtras()

        selfReportCheckAnswersRobot.checkActivityIsDisplayed()

        MockApiModule.behaviour.delayMillis = 1000
        selfReportCheckAnswersRobot.clickContinueButton()

        waitFor { selfReportSubmitTestResultAndKeysProgressRobot.checkActivityIsDisplayed() }
    }

    private fun startActivityWithExtras() {
        startTestActivity<SelfReportCheckAnswersActivity> {
            putExtra(
                SelfReportCheckAnswersActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private fun startActivityWithExtras(selfReportTestQuestions: SelfReportTestQuestions) {
        startTestActivity<SelfReportCheckAnswersActivity> {
            putExtra(
                SelfReportCheckAnswersActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
            )
        }
    }

    private fun startActivityWithSymptomsAndNonNHSTest() {
        startTestActivity<SelfReportCheckAnswersActivity> {
            putExtra(
                SelfReportCheckAnswersActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
                    .copy(isNHSTest = false, hasReportedResult = null)
            )
        }
    }

    private fun startActivityWithoutSymptomsAndNonNHSTest() {
        startTestActivity<SelfReportCheckAnswersActivity> {
            putExtra(
                SelfReportCheckAnswersActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
                .copy(hadSymptoms = false, symptomsOnsetDate = null, isNHSTest = false, hasReportedResult = null)
            )
        }
    }

    private fun startActivityWithoutSymptomsAnswerAndNonNHSTest() {
        startTestActivity<SelfReportCheckAnswersActivity> {
            putExtra(
                SelfReportCheckAnswersActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
                    .copy(hadSymptoms = null, symptomsOnsetDate = null, isNHSTest = false, hasReportedResult = null)
            )
        }
    }

    private fun startActivityWithOnlyNecessaryAnswers() {
        startTestActivity<SelfReportCheckAnswersActivity> {
            putExtra(
                SelfReportCheckAnswersActivity.SELF_REPORT_QUESTIONS_DATA_KEY, selfReportTestQuestions
                    .copy(testKitType = LAB_RESULT, hadSymptoms = null, symptomsOnsetDate = null, isNHSTest = null, hasReportedResult = null)
            )
        }
    }

    private val selfReportTestQuestions = SelfReportTestQuestions(
        POSITIVE,
        temporaryExposureKeys = listOf(
            NHSTemporaryExposureKey(
                key = "key",
                rollingStartNumber = 2
            )
        ),
        RAPID_SELF_REPORTED,
        true,
        ChosenDate(true, LocalDate.now(testAppContext.clock)),
        true,
        ChosenDate(false, LocalDate.now(testAppContext.clock)),
        true
    )
}
