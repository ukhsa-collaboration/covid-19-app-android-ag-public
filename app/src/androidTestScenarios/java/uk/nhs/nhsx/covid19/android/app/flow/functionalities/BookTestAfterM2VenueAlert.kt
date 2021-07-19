package uk.nhs.nhsx.covid19.android.app.flow.functionalities

import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.BrowserRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.OrderLfdTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.QuestionnaireRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.StatusRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.SymptomsAfterRiskyVenueRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.VenueAlertBookTestRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor

class BookTestAfterM2VenueAlert {

    private val statusRobot = StatusRobot()
    private val venueAlertBookTestRobot = VenueAlertBookTestRobot()
    private val symptomsAfterRiskyVenueVisitRobot = SymptomsAfterRiskyVenueRobot()
    private val questionnaireRobot = QuestionnaireRobot()
    private val orderLfdTestRobot = OrderLfdTestRobot()
    private val browserRobot = BrowserRobot()

    operator fun invoke(
        bookTest: Boolean = false,
        hasSymptoms: Boolean = false,
        alreadyHasLfdTests: Boolean = false,
    ) {
        waitFor { venueAlertBookTestRobot.checkActivityIsDisplayed() }
        if (bookTest) {
            venueAlertBookTestRobot.clickBookTestButton()
            waitFor { symptomsAfterRiskyVenueVisitRobot.checkActivityIsDisplayed() }
            if (hasSymptoms) {
                symptomsAfterRiskyVenueVisitRobot.clickHasSymptomsButton()
                waitFor { questionnaireRobot.checkActivityIsDisplayed() }
            } else {
                symptomsAfterRiskyVenueVisitRobot.clickNoSymptomsButton()
                waitFor { orderLfdTestRobot.checkActivityIsDisplayed() }
                if (alreadyHasLfdTests) {
                    orderLfdTestRobot.clickIAlreadyHaveKitButton()
                    waitFor { statusRobot.checkActivityIsDisplayed() }
                } else {
                    orderLfdTestRobot.clickOrderTestButton()
                    waitFor { browserRobot.checkActivityIsDisplayed() }
                }
            }
        } else {
            venueAlertBookTestRobot.clickIllDoItLaterButton()
            waitFor { statusRobot.checkActivityIsDisplayed() }
        }
    }
}
