package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LocalAuthorityInformationRobot
import kotlin.test.assertTrue

class LocalAuthorityInformationActivityTest : EspressoTest() {

    private val localAuthorityInformationRobot = LocalAuthorityInformationRobot()

    @Test
    fun clickBack_nothingHappens() = notReported {
        startTestActivity<LocalAuthorityInformationActivity>()

        localAuthorityInformationRobot.checkActivityIsDisplayed()

        testAppContext.device.pressBack()

        localAuthorityInformationRobot.checkActivityIsDisplayed()
    }

    @Test
    fun onActivityResultLocalAuthorityOk_finished() = notReported {
        Intents.init()
        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, Intent())
        Intents.intending(hasComponent(LocalAuthorityActivity::class.qualifiedName)).respondWith(result)

        val activity = startTestActivity<LocalAuthorityInformationActivity>()
        localAuthorityInformationRobot.clickContinue()

        waitFor { assertTrue(activity!!.isDestroyed) }

        Intents.release()
    }

    @Test
    fun onActivityResultLocalAuthorityOk_nothingHappens() = notReported {
        Intents.init()

        val result = Instrumentation.ActivityResult(Activity.RESULT_CANCELED, Intent())
        Intents.intending(hasComponent(LocalAuthorityActivity::class.qualifiedName)).respondWith(result)

        startTestActivity<LocalAuthorityInformationActivity>()
        localAuthorityInformationRobot.clickContinue()

        localAuthorityInformationRobot.checkActivityIsDisplayed()

        Intents.release()
    }
}
