package uk.nhs.nhsx.covid19.android.app.settings.myarea

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.report.notReported
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.MyAreaRobot

class MyAreaActivityTest : EspressoTest() {

    private val myAreaRobot = MyAreaRobot()

    @Test
    fun whenNeitherPostCodeOrLocalAuthorityAreStored_thenNoValuesDisplayed() = notReported {
        testAppContext.setPostCode(null)
        testAppContext.setLocalAuthority(null)

        startTestActivity<MyAreaActivity>()

        myAreaRobot.checkActivityIsDisplayed()
        waitFor { myAreaRobot.checkPostCodeIsEmpty() }
        waitFor { myAreaRobot.checkLocalAuthorityIsEmpty() }
    }

    @Test
    fun whenPostCodeAndLocalAuthorityAreStored_thenDisplayStoredValues() = notReported {
        testAppContext.setPostCode("SE1")
        testAppContext.setLocalAuthority("E09000028")

        startTestActivity<MyAreaActivity>()

        myAreaRobot.checkActivityIsDisplayed()
        waitFor { myAreaRobot.checkPostCodeEquals("SE1") }
        waitFor { myAreaRobot.checkLocalAuthorityEquals("Southwark") }
    }
}
