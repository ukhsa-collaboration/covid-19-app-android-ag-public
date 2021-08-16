package uk.nhs.nhsx.covid19.android.app.about

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.report.Reported
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MoreAboutAppRobot

@RunWith(Parameterized::class)
class MoreAboutAppActivityTest(override val configuration: TestConfiguration) : EspressoTest() {

    val robot = MoreAboutAppRobot()

    @Test
    @Reported
    fun showsAppReleaseDate() = reporter(
        scenario = "About app",
        title = "App date of release",
        description = "Shows app date of release",
        kind = Reporter.Kind.SCREEN
    ) {
        startTestActivity<MoreAboutAppActivity>()

        robot.checkActivityIsDisplayed()
        robot.checkAppReleaseDateIsCorrect()

        step("App date of release", "Shows app date of release")
    }
}
