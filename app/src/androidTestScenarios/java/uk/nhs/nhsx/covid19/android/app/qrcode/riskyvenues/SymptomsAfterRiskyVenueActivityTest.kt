package uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.OrderLfdTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAfterRiskyVenueRobot

class SymptomsAfterRiskyVenueActivityTest : EspressoTest() {

    private val symptomsAfterRiskyVenueVisitRobot = SymptomsAfterRiskyVenueRobot()
    private val questionnaireRobot = QuestionnaireRobot()
    private val orderLfdTestRobot = OrderLfdTestRobot()

    @Test
    fun whenHasSymptomsIsClicked_thenNavigateToQuestionnaire() {
        startTestActivity<SymptomsAfterRiskyVenueActivity>()

        symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed()

        symptomsAfterRiskyVenueVisitRobot.clickHasSymptomsButton()

        questionnaireRobot.checkActivityIsDisplayed()

        questionnaireRobot.clickCloseButton()

        symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed()
    }

    @Test
    fun whenHasSymptomsIsClicked_thenNavigateToQuestionnaireWhichFails() {
        testAppContext.executeWhileOffline {
            startTestActivity<SymptomsAfterRiskyVenueActivity>()

            symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed()

            symptomsAfterRiskyVenueVisitRobot.clickHasSymptomsButton()

            questionnaireRobot.checkErrorStateIsDisplayed()

            questionnaireRobot.clickCloseButton()

            symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed()
        }
    }

    @Test
    fun whenNoSymptomsIsClicked_thenNavigateToOrderLfdTest() {
        startTestActivity<SymptomsAfterRiskyVenueActivity>()

        symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed()

        symptomsAfterRiskyVenueVisitRobot.clickNoSymptomsButton()

        orderLfdTestRobot.checkActivityIsDisplayed()
    }
}
