package uk.nhs.nhsx.covid19.android.app.onboarding

import androidx.test.espresso.Espresso.pressBack
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.interfaces.HasActivity

class HowAppWorksActivityTest : EspressoTest(), HasActivity {
    override val containerId: Int
        get() = R.id.howAppWorksContainer

    @Test
    fun canActivityLaunchSuccessfully() {
        startTestActivity<HowAppWorksActivity>()
        checkActivityIsDisplayed()
    }

    @Test
    fun cannotGoBackFromActivity() {
        startTestActivity<HowAppWorksActivity>()
        checkActivityIsDisplayed()
        pressBack()
        checkActivityIsDisplayed()
    }
}
