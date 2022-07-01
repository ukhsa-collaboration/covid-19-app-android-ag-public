package uk.nhs.nhsx.covid19.android.app.state

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.state.IsolationExpirationActivity.Companion.EXTRA_EXPIRY_DATE
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.IsolationExpirationRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.setup.LocalAuthoritySetupHelper
import java.time.LocalDate
import kotlin.test.assertTrue

class IsolationExpirationActivityTest : EspressoTest(), LocalAuthoritySetupHelper {

    private val isolationExpirationRobot = IsolationExpirationRobot()

    @Test
    fun startWithNullExpiryDate_shouldFinish() {
        val activity = startTestActivity<IsolationExpirationActivity>()

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun startWithEmptyExpiryDate_shouldFinish() {
        val activity = startTestActivity<IsolationExpirationActivity> {
            putExtra(EXTRA_EXPIRY_DATE, "")
        }

        waitFor { assertTrue(activity!!.isDestroyed) }
    }

    @Test
    fun startWithExpiryDayInThePast_shouldShowIsolationHasFinished_wales() {
        val expiryDate = LocalDate.now().minusDays(1)
        startTestActivity<IsolationExpirationActivity> {
            putExtra(EXTRA_EXPIRY_DATE, expiryDate.toString())
        }

        isolationExpirationRobot.checkActivityIsDisplayed()

        waitFor { isolationExpirationRobot.checkIsolationHasFinishedWales(expiryDate) }
    }

    @Test
    fun startWithExpiryDayInTheFuture_shouldShowIsolationWillFinish_wales() {
        givenLocalAuthorityIsInWales()
        val expiryDate = LocalDate.now().plusDays(1)
        startTestActivity<IsolationExpirationActivity> {
            putExtra(EXTRA_EXPIRY_DATE, expiryDate.toString())
        }

        isolationExpirationRobot.checkActivityIsDisplayed()

        waitFor { isolationExpirationRobot.checkIsolationWillFinishWales(expiryDate) }
    }

    @Test
    fun startWithExpiryDayInThePast_shouldShowIsolationHasFinished_england() {
        givenLocalAuthorityIsInEngland()
        val expiryDate = LocalDate.now().minusDays(1)
        startTestActivity<IsolationExpirationActivity> {
            putExtra(EXTRA_EXPIRY_DATE, expiryDate.toString())
        }

        isolationExpirationRobot.checkActivityIsDisplayed()

        waitFor { isolationExpirationRobot.checkIsolationHasFinishedEngland(expiryDate) }
    }

    @Test
    fun startWithExpiryDayInTheFuture_shouldShowIsolationWillFinish_england() {
        givenLocalAuthorityIsInEngland()
        val expiryDate = LocalDate.now().plusDays(1)
        startTestActivity<IsolationExpirationActivity> {
            putExtra(EXTRA_EXPIRY_DATE, expiryDate.toString())
        }

        isolationExpirationRobot.checkActivityIsDisplayed()

        waitFor { isolationExpirationRobot.checkIsolationWillFinishEngland(expiryDate) }
    }
}
