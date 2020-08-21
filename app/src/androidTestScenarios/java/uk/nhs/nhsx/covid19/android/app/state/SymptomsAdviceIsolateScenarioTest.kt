package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.SymptomsAdviceIsolateActivity
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAdviceIsolateRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot

class SymptomsAdviceIsolateScenarioTest : EspressoTest() {

    private val symptomsAdviceIsolateRobot = SymptomsAdviceIsolateRobot()

    private val testOrderingRobot = TestOrderingRobot()

    private val statusRobot = StatusRobot()

    @Test
    fun pressTestOrderButtonIsShouldNavigateToTestOrderingActivity() = notReported {
        startTestActivity<SymptomsAdviceIsolateActivity> {
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_IS_POSITIVE_SYMPTOMS, true)
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_DURATION, 14)
        }

        symptomsAdviceIsolateRobot.clickBottomActionButton()

        testOrderingRobot.checkActivityIsDisplayed()
    }

    @Test
    fun pressBackToHomeButtonShouldNavigateToStatusActivity() = notReported {
        startTestActivity<SymptomsAdviceIsolateActivity> {
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_IS_POSITIVE_SYMPTOMS, false)
            putExtra(SymptomsAdviceIsolateActivity.EXTRA_ISOLATION_DURATION, 14)
        }

        symptomsAdviceIsolateRobot.clickBottomActionButton()

        statusRobot.checkActivityIsDisplayed()
    }
}
