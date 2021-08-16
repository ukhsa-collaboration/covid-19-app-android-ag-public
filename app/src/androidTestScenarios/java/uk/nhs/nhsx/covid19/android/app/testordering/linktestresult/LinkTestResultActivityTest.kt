package uk.nhs.nhsx.covid19.android.app.testordering.linktestresult

import androidx.test.platform.app.InstrumentationRegistry
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_EXTERNAL_BROWSER
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.report.config.TestConfiguration
import uk.nhs.nhsx.covid19.android.app.testhelpers.assertBrowserIsOpened
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.covid19.android.app.testhelpers.robots.LinkTestResultRobot
import uk.nhs.nhsx.covid19.android.app.testhelpers.runWithFeatureEnabled

@RunWith(Parameterized::class)
class LinkTestResultActivityTest(override val configuration: TestConfiguration) : EspressoTest() {

    private val linkTestResultRobot = LinkTestResultRobot()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun userTapsOnLink_NavigateToExternalLink() {
        runWithFeatureEnabled(USE_WEB_VIEW_FOR_EXTERNAL_BROWSER) {
            startTestActivity<LinkTestResultActivity>()
            assertBrowserIsOpened(context.getString(R.string.link_test_result_report_link_url)) {
                linkTestResultRobot.clickReportLink()
            }
        }
    }
}
