package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.questionnaire.review.PositiveSymptomsActivity
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.PositiveSymptomsRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.TestOrderingRobot
import java.time.Instant
import java.time.LocalDate

class PositiveSymptomsScenarioTest : EspressoTest() {

    private val positiveSymptomsRobot = PositiveSymptomsRobot()

    private val testOrderingRobot = TestOrderingRobot()

    @Test
    fun pressTestOrderButtonIsShouldNavigateToTestOrderingActivity() = notReported {
        val onsetDate = LocalDate.now()
        testAppContext.setState(
            Isolation(
                Instant.now(), LocalDate.now().plusDays(5), indexCase = IndexCase(onsetDate)
            )
        )

        startTestActivity<PositiveSymptomsActivity>()

        positiveSymptomsRobot.clickTestOrderingButton()

        testOrderingRobot.checkActivityIsDisplayed()
    }
}
