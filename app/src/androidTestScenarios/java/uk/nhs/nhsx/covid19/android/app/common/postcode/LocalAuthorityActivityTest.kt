package uk.nhs.nhsx.covid19.android.app.common.postcode

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.Reporter
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.report.reporter
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityRobot
import kotlin.test.assertTrue

class LocalAuthorityActivityTest : EspressoTest() {

    private val localAuthorityRobot = LocalAuthorityRobot()

    @Test
    fun showSingleLocalAuthority() = reporter(
        "Local authority",
        "Single local authority",
        "User's post code corresponds to a single local authority; user is asked to confirm'",
        Reporter.Kind.FLOW
    ) {
        startTestActivity<LocalAuthorityActivity> {
            putExtra(
                LocalAuthorityActivity.EXTRA_POST_CODE,
                "N12"
            )
        }

        localAuthorityRobot.checkActivityIsDisplayed()
        waitFor { localAuthorityRobot.checkSingleAuthorityIsDisplayed("N12", "Barnet") }
        localAuthorityRobot.checkErrorIsNotDisplayed()

        step(
            stepName = "Show single local authority screen",
            stepDescription = "User's post code corresponds to a single local authority; user is asked to confirm."
        )
    }

    @Test
    fun showMultipleLocalAuthorities() = reporter(
        "Local authority",
        "Multiple local authorities",
        "User's post code corresponds to multiple local authorities; user is asked to select one",
        Reporter.Kind.SCREEN
    ) {
        startTestActivity<LocalAuthorityActivity> {
            putExtra(
                LocalAuthorityActivity.EXTRA_POST_CODE,
                "TD12"
            )
        }

        localAuthorityRobot.checkActivityIsDisplayed()

        waitFor { localAuthorityRobot.checkMultipleAuthoritiesAreDisplayed("TD12") }
        localAuthorityRobot.checkErrorIsNotDisplayed()

        step(
            stepName = "Show multiple local authorities screen",
            stepDescription = "User's post code corresponds to multiple local authorities; user is asked to select one."
        )

        localAuthorityRobot.clickConfirm()
        localAuthorityRobot.checkNoLocalAuthoritySelectedErrorIsDisplayed()

        step(
            stepName = "Click confirm without selection",
            stepDescription = "User clicks 'confirm' but they haven't selected any local authority. An error is displayed indicating they must select one."
        )

        // "S12000026": { "name": "Scottish Borders", "country": "Scotland" }
        localAuthorityRobot.selectLocalAuthority("Scottish Borders")
        localAuthorityRobot.checkLocalAuthorityNotSupportedErrorIsDisplayed()

        step(
            stepName = "Select unsupported local authority",
            stepDescription = "User selects an unsupported local authority. An error is displayed indicating they must download a different app."
        )

        localAuthorityRobot.clickConfirm()
        localAuthorityRobot.checkActivityIsDisplayed()

        // "E06000057": { "name": "Northumberland", "country": "England" }
        localAuthorityRobot.selectLocalAuthority("Northumberland")
        localAuthorityRobot.checkErrorIsNotDisplayed()

        step(
            stepName = "Select supported local authority",
            stepDescription = "User selects a supported local authority. Any previous error disappears."
        )
    }

    @Test
    fun clickBackWhenBackAllowedIsNotDefined_nothingHappens() = notReported {
        startTestActivity<LocalAuthorityActivity> {
            putExtra(
                LocalAuthorityActivity.EXTRA_POST_CODE,
                "TD12"
            )
        }

        localAuthorityRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        localAuthorityRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickBackWhenBackAllowedIsFalse_nothingHappens() = notReported {
        startTestActivity<LocalAuthorityActivity> {
            putExtra(
                LocalAuthorityActivity.EXTRA_POST_CODE,
                "TD12"
            )
            putExtra(
                LocalAuthorityActivity.EXTRA_BACK_ALLOWED,
                false
            )
        }

        localAuthorityRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        localAuthorityRobot.checkActivityIsDisplayed()
    }

    @Test
    fun clickBackWhenBackAllowedIsTrue_goesBack() = notReported {
        val activity = startTestActivity<LocalAuthorityActivity> {
            putExtra(
                LocalAuthorityActivity.EXTRA_POST_CODE,
                "TD12"
            )
            putExtra(
                LocalAuthorityActivity.EXTRA_BACK_ALLOWED,
                true
            )
        }

        localAuthorityRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }
}
